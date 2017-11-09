package com.example.macmini1.takephoto;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.macmini1.takephoto.Crop.CropImageActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class CameraActivity extends Activity implements View.OnTouchListener{

    private ImageButton takeBtn;
    private ImageButton modeSwitchBtn;
    private ImageButton cameraSwitchBtn;
    private CameraSurfaceView surfaceView;
    private TextView timerTv;

    private boolean isRecordVideo;
    private boolean isRecording;
    private boolean isFront;
    private String photoDir;
    private String videoDir;
    private boolean canRecordVideo;
    private boolean canTakePhoto;

    private long  mRecordSeconds;
    private Timer mRecordTimer;
    private TimerTask mRecordTimerTask;

    private void startTimer() {
        mRecordTimer = new Timer();
        mRecordTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (timerTv != null) {
                    mRecordSeconds++;
                    long second = mRecordSeconds % 60;
                    long minute = mRecordSeconds / 60;
                    final String time = String.format("%02d:%02d",minute,second);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            timerTv.setText(time);
                        }
                    });
                }
            }
        };
        mRecordTimer.schedule(mRecordTimerTask, new Date(),1000);
        if (timerTv != null) {
            timerTv.setVisibility(View.VISIBLE);
        }
    }

    private void stopTimer() {
        if (mRecordTimer != null) {
            mRecordTimer.cancel();
            mRecordTimer = null;
            mRecordTimerTask.cancel();
            mRecordTimerTask = null;
        }
        if (timerTv != null) {
            timerTv.setText("");
            timerTv.setVisibility(View.INVISIBLE);
        }
    }

    private void init() {
        Intent intent = getIntent();
        if (intent != null) {

            int camera_mode = intent.getIntExtra("cameraMode",CameraSurfaceView.CameraMode_Take_Photo);
            int camera_face = intent.getIntExtra("cameraFace",Camera.CameraInfo.CAMERA_FACING_BACK);
            photoDir = intent.getStringExtra("photoDir");
            videoDir = intent.getStringExtra("videoDir");
            canRecordVideo = intent.getBooleanExtra("recordVideo",true);
            canTakePhoto = intent.getBooleanExtra("takePhoto",true);

            if (canRecordVideo && !canTakePhoto) {
                camera_mode = CameraSurfaceView.CameraMode_Record_Video;
                modeSwitchBtn.setVisibility(View.INVISIBLE);
                // 设置快门按钮图标--->record
                takeBtn.setImageDrawable(getResources().getDrawable(R.drawable.start_record));
            }
            if (canTakePhoto && !canRecordVideo) {
                camera_mode = CameraSurfaceView.CameraMode_Take_Photo;
                modeSwitchBtn.setVisibility(View.INVISIBLE);
                // 设置快门按钮图标--->take photo
                takeBtn.setImageDrawable(getResources().getDrawable(R.drawable.take_photo));
            }


            if (camera_mode == CameraSurfaceView.CameraMode_Record_Video) {
                isRecordVideo = true;
            } else {
                isRecordVideo = false;
            }

            if (camera_face == Camera.CameraInfo.CAMERA_FACING_BACK) {
                isFront = false;
            } else {
                isFront = true;
            }

            surfaceView.initCamera(camera_mode, camera_face);
        }
    }

    private String getApplicationName() {
        PackageManager packageManager = null;
        ApplicationInfo applicationInfo = null;
        try {
            packageManager = getApplicationContext().getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        String applicationName = (String) packageManager.getApplicationLabel(applicationInfo);
        return applicationName;
    }

    @Nullable
    private String photoDirectory() {
        String dir = null;
        if (photoDir != null) {
            dir = photoDir;
        } else {
            dir = Environment.getExternalStorageDirectory().getPath() + '/' + getApplicationName() + "/photo";
        }

        File file = new File(dir);
        if (!file.exists()) {
            if (file.mkdirs()) {
                return dir;
            } else {
                return null;
            }
        }
        return dir;
    }

    @Nullable
    private String videoDirectory() {
        String dir = null;
        if (videoDir != null) {
            dir = videoDir;
        } else {
            dir = Environment.getExternalStorageDirectory().getPath() + '/' + getApplicationName() + "/video";
        }

        File file = new File(dir);
        if (!file.exists()) {
            if (file.mkdirs()) {
                return dir;
            } else {
                return null;
            }
        }
        return dir;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

//        Log.d("Directory", photoDirectory());

        setContentView(R.layout.activity_main);
        isFront = false;
        isRecordVideo = false;
        isRecording = false;

        surfaceView = (CameraSurfaceView)findViewById(R.id.cameraSurfaceView);

        surfaceView.editor = new CameraSurfaceView.PhotoEditor() {
            @Override
            public void editPhoto(File file, float rotate) {

                CropPictureFile(file);
            }

        };

        modeSwitchBtn = (ImageButton)findViewById(R.id.cameraModeSwitchBtn);
        modeSwitchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRecordVideo = !isRecordVideo;
                if (isRecordVideo) {
                    modeSwitchBtn.setImageDrawable(getResources().getDrawable(R.drawable.photo_mode));
                    surfaceView.switchRecordVideo();
                    takeBtn.setImageDrawable(getResources().getDrawable(R.drawable.start_record));
                } else {
                    modeSwitchBtn.setImageDrawable(getResources().getDrawable(R.drawable.record_mode));
                    surfaceView.swithToTakePhoto();
                    takeBtn.setImageDrawable(getResources().getDrawable(R.drawable.take_photo));
                }
            }
        });


        takeBtn = (ImageButton)findViewById(R.id.takePhotoBtn);
        takeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



                Date date= new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss" );
                String file_name = dateFormat.format(date);

                if (isRecordVideo) {

                    if (isRecording) {
                        surfaceView.stopRecordVideo();
                        isRecording = false;
                        takeBtn.setImageDrawable(getResources().getDrawable(R.drawable.start_record));
                        cameraSwitchBtn.setVisibility(View.VISIBLE);
                        modeSwitchBtn.setVisibility(View.VISIBLE);
                        stopTimer();
                    } else  {
                        String dir = videoDirectory();
                        if (dir != null) {
                            String tempStr = dir + "/" + file_name + ".mp4";
                            surfaceView.recordVideoToPath(tempStr);
                            isRecording = true;
                            takeBtn.setImageDrawable(getResources().getDrawable(R.drawable.stop_record));
                            cameraSwitchBtn.setVisibility(View.INVISIBLE);
                            modeSwitchBtn.setVisibility(View.INVISIBLE);
                            startTimer();
                        }
                    }

                } else {
                    String dir = photoDirectory();
                    if (dir != null) {
                        String tempStr = dir + "/" + file_name + ".jpg";
                        File file = new File(tempStr);
                        surfaceView.takePhotoToFile(file);
                    }
                }


            }
        });


        cameraSwitchBtn = (ImageButton)findViewById(R.id.cameraSwitchBtn);
        cameraSwitchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                isFront = !isFront;
                surfaceView.switchFrontCamera(isFront);
            }
        });

        timerTv = (TextView)findViewById(R.id.timer_tv);

        init();

        cameraSwitchBtn.setOnTouchListener(this);
        takeBtn.setOnTouchListener(this);
        modeSwitchBtn.setOnTouchListener(this);

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if(event.getAction() == MotionEvent.ACTION_DOWN){
            // 按下时
            v.setAlpha(0.5f);
        }else if(event.getAction() == MotionEvent.ACTION_UP){
            // 抬起时
            v.setAlpha(1.0f);
        }

        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopTimer();
        surfaceView.stopPreview();
        isRecording = false;
    }

    @Override
    protected void onStop() {
        super.onStop();

        surfaceView.stopPreview();
        isRecording = false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        surfaceView.startPreview();
        if (isRecordVideo) {
            takeBtn.setImageDrawable(getResources().getDrawable(R.drawable.start_record));
            cameraSwitchBtn.setVisibility(View.VISIBLE);
            modeSwitchBtn.setVisibility(View.VISIBLE);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        surfaceView.startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        surfaceView.stopPreview();
        isRecording = false;
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        surfaceView.startPreview();
        if (isRecordVideo) {
            takeBtn.setImageDrawable(getResources().getDrawable(R.drawable.start_record));
            cameraSwitchBtn.setVisibility(View.VISIBLE);
            modeSwitchBtn.setVisibility(View.VISIBLE);
        }
    }


    /**系统拍照裁剪**/
    private File photoFile = null;

    @Nullable
    private Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.Media._ID },
                MediaStore.Images.Media.DATA + "=? ",
                new String[] { filePath }, null);

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    private final static int CROP_REQUEST_CODE = 1024;

    private void CropPictureFile(File file) {

        photoFile = file;
//       try {
//           Uri uri = getImageContentUri(this,file);
//           Intent intent = new Intent("com.android.camera.action.CROP");
//           intent.setDataAndType(uri, "image/*");
//           intent.putExtra("crop", "true");
//           intent.putExtra("aspectX", 1);
//           intent.putExtra("aspectY", 1);
//           intent.putExtra("outputX", 1024);
//           intent.putExtra("outputY", 1024);
//           intent.putExtra("return-data", false);
//           intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//           intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
//           intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
//           intent.putExtra("noFaceDetection", true);
//           startActivityForResult(intent, CROP_REQUEST_CODE);
//       } catch (Exception e) {
           CropImageActivity.Builder builder = new CropImageActivity.Builder(1024,1024,photoFile,photoFile);
           Intent intent = builder.getIntent(this);
           startActivityForResult(intent, CROP_REQUEST_CODE);
//       }

//        Intent intent = new Intent(this,PhotoEditActivity.class);
//        intent.putExtra("file",photoFile);
//        startActivity(intent);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CROP_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Log.d("CameraActivity", "onActivityResult: Crop Image Ok");

            } else {
                Log.d("CameraActivity", "onActivityResult: Crop Image Cancel");
                if (photoFile.exists()) {
                    photoFile.delete();
                }

            }
            photoFile = null;
        }

    }
}
