package com.github.quarck.kriptileto.ui

/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import com.github.quarck.kriptileto.R
import com.github.quarck.kriptileto.aks.AndroidKeyStore
import com.github.quarck.kriptileto.crypto.AESTwofishSerpentEngine
import com.github.quarck.kriptileto.keysdb.KeyEntry
import com.github.quarck.kriptileto.keysdb.KeysDatabase
import com.github.quarck.kriptileto.ui.camera.CameraManager
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import org.bouncycastle.util.encoders.UrlBase64
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.CountDownLatch


/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the barcode correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */

class RandomKeyScanActivity : Activity(), SurfaceHolder.Callback {

    internal class DecodeThread(private val activity: RandomKeyScanActivity,
                                baseHints: kotlin.collections.Map<DecodeHintType, Any>?,
                                characterSet: String?) : Thread() {
        private val hints: MutableMap<DecodeHintType, Any>
        private var handler: Handler? = null
        private val handlerInitLatch: CountDownLatch

        init {
            handlerInitLatch = CountDownLatch(1)

            hints = EnumMap(DecodeHintType::class.java)
            if (baseHints != null) {
                hints.putAll(baseHints)
            }
            hints[DecodeHintType.POSSIBLE_FORMATS] = EnumSet.of(BarcodeFormat.QR_CODE)

            if (characterSet != null) {
                hints[DecodeHintType.CHARACTER_SET] = characterSet
            }
            //hints[DecodeHintType.NEED_RESULT_POINT_CALLBACK] = resultPointCallback
        }

        fun getHandlerW(): Handler {
            try {
                handlerInitLatch.await()
            } catch (ie: InterruptedException) {
                // continue?
            }

            return handler!!
        }

        override fun run() {
            Looper.prepare()
            handler = DecodeHandler(activity, hints)
            handlerInitLatch.countDown()
            Looper.loop()
        }

        companion object {

            val BARCODE_BITMAP = "barcode_bitmap"
            val BARCODE_SCALED_FACTOR = "barcode_scaled_factor"
        }

    }

    internal class DecodeHandler(private val activity: RandomKeyScanActivity, hints: kotlin.collections.Map<DecodeHintType, Any>) : Handler() {
        private val multiFormatReader: MultiFormatReader
        private var running = true

        init {
            multiFormatReader = MultiFormatReader()
            multiFormatReader.setHints(hints)
        }

        override fun handleMessage(message: Message?) {
            if (message == null || !running) {
                return
            }
            when (message.what) {
                0 -> {
                    decode(message.obj as ByteArray, message.arg1, message.arg2)
                }
                1 -> {
                    running = false
                    Looper.myLooper()!!.quit()
                }
            }
        }

        /**
         * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
         * reuse the same reader objects from one decode to the next.
         *
         * @param data   The YUV preview frame.
         * @param width  The width of the preview frame.
         * @param height The height of the preview frame.
         */
        private fun decode(data: ByteArray, width: Int, height: Int) {
            val start = System.currentTimeMillis()
            var rawResult: Result? = null
            val source = activity.cameraManager()?.buildLuminanceSource(data, width, height)
            if (source != null) {
                val bitmap = BinaryBitmap(HybridBinarizer(source))
                try {
                    rawResult = multiFormatReader.decodeWithState(bitmap)
                } catch (re: ReaderException) {
                    // continue
                } finally {
                    multiFormatReader.reset()
                }
            }

            val handler = activity.getHandler()
            if (rawResult != null) {
                // Don't log the barcode contents for security.
                val end = System.currentTimeMillis()
                Log.d(TAG, "Found barcode in " + (end - start) + " ms")
                if (handler != null) {
                    val message = Message.obtain(handler, 1, rawResult)
                    val bundle = Bundle()
                    bundleThumbnail(source!!, bundle)
                    message.data = bundle
                    message.sendToTarget()
                }
            } else {
                if (handler != null) {
                    val message = Message.obtain(handler, 2)
                    message.sendToTarget()
                }
            }
        }

        companion object {

            private val TAG = DecodeHandler::class.java.simpleName

            private fun bundleThumbnail(source: PlanarYUVLuminanceSource, bundle: Bundle) {
                val pixels = source.renderThumbnail()
                val width = source.thumbnailWidth
                val height = source.thumbnailHeight
                val bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888)
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out)
                bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray())
                bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, width.toFloat() / source.width)
            }
        }

    }


    class CaptureActivityHandler internal constructor(private val activity: RandomKeyScanActivity,
                                                      decodeFormats: MutableCollection<BarcodeFormat>,
                                                      baseHints: kotlin.collections.Map<DecodeHintType, Any>?,
                                                      characterSet: String?,
                                                      private val cameraManager: CameraManager?) : Handler() {
        private val decodeThread: DecodeThread
        private var state: State? = null

        private enum class State {
            PREVIEW,
            SUCCESS,
            DONE
        }

        init {
            decodeThread = DecodeThread(activity, baseHints, characterSet)
            decodeThread.start()
            state = State.SUCCESS
            cameraManager?.startPreview()
            restartPreviewAndDecode()
        }// Start ourselves capturing previews and decoding.

        override fun handleMessage(message: Message) {
            when (message.what) {
                0 // R.id.restart_preview:
                -> restartPreviewAndDecode()
                1 // R.id.decode_succeeded:
                -> {
                    state = State.SUCCESS
                    val bundle = message.data
                    var barcode: Bitmap? = null
                    var scaleFactor = 1.0f
                    if (bundle != null) {
                        val compressedBitmap = bundle.getByteArray(DecodeThread.BARCODE_BITMAP)
                        if (compressedBitmap != null) {
                            barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.size, null)
                            // Mutable copy:
                            barcode = barcode!!.copy(Bitmap.Config.ARGB_8888, true)
                        }
                        scaleFactor = bundle.getFloat(DecodeThread.BARCODE_SCALED_FACTOR)
                    }
                    activity.handleDecode(message.obj as Result, barcode, scaleFactor)
                }
                2 //R.id.decode_failed:
                -> {
                    // We're decoding as fast as possible, so when one decode fails, start another.
                    state = State.PREVIEW
                    cameraManager?.requestPreviewFrame(decodeThread.getHandlerW(), 0)
                }
                3// R.id.return_scan_result:
                -> {
                    activity.setResult(Activity.RESULT_OK, message.obj as Intent)
                    activity.finish()
                }
            }
        }

        fun quitSynchronously() {
            state = State.DONE
            cameraManager?.stopPreview()
            val quit = Message.obtain(decodeThread.getHandlerW(), 1)
            quit.sendToTarget()
            try {
                // Wait at most half a second; should be enough time, and onPause() will timeout quickly
                decodeThread.join(500L)
            } catch (e: InterruptedException) {
                // continue
            }

            // Be absolutely sure we don't send any queued up messages
            removeMessages(1)
            removeMessages(2)
        }

        private fun restartPreviewAndDecode() {
            if (state == State.SUCCESS) {
                state = State.PREVIEW
                cameraManager?.requestPreviewFrame(decodeThread.getHandlerW(), 0)
                activity.drawViewfinder()
            }
        }
    }

    private var cameraManager: CameraManager? = null
    private var handler: CaptureActivityHandler? = null

    private lateinit var viewfinderView: ViewfinderView
    private var lastResult: Result? = null
    private var hasSurface: Boolean = false
    private var copyToClipboard: Boolean = false

    private lateinit var inactivityTimer: InactivityTimer


    fun cameraManager() = cameraManager

    fun getHandler(): Handler? {
        return handler
    }

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_random_key_scan)

        viewfinderView = findViewById(R.id.viewfinder_view) as ViewfinderView

        hasSurface = false
        inactivityTimer = InactivityTimer(this)
    }

    override fun onResume() {
        super.onResume()

//    // historyManager must be initialized here to update the history preference
//    historyManager = HistoryManager(this)
//    historyManager!!.trimHistory()

        // CameraManager must be initialized here, not in onCreate(). This is necessary because we don't
        // want to open the camera driver and measure the screen size if we're going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the wrong size and partially
        // off screen.
        cameraManager = CameraManager(application).apply {
            viewfinderView.setCameraManager(this)
            setTorch(false)
        }

        handler = null
        lastResult = null

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT

        inactivityTimer.onResume()

        copyToClipboard = false

        val surfaceView = findViewById(R.id.preview_view) as SurfaceView
        val surfaceHolder = surfaceView.holder
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder)
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this)
        }
    }


    private fun shutdownScanning() {
        handler?.quitSynchronously()
        handler = null

        inactivityTimer.onPause()

        cameraManager?.closeDriver()

        if (!hasSurface) {
            val surfaceView = findViewById(R.id.preview_view) as SurfaceView
            val surfaceHolder = surfaceView.holder
            surfaceHolder.removeCallback(this)

            surfaceView.visibility = View.GONE
        }

        viewfinderView.visibility = View.GONE
    }

    override fun onPause() {
        shutdownScanning()
        super.onPause()
    }

    override fun onDestroy() {
        inactivityTimer.shutdown()
        super.onDestroy()
    }


    override fun surfaceCreated(holder: SurfaceHolder?) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!")
        }
        if (!hasSurface) {
            hasSurface = true
            initCamera(holder)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        hasSurface = false
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // do nothing
    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult The contents of the barcode.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param barcode   A greyscale bitmap of the camera data which was decoded.
     */
    fun handleDecode(rawResult: Result, barcode: Bitmap?, scaleFactor: Float) {
        inactivityTimer.onActivity()
        lastResult = rawResult

        Log.e(TAG, "Result: ${rawResult.text}")

        val rawKey = UrlBase64.decode(rawResult.text)

        if (rawKey.size == AESTwofishSerpentEngine.KEY_LENGTH_BYTES) {
            shutdownScanning()
            findViewById<LinearLayout>(R.id.layoutKeyNameAndSave).visibility = View.VISIBLE
            findViewById<View>(R.id.fillWhiteView).visibility = View.VISIBLE
            findViewById<Button>(R.id.buttonSave)?.setOnClickListener {
                val name = findViewById<EditText>(R.id.editTextKeyName).text.toString()
                saveKey(name, rawKey)
                finish()
            }
        }
    }

    private fun saveKey(name: String, key: ByteArray) {
        KeysDatabase(context = this).use {
            db ->

            val id = db.add(KeyEntry.forName("_")) // temp name to make sure it was updated

            val updatedKeyEntry =
                    if (AndroidKeyStore.isSupported) {
                        val aks = AndroidKeyStore()
                        aks.createKey(id) // create matchng keystore key that would be encrypting this key in DB
                        val encryptedKey = aks.encrypt(id, key)
                        val encryptedBase64Key = UrlBase64.encode(encryptedKey)
                        KeyEntry(id, name, encryptedBase64Key.toString(charset = Charsets.UTF_8), true)
                    }
                    else {
                        val base64Key = UrlBase64.encode(key)
                        KeyEntry(id, name, base64Key.toString(charset = Charsets.UTF_8), false)
                    }

            db.update(updatedKeyEntry)
        }
    }


    private fun initCamera(surfaceHolder: SurfaceHolder?) {
        if (surfaceHolder == null)
            throw IllegalStateException("No SurfaceHolder provided")

        if (cameraManager?.isOpen == true) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?")
            return
        }

        try {
            cameraManager?.openDriver(surfaceHolder)
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = CaptureActivityHandler(this, mutableSetOf(BarcodeFormat.QR_CODE), null, null, cameraManager)
            }
        } catch (ioe: IOException) {
            Log.w(TAG, ioe)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Unexpected error initializing camera", e)
        }

    }

    fun drawViewfinder() {
        viewfinderView.drawViewfinder()
    }

//    fun getViewfinderViewX() = viewfinderView

    companion object {
        private val TAG = RandomKeyScanActivity::class.java.simpleName
    }
}

