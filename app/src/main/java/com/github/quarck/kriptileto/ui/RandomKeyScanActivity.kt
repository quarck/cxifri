package com.github.quarck.kriptileto.ui

//import android.os.Bundle
//import android.app.Activity
import com.github.quarck.kriptileto.R
//
//import kotlinx.android.synthetic.main.activity_random_key_scan.*
//
//class RandomKeyScanActivity : Activity() {
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_random_key_scan)
//        actionBar?.setDisplayHomeAsUpEnabled(true)
//    }
//
//}

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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.github.quarck.kriptileto.ui.camera.CameraManager

import java.io.IOException;
import java.text.DateFormat;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;


internal enum class IntentSource {

  NATIVE_APP_INTENT,
  PRODUCT_SEARCH_LINK,
  ZXING_LINK,
  NONE

}
/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the barcode correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
class RandomKeyScanActivity: Activity(), SurfaceHolder.Callback {
  public var cameraManager: CameraManager? = null


  private var handler: CaptureActivityHandler? = null
  private var savedResultToShow: Result? = null
  public var viewfinderView: ViewfinderView? = null
  private var statusView: TextView? = null
  private var resultView: View? = null
  private var lastResult: Result? = null
  private var hasSurface: Boolean = false
  private var copyToClipboard: Boolean = false
  private var source: IntentSource? = null
  private var sourceUrl: String? = null
  ///private var scanFromWebPageManager: KillThisShit? = null
  private var decodeFormats: kotlin.collections.MutableCollection<BarcodeFormat>? = null
  private var decodeHints: kotlin.collections.Map<DecodeHintType, Any>? = null
  //private var characterSet: String? = null
  private var inactivityTimer: InactivityTimer? = null

  private val currentOrientation: Int
    get() {
      val rotation = windowManager.defaultDisplay.rotation
      return if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        when (rotation) {
          Surface.ROTATION_0, Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
          else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        }
      } else {
        when (rotation) {
          Surface.ROTATION_0, Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
          else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        }
      }
    }

  fun getHandler(): Handler? {
    return handler
  }

  public override fun onCreate(icicle: Bundle?) {
    super.onCreate(icicle)

    val window = window
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    setContentView(R.layout.activity_random_key_scan)

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
    cameraManager = CameraManager(application)

    viewfinderView = findViewById(R.id.viewfinder_view) as ViewfinderView
    viewfinderView!!.setCameraManager(cameraManager!!)

    resultView = findViewById(R.id.result_view)
    statusView = findViewById(R.id.status_view) as TextView

    handler = null
    lastResult = null

    val prefs = PreferenceManager.getDefaultSharedPreferences(this)

    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT

    resetStatusView()


    cameraManager?.setTorch(false)

    inactivityTimer!!.onResume()

    val intent = intent

    copyToClipboard = false

    source = IntentSource.NONE
    sourceUrl = null
    //scanFromWebPageManager = null
    decodeFormats = null
    //characterSet = null

    if (intent != null) {

      val action = intent.action
      val dataString = intent.dataString

        if (dataString != null &&
              dataString.contains("http://www.google") &&
              dataString.contains("/m/products/scan")) {

        // Scan only products and send the result to mobile Product Search.
        source = IntentSource.PRODUCT_SEARCH_LINK
        sourceUrl = dataString
      }

      decodeFormats = mutableSetOf(BarcodeFormat.QR_CODE)

      // characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET)

    }

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

  override fun onPause() {
    if (handler != null) {
      handler!!.quitSynchronously()
      handler = null
    }
    inactivityTimer!!.onPause()

    cameraManager!!.closeDriver()
    //historyManager = null; // Keep for onActivityResult
    if (!hasSurface) {
      val surfaceView = findViewById(R.id.preview_view) as SurfaceView
      val surfaceHolder = surfaceView.holder
      surfaceHolder.removeCallback(this)
    }
    super.onPause()
  }

  override fun onDestroy() {
    inactivityTimer!!.shutdown()
    super.onDestroy()
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    when (keyCode) {
      KeyEvent.KEYCODE_BACK -> {
        if (source === IntentSource.NATIVE_APP_INTENT) {
          setResult(Activity.RESULT_CANCELED)
          finish()
          return true
        }
        if ((source === IntentSource.NONE || source === IntentSource.ZXING_LINK) && lastResult != null) {
          restartPreviewAfterDelay(0L)
          return true
        }
      }
      KeyEvent.KEYCODE_FOCUS, KeyEvent.KEYCODE_CAMERA ->
        // Handle these events so they don't launch the Camera app
        return true
      // Use volume up/down to turn on light
      KeyEvent.KEYCODE_VOLUME_DOWN -> {
        cameraManager!!.setTorch(false)
        return true
      }
      KeyEvent.KEYCODE_VOLUME_UP -> {
        cameraManager!!.setTorch(true)
        return true
      }
    }
    return super.onKeyDown(keyCode, event)
  }



//  public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
//    if (resultCode == Activity.RESULT_OK && requestCode == HISTORY_REQUEST_CODE && historyManager != null) {
//      val itemNumber = intent.getIntExtra(Intents.History.ITEM_NUMBER, -1)
//      if (itemNumber >= 0) {
//        val historyItem = historyManager!!.buildHistoryItem(itemNumber)
//        decodeOrStoreSavedBitmap(null, historyItem.result)
//      }
//    }
//  }

  private fun decodeOrStoreSavedBitmap(bitmap: Bitmap?, result: Result?) {
    // Bitmap isn't used yet -- will be used soon
    if (handler == null) {
      savedResultToShow = result
    } else {
      if (result != null) {
        savedResultToShow = result
      }
      if (savedResultToShow != null) {
//        val message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow)
//        handler!!.sendMessage(message)
      }
      savedResultToShow = null
    }
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
    inactivityTimer!!.onActivity()
    lastResult = rawResult
    handleDecodeInternally(rawResult, barcode)
  }

  // Put up our own UI for how to handle the decoded contents.
  private fun handleDecodeInternally(rawResult: Result, barcode: Bitmap?) {

    Log.e(TAG, "Result: ${rawResult.text}")

  }

  private fun sendReplyMessage(id: Int, arg: Any, delayMS: Long) {
    if (handler != null) {
      val message = Message.obtain(handler, id, arg)
      if (delayMS > 0L) {
        handler!!.sendMessageDelayed(message, delayMS)
      } else {
        handler!!.sendMessage(message)
      }
    }
  }

  private fun initCamera(surfaceHolder: SurfaceHolder?) {
    if (surfaceHolder == null) {
      throw IllegalStateException("No SurfaceHolder provided")
    }
    if (cameraManager!!.isOpen) {
      Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?")
      return
    }
    try {
      cameraManager!!.openDriver(surfaceHolder)
      // Creating the handler starts the preview, which can also throw a RuntimeException.
      if (handler == null) {
        handler = CaptureActivityHandler(this, decodeFormats!!, null, null, cameraManager)
      }
      decodeOrStoreSavedBitmap(null, null)
    } catch (ioe: IOException) {
      Log.w(TAG, ioe)
      //displayFrameworkBugMessageAndExit()
    } catch (e: RuntimeException) {
      // Barcode Scanner has seen crashes in the wild of this variety:
      // java.?lang.?RuntimeException: Fail to connect to camera service
      Log.w(TAG, "Unexpected error initializing camera", e)
//      displayFrameworkBugMessageAndExit()
    }

  }

//  private fun displayFrameworkBugMessageAndExit() {
//    val builder = AlertDialog.Builder(this)
//    builder.setTitle(getString(R.string.app_name))
//    builder.setMessage(getString(R.string.msg_camera_framework_bug))
//    builder.setPositiveButton(R.string.button_ok, FinishListener(this))
//    builder.setOnCancelListener(FinishListener(this))
//    builder.show()
//  }

  fun restartPreviewAfterDelay(delayMS: Long) {
    if (handler != null) {
      handler!!.sendEmptyMessageDelayed(0, delayMS)
    }
    resetStatusView()
  }

  private fun resetStatusView() {
    resultView!!.visibility = View.GONE
    statusView!!.setText("")
    statusView!!.visibility = View.VISIBLE
    viewfinderView!!.setVisibility(View.VISIBLE)
    lastResult = null
  }

  fun drawViewfinder() {
    viewfinderView!!.drawViewfinder()
  }

  companion object {

    private val TAG = RandomKeyScanActivity::class.java.simpleName

    private val DEFAULT_INTENT_RESULT_DURATION_MS = 1500L
    private val BULK_MODE_SCAN_DELAY_MS = 1000L

    private val ZXING_URLS = arrayOf("http://zxing.appspot.com/scan", "zxing://scan/")

    private val HISTORY_REQUEST_CODE = 0x0000bacc

    private val DISPLAYABLE_METADATA_TYPES = EnumSet.of(ResultMetadataType.ISSUE_NUMBER,
            ResultMetadataType.SUGGESTED_PRICE,
            ResultMetadataType.ERROR_CORRECTION_LEVEL,
            ResultMetadataType.POSSIBLE_COUNTRY)

    private fun isZXingURL(dataString: String?): Boolean {
      if (dataString == null) {
        return false
      }
      for (url in ZXING_URLS) {
        if (dataString.startsWith(url)) {
          return true
        }
      }
      return false
    }

    private fun drawLine(canvas: Canvas, paint: Paint, a: ResultPoint?, b: ResultPoint?, scaleFactor: Float) {
      if (a != null && b != null) {
        canvas.drawLine(scaleFactor * a.x,
                scaleFactor * a.y,
                scaleFactor * b.x,
                scaleFactor * b.y,
                paint)
      }
    }
  }


}

