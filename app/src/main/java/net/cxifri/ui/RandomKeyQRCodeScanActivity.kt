/*
 * Copyright (C) 2018 Sergey Parshin (quarck@gmail.com)
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

package net.cxifri.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import net.cxifri.R
import net.cxifri.crypto.RandomKeyGenerator
import net.cxifri.keysdb.KeySaveHelper
import net.cxifri.ui.camera.CameraManager
import net.cxifri.utils.hasCameraPermission
import net.cxifri.utils.requestCameraPermission
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import net.cxifri.crypto.AESTwofishSerpentEngine
import org.bouncycastle.util.encoders.UrlBase64
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

class RandomKeyQRCodeScanActivity : AppCompatActivity(), SurfaceHolder.Callback {


    internal class DecodeThread(val activity: RandomKeyQRCodeScanActivity) : Thread() {

        private val hints: MutableMap<DecodeHintType, Any> =
                EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                    this[DecodeHintType.POSSIBLE_FORMATS] = EnumSet.of(BarcodeFormat.QR_CODE)
                }

        private var handler: Handler? = null
        private val handlerInitLatch = CountDownLatch(1)

        fun getThreadHandler(): Handler {
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
    }

    internal class DecodeHandler(private val activity: RandomKeyQRCodeScanActivity, hints: kotlin.collections.Map<DecodeHintType, Any>) : Handler() {
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
                MSG_DECODER_HANDLER_DECODE -> {
                    decode(message.obj as ByteArray, message.arg1, message.arg2)
                }
                MSG_DECODER_HANDLER_QUIT -> {
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

            activity.getHandler()?.let {
                handler ->

                if (rawResult != null) {
                    val message = Message.obtain(handler, MSG_CAPTURE_HANDLER_DECODE_SUCCEEDED, rawResult)
                    message.sendToTarget()
                } else {
                    val message = Message.obtain(handler, MSG_CAPTURE_HANDLER_FAILED)
                    message.sendToTarget()
                }
            }
        }

    }


    class CaptureActivityHandler(private val activity: RandomKeyQRCodeScanActivity,
                                 private val cameraManager: CameraManager?) : Handler() {
        private val decodeThread: DecodeThread
        private var state: State? = null

        private enum class State {
            PREVIEW,
            SUCCESS,
            DONE
        }

        init {
            decodeThread = DecodeThread(activity)
            decodeThread.start()
            state = State.SUCCESS
            cameraManager?.startPreview()
            restartPreviewAndDecode()
        }// Start ourselves capturing previews and decoding.

        override fun handleMessage(message: Message) {
            if (message.what == MSG_CAPTURE_HANDLER_RESTART_PREVIEW) {
                restartPreviewAndDecode()
            } else if (message.what == MSG_CAPTURE_HANDLER_DECODE_SUCCEEDED) {
                state = State.SUCCESS
                activity.handleDecode(message.obj as Result)
            }
            else if (message.what == MSG_CAPTURE_HANDLER_FAILED) {
                // We're decoding as fast as possible, so when one decode fails, start another.
                state = State.PREVIEW
                cameraManager?.requestPreviewFrame(decodeThread.getThreadHandler(), 0)
            }
        }

        fun quitSynchronously() {
            state = State.DONE
            cameraManager?.stopPreview()
            val quit = Message.obtain(decodeThread.getThreadHandler(), MSG_DECODER_HANDLER_QUIT)
            quit.sendToTarget()
            try {
                // Wait at most half a second; should be enough time, and onPause() will timeout quickly
                decodeThread.join(500L)
            } catch (e: InterruptedException) {
                // continue
            }

            // Be absolutely sure we don't send any queued up messages
            removeMessages(MSG_CAPTURE_HANDLER_DECODE_SUCCEEDED)
            removeMessages(MSG_CAPTURE_HANDLER_FAILED)
        }

        private fun restartPreviewAndDecode() {
            if (state == State.SUCCESS) {
                state = State.PREVIEW
                cameraManager?.requestPreviewFrame(decodeThread.getThreadHandler(), MSG_DECODER_HANDLER_DECODE)
                activity.drawViewfinder()
            }
        }
    }

    private var cameraManager: CameraManager? = null
    private var handler: CaptureActivityHandler? = null

    private lateinit var viewfinderView: ViewfinderView
    private var lastResult: Result? = null
    private var hasSurface: Boolean = false

    private lateinit var inactivityTimer: InactivityTimer


    fun cameraManager() = cameraManager

    fun getHandler(): Handler? {
        return handler
    }

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_random_key_scan)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        viewfinderView = findViewById(R.id.viewfinder_view) as ViewfinderView

        hasSurface = false
        inactivityTimer = InactivityTimer(this)
    }

    override fun onResume() {
        super.onResume()

        if (!this.hasCameraPermission) {
            this.requestCameraPermission()
        }

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
    fun handleDecode(rawResult: Result) {
        inactivityTimer.onActivity()
        lastResult = rawResult

        Log.e(TAG, "Result: ${rawResult.text}")

        val rawKey = RandomKeyGenerator().verifyChecksum(UrlBase64.decode(rawResult.text))

        if (rawKey != null && rawKey.size == AESTwofishSerpentEngine.KEY_LENGTH_BYTES) {
            shutdownScanning()
            findViewById<LinearLayout>(R.id.layoutKeyNameAndSave).visibility = View.VISIBLE
            findViewById<View>(R.id.fillWhiteView).visibility = View.VISIBLE
            findViewById<Button>(R.id.buttonSave)?.setOnClickListener {
                val name = findViewById<EditText>(R.id.editTextKeyName).text.toString()
                KeySaveHelper().saveKey(this, name, rawKey, true)
                finish()
            }
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
                handler = CaptureActivityHandler(this, cameraManager)
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
        private val TAG = RandomKeyQRCodeScanActivity::class.java.simpleName

        val BARCODE_BITMAP = "barcode_bitmap"
        val BARCODE_SCALED_FACTOR = "barcode_scaled_factor"

        val MSG_DECODER_HANDLER_DECODE = 0
        val MSG_DECODER_HANDLER_QUIT = 1

        val MSG_CAPTURE_HANDLER_RESTART_PREVIEW = 0
        val MSG_CAPTURE_HANDLER_DECODE_SUCCEEDED = 1
        val MSG_CAPTURE_HANDLER_FAILED = 2
    }
}

