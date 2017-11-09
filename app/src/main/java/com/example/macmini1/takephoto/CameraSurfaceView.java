package com.example.macmini1.takephoto;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by macmini1 on 2017/7/27.
 */

public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.AutoFocusCallback,MediaRecorder.OnInfoListener,MediaRecorder.OnErrorListener {


    public interface PhotoEditor {
        void editPhoto(File file, float rotate);
    }


    private Context mCtx;
    private SurfaceHolder mHolder;
    private Camera mCamera;

    private int mScreenWidth;
    private int mScreenHeight;

    private File photoFile;
    public PhotoEditor editor;
    private int flashMode;

    private boolean isFront;
    private int cameraPosition;

    public final static int CameraMode_Take_Photo = 0;
    public final static int CameraMode_Record_Video = 1;
    public int mCameraMode;


    public CameraSurfaceView(Context context) {
        this(context,null);
    }

    public CameraSurfaceView(Context ctx, AttributeSet attrs) {
        this(ctx,attrs,0);
    }

    public CameraSurfaceView(Context ctx, AttributeSet attrs, int defStyleAttr) {
        super(ctx,attrs,defStyleAttr);
        boolean hasCamera = checkCameraHardware(ctx);
        if (!hasCamera) {
            return;
        }
        mCtx = ctx;
        getScreenMetrix(ctx);
        cameraPosition = getCameraPosition(Camera.CameraInfo.CAMERA_FACING_BACK);
        isFront = false;
        mCameraMode = CameraMode_Take_Photo;
        flashMode = 0;
        initView();
    }

    public void initCamera(int camera_mode, int camera_face) {
        switch (camera_mode) {
            case CameraMode_Take_Photo: {
                mCameraMode = CameraMode_Take_Photo;
            }
            break;
            case CameraMode_Record_Video: {
                mCameraMode = CameraMode_Record_Video;
            }
            break;
            default: {
                mCameraMode = CameraMode_Take_Photo;
            }
            break;
        }
        cameraPosition = getCameraPosition(camera_face);
        if (camera_face == Camera.CameraInfo.CAMERA_FACING_BACK) {
            isFront = false;
        }
        if (camera_face == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            isFront = true;
        }
    }


    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
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
    /*****/

    private  Camera.Size getPreviewSize(List<Camera.Size> list, int th){
        Collections.sort(list, new CameraSizeComparator());

        int i = 0;
        for(Camera.Size s:list){
            if((s.width > th) && equalRate(s, 1.33f)){
                Log.i("Preview Size", "最终设置预览尺寸:w = " + s.width + " h = " + s.height);
                break;
            }
            i++;
        }

        return list.get(i);
    }
    private Camera.Size getPictureSize(List<Camera.Size> list, int th){
        Collections.sort(list, new CameraSizeComparator());

        int i = 0;
        for(Camera.Size s:list){
            if((s.width > th) && equalRate(s, 1.33f)){
                Log.i("Picture Size", "最终设置图片尺寸:w = " + s.width + " h = " + s.height);
                break;
            }
            i++;
        }

        return list.get(i);
    }

    private boolean equalRate(Camera.Size s, float rate){
        float r = (float)(s.width)/(float)(s.height);
        if(Math.abs(r - rate) <= 0.2)
        {
            return true;
        }
        else{
            return false;
        }
    }

    private  class CameraSizeComparator implements Comparator<Camera.Size> {
        //按降序
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            // TODO Auto-generated method stub
            if(lhs.width == rhs.width){
                return 0;
            }
            else if(lhs.width > rhs.width){
                return -1;
            }
            else{
                return 1;
            }
        }

    }



    /*****/
    // 默认 w : h = 4 : 3
    private Camera.Size getProperSize(List<Camera.Size> pictureSizeList, float screenRatio) {

//        Log.d("Property Size", "Ratio: " + screenRatio);
        Collections.sort(pictureSizeList, new CameraSizeComparator());
        Camera.Size result = null;
        for (Camera.Size size : pictureSizeList) {
            float currentRatio = ((float) size.width) / size.height;
//            Log.d("Size", "size " + size + " ration: " + currentRatio);
            if (currentRatio - screenRatio == 0) {
                result = size;
                break;
            }
        }

        if (null == result) {
            for (Camera.Size size : pictureSizeList) {
                float curRatio = ((float) size.width) / size.height;
                if (curRatio == 4f / 3) {// 默认w:h = 4:3
                    result = size;
                    break;
                }
            }
        }

        return result;
    }

    public void setFlashMode(int flashMode) {
        if (this.flashMode == flashMode) {
            return;
        }
        if (mCamera == null) {
            return;
        }

        this.flashMode = flashMode;
        Camera.Parameters params = mCamera.getParameters();
        setParamsFlashMode(params,flashMode);

        mCamera.setParameters(params);
    }

    public int getFlashMode() {
        return this.flashMode;
    }

    private void setParamsFlashMode(Camera.Parameters params,int flashMode) {
        if (params == null) {
            return;
        }
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
    }

    private int getCameraDisplayOrientation() {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo (cameraPosition , info);
        WindowManager windowManager = (WindowManager)mCtx.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay ().getRotation ();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;   // compensate the mirror
        } else {
            // back-facing
            result = ( info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    private void changeLayoutSize(float width, float height) {
        this.setLayoutParams(new FrameLayout.LayoutParams(getWidth(), (int) (getWidth() * ((width * 1.0) / height))));
    }

    private void setCameraParamSize(Camera.Parameters params, float ration) {
        // 获取摄像头支持的PictureSize列表
        List<Camera.Size> pictureSizeList = params.getSupportedPictureSizes();
        // 选取适合的分辨率
        Camera.Size pictureSize = getProperSize(pictureSizeList, ration);

        if (pictureSize == null) {
            pictureSize = params.getPictureSize();


        }
        // 设置图片大小，Size不正确保存的图像清晰度就不够，或者程序崩溃。
        params.setPictureSize(pictureSize.width,pictureSize.height);

        // 获取摄像头支持的PreviewSize列表
        List<Camera.Size> previewSizeList = params.getSupportedPreviewSizes();
        Camera.Size previewSize = getProperSize(previewSizeList,ration);

        if (previewSize == null) {
            previewSize = params.getPreviewSize();
        }
        if (previewSize != null) {
            params.setPreviewSize(previewSize.width,previewSize.height);

            float w = previewSize.width;
            float h = previewSize.height;
            changeLayoutSize(w,h);
        }
    }

    private void setCameraParams(Camera camera,int width,int height) {
        Camera.Parameters params = camera.getParameters();

        if (mCameraMode == CameraMode_Take_Photo) { //  解决录像模式下预览变形
            changeNormalCameraParams(params);
        } else {
            changeRecordCameraParams(params);
        }

        // 设置照片质量
        params.setJpegQuality(100);

        // 设置聚焦
        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO); // 自动聚焦模式
        }

        // 设置白平衡
        List<String> whiteBalanceModes = params.getSupportedWhiteBalance();
        if (whiteBalanceModes != null && whiteBalanceModes.contains(Camera.Parameters.WHITE_BALANCE_AUTO)) {
            params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        }

//        // 设置曝光（前置摄像头黢黑一片）
//        if (params.isAutoExposureLockSupported()) {
//            params.setAutoExposureLock(true);
//        }

        // 设置Flash
        setParamsFlashMode(params,this.flashMode);

        // 设置图片保存格式
        params.setPictureFormat(ImageFormat.JPEG);
        int displayOrientation = getCameraDisplayOrientation();
        camera.setDisplayOrientation(displayOrientation);
        camera.setParameters(params);

    }

    /***/

    @Override
    public void onAutoFocus(boolean success, Camera camera) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mCamera == null) {
            mCamera = Camera.open(cameraPosition);
            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        setCameraParams(mCamera,getWidth(),getHeight());
        mCamera.startPreview();

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        releaseRecorder();
        releaseCamera();
        mHolder = null;
    }

    /** Take Photo */

    public void takePhotoToFile(File file) {
        if (mCamera == null) {
            return;
        }
        photoFile = file;
        mCamera.setErrorCallback(new Camera.ErrorCallback() {
            @Override
            public void onError(int error, Camera camera) {
                Log.d("Camera Error", "onError: " + error + "----Camera: " + camera);
            }
        });
        mCamera.takePicture(new Camera.ShutterCallback() {
            @Override
            public void onShutter() {
                Log.d("Camera", "onShutter: ");
            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.d("Camera", "onPictureTaken: ");
            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                dealWithCameraData(data);
            }
        });
    }


    private void dealWithCameraData(byte[] data) {
        if (data == null) {
            return;
        }
        if (photoFile != null) {

            // 保存的相片仍然是摄像头成像，需要做旋转。前置摄像头翻转270，后翻转90
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length); // 解码费时
            Matrix matrix = new Matrix();
            if (isFront) {
                matrix.postRotate(270);
            } else {
                matrix.postRotate(90);
            }
            bitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);

            float rotate = 90;
            if (isFront) {
                rotate = 270;
            } else {
                rotate = 90;
            }

            FileOutputStream fos = null;
            try {

                fos = new FileOutputStream(photoFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);
                fos.write(data);
                fos.flush();
                fos.close();

                if (this.editor != null) {
                    this.editor.editPhoto(photoFile,rotate);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {

                    fos.flush();
                    fos.close();
                    bitmap.recycle(); // 回收bitmap空间

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

    /***/
    public void stopPreview() {

        releaseRecorder();
        releaseCamera();

    }

    public void startPreview() {
        if (mCamera != null) {
            mCamera.startPreview();
        } else {
            initView();
        }
    }

     /***/

     public void switchFrontCamera(boolean isSwitchFront) {

         if (isSwitchFront ==isFront) {
             return;
         }

         if (mCameraMode == CameraMode_Record_Video) {

//             releaseRecorder();
             if (isRecording == true) {
                 return;
             }
         }


         // 切换摄像头必须先释放旧的再打开新的
         releaseCamera();

         int cameraPosition = -1;
         if (isSwitchFront == true) {
             cameraPosition = getCameraPosition(Camera.CameraInfo.CAMERA_FACING_FRONT);
         } else {
             cameraPosition = getCameraPosition(Camera.CameraInfo.CAMERA_FACING_BACK);
         }
         if (cameraPosition == -1) {
             return;
         }
         Camera camera = Camera.open(cameraPosition);
         if (camera == null) {
             return;
         }
         this.cameraPosition = cameraPosition;
         isFront = isSwitchFront;


         setCameraParams(camera,getWidth(),getHeight());
         try {
             camera.setPreviewDisplay(mHolder); // 通过surfaceview显示取景画面
         } catch (IOException e) {
             e.printStackTrace();
         }

         camera.startPreview();//开始预览

         mCamera = camera;



     }

     private int getCameraPosition(int cameraFacing) {
         int cameraCount = Camera.getNumberOfCameras();
         Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
         for (int i = 0; i < cameraCount; i++) {
             Camera.getCameraInfo(i, cameraInfo);
             if (cameraInfo.facing == cameraFacing) {

                 return i;

             }
         }
         return -1;
     }


    /** RecordVideo */
    private MediaRecorder mRecorder;
    private boolean isRecording;

    private void changeNormalCamera() {
        Camera.Parameters params = mCamera.getParameters();

        changeNormalCameraParams(params);

        mCamera.setParameters(params);
    }

    private void changeNormalCameraParams(Camera.Parameters params) {
        float w = (float) getWidth();
        float h = (float) getHeight();
        setCameraParamSize(params,w / h);
    }

    private void changeRecordCameraParams(Camera.Parameters params) {
        int quality = CamcorderProfile.QUALITY_LOW;
        for (int i = 1; i <= 7; i++) {
            if (i == 1) {
                quality = CamcorderProfile.QUALITY_2160P;
            } else if (i == 2) {
                quality = CamcorderProfile.QUALITY_1080P;
            } else if (i == 3) {
                quality = CamcorderProfile.QUALITY_720P;
            } else if (i == 4) {
                quality = CamcorderProfile.QUALITY_480P;
            } else if (i == 5) {
                quality = CamcorderProfile.QUALITY_CIF;
            } else if (i == 6) {
                quality = CamcorderProfile.QUALITY_QVGA;
            } else if (i == 7) {
                quality = CamcorderProfile.QUALITY_QCIF;
            }

            if (CamcorderProfile.hasProfile(cameraPosition,quality)) {
                CamcorderProfile profile = CamcorderProfile.get(quality);
                float w = (float) profile.videoFrameWidth;
                float h = (float) profile.videoFrameHeight;

                Log.d("Video", "ration: " + w / h);
                Log.d("Video", "width: " + w + " height: " + h);
//                changeLayoutSize(profile.videoFrameWidth,profile.videoFrameHeight);



                setCameraParamSize(params,w / h);



                break;
            }

        }
    }

    private void changeRecordCamera() {
        Camera.Parameters params = mCamera.getParameters();
        changeRecordCameraParams(params);
        mCamera.setParameters(params);
    }

    public void swithToTakePhoto() {
        if (mCameraMode == CameraMode_Record_Video && isRecording == true) {
            return;
        }

        if (mCameraMode == CameraMode_Take_Photo) {
            return;
        }

        releaseRecorder();

        mCameraMode = CameraMode_Take_Photo;

        changeNormalCamera();
    }

    private void initMediaRecorder(Camera camera) {
         if (mRecorder == null) {
             mRecorder = new MediaRecorder();
             mRecorder.setOnErrorListener(this);
             mRecorder.setOnInfoListener(this);
         } else {
             mRecorder.reset();
         }

         camera.unlock();
         mRecorder.setCamera(camera);

//         mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//         mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//         mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//         mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//         mRecorder.setAudioEncodingBitRate(5 * 1024 * 1024);
//         mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//         mRecorder.setVideoEncodingBitRate(2 * 1024 * 1024);
//
//
//
//        // 设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
//         mRecorder.setVideoSize(640,480);
//
//         // 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错
//         mRecorder.setVideoFrameRate(20);
//
//         mRecorder.setPreviewDisplay(mHolder.getSurface());

        // Step 2: Set sources
        mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        int quality = CamcorderProfile.QUALITY_LOW;
        for (int i = 1; i <= 7; i++) {
            if (i == 1) {
                quality = CamcorderProfile.QUALITY_2160P;
            } else if (i == 2) {
                quality = CamcorderProfile.QUALITY_1080P;
            } else if (i == 3) {
                quality = CamcorderProfile.QUALITY_720P;
            } else if (i == 4) {
                quality = CamcorderProfile.QUALITY_480P;
            } else if (i == 5) {
                quality = CamcorderProfile.QUALITY_CIF;
            } else if (i == 6) {
                quality = CamcorderProfile.QUALITY_QVGA;
            } else if (i == 7) {
                quality = CamcorderProfile.QUALITY_QCIF;
            }

            if (CamcorderProfile.hasProfile(cameraPosition,quality)) {
                CamcorderProfile profile = CamcorderProfile.get(quality);
                float w = (float) profile.videoFrameWidth;
                float h = (float) profile.videoFrameHeight;
                mRecorder.setProfile(profile);

                Log.d("Video", "ration: " + w / h);
                Log.d("Video", "width: " + w + " height: " + h);
                changeLayoutSize(profile.videoFrameWidth,profile.videoFrameHeight);

                break;
            }

        }


        // Step 5: Set the preview output
        mRecorder.setPreviewDisplay(mHolder.getSurface());

    }

     public void switchRecordVideo() {
         if (mCameraMode == CameraMode_Record_Video) {
             return;
         }
         mCameraMode = CameraMode_Record_Video;
         changeRecordCamera();
     }

     public void recordVideoToPath(String path) {
         if (isRecording == true) {
             return;
         }

         if (mRecorder == null) {
             if (mCameraMode == CameraMode_Record_Video) {
                 initMediaRecorder(mCamera);
             } else {
                 return;
             }
         }

         if (TextUtils.isEmpty(path)) {
             return;
         }

         try {

             mRecorder.setOutputFile(path);
             mRecorder.prepare();
             mRecorder.start();
             isRecording = true;
         } catch (IOException e) {
             e.printStackTrace();
         }
     }

     public void stopRecordVideo() {
         if (mRecorder != null && isRecording == true) {
             mRecorder.stop();
             isRecording = false;
         }
         releaseRecorder();
     }

     private void releaseRecorder() {
         if (mRecorder != null && isRecording == true) {
             mRecorder.stop();
             isRecording = false;
         }
         if (mRecorder != null) {
             mRecorder.reset();
             mRecorder.setPreviewDisplay(null);
             mRecorder.setOnErrorListener(null);
             mRecorder.setOnInfoListener(null);
             mRecorder.release();
             mRecorder = null;

             if (mCamera != null) {
                 mCamera.lock(); // 必须要lock，否则在切换到 拍照模式 和 切换摄像头 时会报错
             }
         }

     }

     private void releaseCamera() {
         if (mCamera != null) {
             mCamera.stopPreview();
             mCamera.release();
             mCamera = null;
         }
     }

     /****/

     @Override
     public void onError(MediaRecorder mr, int what, int extra) {

         Log.d("Media Error", "onError: what" + what + "extra  ->" + extra);

     }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        Log.d("Media Info", "onInfo: what" + what + "extra  ->" + extra);
    }

     /****/

}
