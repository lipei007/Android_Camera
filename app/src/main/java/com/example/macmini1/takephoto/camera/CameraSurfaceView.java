package com.example.macmini1.takephoto.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;


public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.AutoFocusCallback, MediaRecorder.OnErrorListener {


    // region Callback && Const
    public interface CaptureImageCallback {
        void captureImageCompletion(byte[] data, String path);
    }

    public interface RecordVideoCallback {
        void recordVideoCompletion(String path);
    }

    public interface CameraErrorHandler {
        void recordError(MediaRecorder mr);
        void initCameraError(String error);
    }

    public interface CameraEventListener {

        void cameraDidChangedToRecordVideo();
        void cameraDidChangedToTakePicture();
        void cameraDidChanged(boolean isFront);
        void cameraDidStartRecordVideo();
        void cameraDidStopRecordVideo();
        void cameraDidTakePicture();
        void cameraDidChangedFlashMode(int flashMode);
    }

    // endregion

    // region Property

    private Context mCtx;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private MediaRecorder recorder;
    private int mCameraID;

    private int mScreenWidth;
    private int mScreenHeight;

    public CameraErrorHandler errorHandler;
    private int flashMode;

    private boolean isRecordVideo; // record mode
    private boolean isRecording; // recording
    private boolean isFront;

    private String pictureDir;
    private String videoDir;

    private RecordVideoCallback mVidCallback;
    private String mVidPath;

    private CameraEventListener mListener;

    // endregion

    // region Getter

    public boolean isRecordVideo() {
        return isRecordVideo;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isFront() {
        return isFront;
    }

    public String getPictureDir() {
        return pictureDir;
    }

    public String getVideoDir() {
        return videoDir;
    }

    // endregion

    // region Construction
    public CameraSurfaceView(Context context) {
        this(context,null);
    }

    public CameraSurfaceView(Context ctx, AttributeSet attrs) {
        this(ctx,attrs,0);
    }

    public CameraSurfaceView(Context ctx, AttributeSet attrs, int defStyleAttr) {
        super(ctx,attrs,defStyleAttr);
        isRecordVideo = false;
        mCtx = ctx;
        getScreenMetrix(ctx);
        initView();
        setFlashMode(0);
    }
    // endregion

    // region Preview
    public void startPreview() {
        if (mCamera != null) {
            mCamera.startPreview();
        } else  {
            initView(); // 从Preview返回时
        }
    }

    public void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    // endregion

    // region Initial

    public void setup(String picDir, String vidDir, CameraEventListener listener) {
        pictureDir = picDir;
        videoDir = vidDir;
        mListener = listener;
    }

    private void getScreenMetrix(Context ctx) {
        WindowManager windowManager = (WindowManager)ctx.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
    }

    private void initView() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void freeResource() {

        releaseRecorder();

        releaseCamera();

        releaseHolder();
    }

    private void releaseRecorder() {

        if (recorder != null) {
            stopRecordVideo();
            recorder.release();
            recorder = null;
        }
    }

    private void releaseCamera() {

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }
        mCamera = null;
    }

    private void releaseHolder() {

        if (mHolder != null) {
            mHolder.addCallback(null);
            mHolder = null;
        }
    }

    private void resetResource() {
        freeResource();
        initView();
    }

    // endregion

    // region Switch Mode

    public void switchToRecordVideo() {
        isRecordVideo = true;
        initRecorder();
        if (mListener != null) {
            mListener.cameraDidChangedToRecordVideo();
        }
    }

    public void switchToTakePhoto() {
        isRecordVideo = false;
        releaseRecorder();
        if (mListener != null) {
            mListener.cameraDidChangedToTakePicture();
        }
    }

    public void switchCamera() {
        switchCamera(!isFront);
    }

    private void switchCamera(boolean isSwitchFront) {

        if (isSwitchFront == isFront) {
            return;
        }

        int cameraCount = Camera.getNumberOfCameras();
        if (cameraCount <= 1) {
            return;
        }

        resetResource();

        mCamera = openCamera(isSwitchFront);
        if (mCamera == null) {
            return;
        }

        startCameraPreview();

        if (mListener != null) {
            mListener.cameraDidChanged(isFront);
        }

        if (isRecordVideo) {
            switchToRecordVideo();
        }

    }

    private void startCameraPreview() {

        try {
            setCameraParams(mCamera,getWidth(),getHeight());
            mCamera.setPreviewDisplay(mHolder); // 通过surfaceview显示取景画面
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();//开始预览
    }

    private Camera openCamera(boolean isSwitchFront) {

        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera camera = null;
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && isSwitchFront) {//获取前置摄像头
                camera = Camera.open(i);
                isFront = true;
                mCameraID = i;
                break;
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK && !isSwitchFront) {//获取后置摄像头
                camera = Camera.open(i);
                isFront = false;
                mCameraID = i;
                break;
            }
        }

        if (camera == null) {

            if (isSwitchFront) {
                if (errorHandler != null) {
                    errorHandler.initCameraError("Can't open front camera");
                }
            } else {
                if (errorHandler != null) {
                    errorHandler.initCameraError("Can't open back camera");
                }
            }
        }

        return camera;
    }

    // endregion

    // region Setting
    public void setFlashMode(int flashMode) {
        if (this.flashMode == flashMode) {
            return;
        }
        if (mCamera == null) {
            return;
        }

        this.flashMode = flashMode;
        Camera.Parameters params = mCamera.getParameters();
        List<String> flashModes =  params.getSupportedFlashModes();
        switch (flashMode) {
            case -1: {
                if (flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                }
            }
            break;
            case 0: {
                if (flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                }
            }
            break;
            case 1: {
                if (flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                }
            }
            break;
            default: {
                if (flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                }
            }
        }
        mCamera.setParameters(params);

        if (mListener != null) {
            mListener.cameraDidChangedFlashMode(flashMode);
        }
    }

    // 宽高 是 surface view 的宽高
    private void setCameraParams(Camera camera,int width,int height) {

        // 相机Sensor坐标为手机右上角为圆点，向下为x轴正方形，向左为y轴正方形。
        // 这与视图坐标系是相反的，而surface与sensor保持一致。

        Camera.Parameters params = camera.getParameters();
        // 获取摄像头支持的PictureSize列表
        List<Camera.Size> pictureSizeList = params.getSupportedPictureSizes();
        // 选取适合的分辨率
        Camera.Size pictureSize = getPictureSize(pictureSizeList,height, width);
        if (pictureSize == null) {
            pictureSize = params.getPictureSize();
        }
        // 设置图片大小，Size不正确保存的图像清晰度就不够，或者程序崩溃。
        params.setPictureSize(pictureSize.width,pictureSize.height);

        // 获取摄像头支持的PreviewSize列表
        List<Camera.Size> previewSizeList = params.getSupportedPreviewSizes();
        Camera.Size previewSize = getPreviewSize(previewSizeList,width, height);
        if (previewSize == null) {
            previewSize = params.getPreviewSize();
        }
        if (previewSize != null) {
            params.setPreviewSize(previewSize.width,previewSize.height);
        }

        // 设置照片质量
        params.setJpegQuality(100);

        // 设置聚焦
        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE); // 自动聚焦模式
        }

        // 设置白平衡
        if (params.getSupportedWhiteBalance().contains(Camera.Parameters.WHITE_BALANCE_AUTO)) {
            params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        }

//        // 设置曝光
//        if (params.isAutoExposureLockSupported()) {
//            params.setAutoExposureLock(true);
//        }

        // 设置Flash
        List<String> flashModes =  params.getSupportedFlashModes();
        if (flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
            params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        }

        // 设置图片保存格式
        params.setPictureFormat(ImageFormat.JPEG);
        camera.setDisplayOrientation(90); // 保持竖屏
        camera.setParameters(params);
        camera.cancelAutoFocus();

    }

    private void sortSizeList(List<Camera.Size> sizeList) {

        Collections.sort(sizeList, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size o1, Camera.Size o2) {

                if (o1.width > o2.width) {

                    return -1;
                } else if (o1.height > o2.height) {

                    return -1;
                }

                return 1;
            }
        });
    }

    private Camera.Size getPictureSize(List<Camera.Size> pictureSizeList, int w, int h) {

        sortSizeList(pictureSizeList);
        return pictureSizeList.get(0);
    }

    private Camera.Size getVideoSize(List<Camera.Size> videoSizeList, int w, int h) {

        sortSizeList(videoSizeList);
        return videoSizeList.get(0);
    }

    // w h 取 surface view的宽高
    private Camera.Size getPreviewSize(List<Camera.Size> pictureSizeList, int w, int h) {

        sortSizeList(pictureSizeList);

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w; // 由于保持竖屏，所以使用 h / w，横屏 w / h
        if (pictureSizeList == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : pictureSizeList) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : pictureSizeList) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    // endregion

    // region Override

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mCamera == null) {
//            mCamera = Camera.open();
//            isFront = false;

            mCamera = openCamera(false);
            if (mCamera == null) {
//                // 后置摄像头打不开
//                if (errorHandler != null) {
//                    errorHandler.initCameraError("Can't open back camera");
//                }
                return;
            }

        }
        try {
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        setCameraParams(mCamera,getWidth(),getHeight());
        mCamera.startPreview();

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        freeResource();
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        if (success) {
            Log.d("CameraSurfaceView", "onAutoFocus: success" + success);
        }
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        Log.d("Media Error", "onError: " + mr);
        if (errorHandler != null) {
            errorHandler.recordError(mr);
        }
    }

    // endregion

    // region Take Picture

    public void takePhotoToFile(final CaptureImageCallback callback) {

        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                dealWithCameraData(data, callback);
                if (mListener != null) {
                    mListener.cameraDidTakePicture();
                }
            }
        });

    }


    private void dealWithCameraData(byte[] data, CaptureImageCallback callback) {
        if (data == null) {
            if (callback != null) {
                callback.captureImageCompletion(null, null);
            }
            return;
        }

        if (pictureDir == null) {
            if (callback != null) {

                // 调用camera.takePiture方法后，camera关闭了预览，这时需要调用startPreview()来重新开启预览。
                // 如果不再次开启预览，则会一直停留在拍摄照片画面。
                mCamera.stopPreview();
                mCamera.startPreview();

                callback.captureImageCompletion(data, null);
            }
            return;
        }

        File dirF = new File(pictureDir);
        if (!dirF.exists()) {
            dirF.mkdirs();
        }

        String date = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String pictureName = String.format("IMG_%s.jpg",date);
        File picF = new File(dirF, pictureName);

        if (picF != null) {

            // 保存的相片仍然是摄像头成像，需要做旋转。前置摄像头翻转270，后翻转90
//            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//            Matrix matrix = new Matrix();
//            if (isFront) {
//                matrix.postRotate(270);
//            } else {
//                matrix.postRotate(90);
//            }
//            bitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);

            int rotate = 90;
            if (isFront) {
                rotate = 270;
            } else {
                rotate = 90;
            }

            FileOutputStream fos = null;
            try {

                fos = new FileOutputStream(picF);
                fos.write(data);
                fos.flush();
                fos.close();

                setPictureOrientation(picF.getAbsolutePath(), rotate);// 旋转图片，调整方向

                if (callback != null) {
                    callback.captureImageCompletion(data, picF.getAbsolutePath());
                }

            } catch (IOException e) {
                e.printStackTrace();

                if (callback != null) {
                    callback.captureImageCompletion(data, null);
                }

            } finally {
                try {

                    fos.flush();
                    fos.close();
//                    bitmap.recycle(); // 回收bitmap空间

                    // 调用camera.takePiture方法后，camera关闭了预览，这时需要调用startPreview()来重新开启预览。
                    // 如果不再次开启预览，则会一直停留在拍摄照片画面。
                    mCamera.stopPreview();
                    mCamera.startPreview();


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private static void setPictureOrientation(String path, int orientation) {
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            // 修正图片的旋转角度，设置其不旋转。这里也可以设置其旋转的角度，可以传值过去，
            // 例如旋转90度，传值ExifInterface.ORIENTATION_ROTATE_90，需要将这个值转换为String类型的
            int rotate = ExifInterface.ORIENTATION_ROTATE_90;
            if (orientation == 90) {
                rotate = ExifInterface.ORIENTATION_ROTATE_90;
            } else if (orientation == 270) {
                rotate = ExifInterface.ORIENTATION_ROTATE_270;
            }
            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(rotate));
            exifInterface.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // endregion


    // region Record Video
    private void initRecorder() {
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
        recorder = new MediaRecorder(); // Create MediaRecorder

        // Step 1: Unlock and set camera to MediaRecorder
        recorder.setCamera(mCamera);

        // Step 2: Set sources
        recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        // 注意这里，最好和预览设置的尺寸保持一直，否则录制时的视频会出现放大或缩小的情况
        recorder.setProfile(CamcorderProfile.get(mCameraID, CamcorderProfile.QUALITY_1080P));

        // Step 4: Set the preview output
        recorder.setPreviewDisplay(mHolder.getSurface());

        // Orientation
        if (isFront) {
            recorder.setOrientationHint(270);
        } else {
            recorder.setOrientationHint(90);
        }
    }

    private File createVideoFile() {

        File dirF = new File(videoDir);
        if (!dirF.exists()) {
            dirF.mkdirs();
        }

        String date = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String pictureName = String.format("VID_%s.mp4",date);
        File vidF = new File(dirF, pictureName);

        return vidF;
    }

    public void recordVideo(RecordVideoCallback callback) {
        if (isRecording) {
            return;
        }

        File vidF = createVideoFile();
        mVidPath = vidF.getAbsolutePath();

        try {
            mCamera.unlock();
            recorder.setOutputFile(mVidPath);
            recorder.prepare();
            recorder.start();
            isRecording = true;

            mVidCallback = callback;

            if (mListener != null) {
                mListener.cameraDidStartRecordVideo();
            }

            Log.d("Record", "recordVideo: ");
        } catch (Exception e) {
            e.printStackTrace();
            mVidCallback = null;
            mVidPath = null;
            mCamera.lock();
        }
    }

    public void stopRecordVideo() {
        if (recorder != null && isRecording) {
            recorder.stop();
            isRecording = false;
            mCamera.lock();

            if (mVidCallback != null) {
                mVidCallback.recordVideoCompletion(mVidPath);
            }

            recorder.release();
            recorder = null;

            if (mListener != null) {
                mListener.cameraDidStopRecordVideo();
            }

            Log.d("Record", "stopRecordVideo: ");
        }
        mVidCallback = null;
        mVidPath = null;

        initRecorder();
    }
    // endregion
}
