//
//   Calendar Notifications Plus
//   Copyright (C) 2016 Sergey Parshin (s.parshin.sc@gmail.com)
//
//   This program is free software; you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation; either version 3 of the License, or
//   (at your option) any later version.
//
//   This program is distributed in the hope that it will be useful,
//   but WITHOUT ANY WARRANTY; without even the implied warranty of
//   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//   GNU General Public License for more details.
//
//   You should have received a copy of the GNU General Public License
//   along with this program; if not, write to the Free Software Foundation,
//   Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
//


package com.github.quarck.kriptileto.utils
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
