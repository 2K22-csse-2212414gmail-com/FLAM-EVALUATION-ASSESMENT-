package com.example.edgeview
}


@SuppressLint("MissingPermission")
fun startCamera() {
val id = cameraManager.cameraIdList[0]
val characteristics = cameraManager.getCameraCharacteristics(id)
val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
val size = map.getOutputSizes(ImageFormat.YUV_420_888)[0]


imageReader = ImageReader.newInstance(size.width, size.height, ImageFormat.YUV_420_888, 2)
imageReader.setOnImageAvailableListener({ reader ->
val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
val yuv = yuv420ToNV21(image)
onFrame(yuv, image.width, image.height)
image.close()
}, handler)


cameraManager.openCamera(id, object : CameraDevice.StateCallback() {
override fun onOpened(device: CameraDevice) {
cameraDevice = device
val surfaces = listOf(imageReader.surface)
device.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
override fun onConfigured(session: CameraCaptureSession) {
captureSession = session
val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
request.addTarget(imageReader.surface)
request.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
session.setRepeatingRequest(request.build(), null, handler)
}
override fun onConfigureFailed(session: CameraCaptureSession) {}
}, handler)
}
override fun onDisconnected(device: CameraDevice) { device.close() }
override fun onError(device: CameraDevice, error: Int) { device.close() }
}, handler)
}


fun stopCamera() {
captureSession?.close(); captureSession = null
cameraDevice?.close(); cameraDevice = null
imageReader.close()
thread.quitSafely()
}


// Convert Image (YUV_420_888) to NV21 byte[]
private fun yuv420ToNV21(image: Image): ByteArray {
val width = image.width
val height = image.height
val ySize = width * height
val uvSize = width * height / 4
val nv21 = ByteArray(ySize + uvSize * 2)


val yBuffer = image.planes[0].buffer
val uBuffer = image.planes[1].buffer
val vBuffer = image.planes[2].buffer


yBuffer.get(nv21, 0, ySize)


val rowStride = image.planes[2].rowStride
val pixelStride = image.planes[2].pixelStride


// Interleave V and U
var offset = ySize
val vRow = ByteArray(rowStride)
val uRow = ByteArray(rowStride)


for (row in 0 until height / 2) {
vBuffer.get(vRow, 0, rowStride)
uBuffer.get(uRow, 0, rowStride)
var col = 0
while (col < width) {
nv21[offset++] = vRow[col]
nv21[offset++] = uRow[col]