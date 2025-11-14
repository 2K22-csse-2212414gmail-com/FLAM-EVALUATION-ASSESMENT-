package com.example.edgeview
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {
private lateinit var textureView: TextureView
private lateinit var renderer: GLRenderer
private lateinit var camera2Helper: Camera2Helper


companion object {
init {
System.loadLibrary("native-lib")
}
const val TAG = "MainActivity"
}


override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
// Simple layout programmatically
textureView = TextureView(this)
setContentView(textureView)


renderer = GLRenderer(this)


textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
renderer.start(surface, width, height)
camera2Helper = Camera2Helper(this@MainActivity) { yuvByteArray, w, h ->
// called on background thread
val processed = processFrameNative(yuvByteArray, w, h)
renderer.updateFrame(processed, w, h)
}
camera2Helper.startCamera()
}


override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {}
override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
camera2Helper.stopCamera()
renderer.stop()
return true
}
override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}
}


requestPermissionsIfNeeded()
}


private fun requestPermissionsIfNeeded() {
val needed = mutableListOf<String>()
if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
needed.add(Manifest.permission.CAMERA)
if (needed.isNotEmpty()) {
ActivityCompat.requestPermissions(this, needed.toTypedArray(), 101)
}
}


// JNI bridge: send YUV (NV21) bytes and dimensions. Native returns RGBA byte[]
private external fun processFrameNative(yuv: ByteArray, width: Int, height: Int): ByteArray
}