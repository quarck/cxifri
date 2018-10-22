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


package net.cxifri.utils
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

fun Context.hasPermission(perm: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

fun Activity.shouldShowRationale(perm: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.shouldShowRequestPermissionRationale(perm)
        } else {
            false
        }


val Context.hasCameraPermission
        get() = this.hasPermission(Manifest.permission.CAMERA)

fun Activity.requestCameraPermission() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.requestPermissions(
                    arrayOf(Manifest.permission.CAMERA), 0)
        } else {
            Unit
        }
