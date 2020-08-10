/*
 * Copyright (C) The Android Open Source Project
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
package uk.nhs.nhsx.covid19.android.app.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceHolder.Callback
import android.view.SurfaceView
import android.view.ViewGroup
import com.google.android.gms.vision.CameraSource
import timber.log.Timber
import java.io.IOException

class CameraSourcePreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {
    private val surfaceView = SurfaceView(context)
    private var startRequested = false
    private var surfaceAvailable = false
    private var cameraSource: CameraSource? = null

    init {
        surfaceView.holder.addCallback(SurfaceCallback())
        addView(surfaceView)
    }

    fun start(cameraSource: CameraSource?) {
        if (cameraSource == null) {
            stop()
        }
        this.cameraSource = cameraSource
        if (this.cameraSource != null) {
            startRequested = true
            startIfReady()
        }
    }

    fun stop() {
        cameraSource?.stop()
    }

    fun release() {
        cameraSource?.release()
        cameraSource = null
    }

    @SuppressLint("MissingPermission")
    private fun startIfReady() {
        if (startRequested && surfaceAvailable) {
            cameraSource!!.start(surfaceView.holder)
            startRequested = false
        }
    }

    private inner class SurfaceCallback : Callback {
        override fun surfaceCreated(surface: SurfaceHolder) {
            surfaceAvailable = true
            try {
                startIfReady()
            } catch (se: SecurityException) {
                Timber.e(se, "Do not have permission to start the camera")
            } catch (e: IOException) {
                Timber.e(e, "Could not start camera source.")
            }
        }

        override fun surfaceDestroyed(surface: SurfaceHolder) {
            surfaceAvailable = false
        }

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
        }
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        var width = 320
        var height = 240
        if (cameraSource != null) {
            val size = cameraSource!!.previewSize
            if (size != null) {
                width = size.width
                height = size.height
            }
        }

        // Swap width and height sizes when in portrait, since it will be rotated 90 degrees
        if (isPortraitMode) {
            val tmp = width
            width = height
            height = tmp
        }
        val layoutWidth = right - left
        val layoutHeight = bottom - top

        // Computes height and width for potentially doing fit width.
        var childWidth = layoutWidth
        var childHeight =
            (layoutWidth.toFloat() / width.toFloat() * height).toInt()

        if (childHeight < layoutHeight) {
            childHeight = layoutHeight
            childWidth = (layoutHeight.toFloat() / height.toFloat() * width).toInt()
        }
        for (i in 0 until childCount) {
            getChildAt(i).layout(0, 0, childWidth, childHeight)
        }
        try {
            startIfReady()
        } catch (se: SecurityException) {
            Timber.e(se, "Do not have permission to start the camera")
        } catch (e: IOException) {
            Timber.e(e, "Could not start camera source.")
        }
    }

    private val isPortraitMode: Boolean
        get() {
            val orientation = context.resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                return false
            }
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                return true
            }
            Timber.d("isPortraitMode returning false by default")
            return false
        }
}
