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

import com.github.quarck.kriptileto.R
import com.github.quarck.kriptileto.ui.camera.CameraManager
import com.google.zxing.ResultPoint

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

import java.util.ArrayList

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */

// This constructor is used when the class is built from an XML resource.
class ViewfinderView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var cameraManager: CameraManager? = null
    private val paint: Paint

    private val maskColor: Int

    init {
        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val resources = resources
        maskColor = resources.getColor(R.color.colorPrimary)

    }

    fun setCameraManager(cameraManager: CameraManager) {
        this.cameraManager = cameraManager
    }

    @SuppressLint("DrawAllocation")
    public override fun onDraw(canvas: Canvas) {
        if (cameraManager == null) {
            return  // not ready yet, early draw before done configuring
        }
        val frame = cameraManager?.getFramingRect()
        val previewFrame = cameraManager?.getFramingRectInPreview()
        if (frame == null || previewFrame == null) {
            return
        }
        val width = canvas.width
        val height = canvas.height

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.color = maskColor

        canvas.drawRect(0f, 0f, width.toFloat(), frame.top.toFloat(), paint)
        canvas.drawRect(0f, frame.top.toFloat(), frame.left.toFloat(), (frame.bottom + 1).toFloat(), paint)
        canvas.drawRect((frame.right + 1).toFloat(), frame.top.toFloat(), width.toFloat(), (frame.bottom + 1).toFloat(), paint)
        canvas.drawRect(0f, (frame.bottom + 1).toFloat(), width.toFloat(), height.toFloat(), paint)

        // Request another update at the animation interval, but only repaint the laser line,
        // not the entire viewfinder mask.
        postInvalidateDelayed(ANIMATION_DELAY,
                frame.left - POINT_SIZE,
                frame.top - POINT_SIZE,
                frame.right + POINT_SIZE,
                frame.bottom + POINT_SIZE)
    }

    fun drawViewfinder() {
        invalidate()
    }


    companion object {

        private val ANIMATION_DELAY = 80L
        private val POINT_SIZE = 6
    }

}
