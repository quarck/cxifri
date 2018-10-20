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

package com.github.quarck.kriptileto.ui

import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.BitmapFactory
import android.provider.Browser

import com.github.quarck.kriptileto.R
import com.github.quarck.kriptileto.ui.camera.CameraManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.Result

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log

/**
 * This class handles all the messaging which comprises the state machine for capture.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
class CaptureActivityHandler internal constructor(private val activity: RandomKeyScanActivity,
                                                  decodeFormats: MutableCollection<BarcodeFormat>,
                                                  baseHints: Map<DecodeHintType, Any>?,
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
        decodeThread = DecodeThread(activity, decodeFormats, baseHints, characterSet,
                ViewfinderResultPointCallback(activity.viewfinderView ?: throw Exception("No view")))
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

    companion object {

        private val TAG = CaptureActivityHandler::class.java.simpleName
    }

}
