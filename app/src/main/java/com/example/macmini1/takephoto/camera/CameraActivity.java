package com.example.macmini1.takephoto.camera;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.macmini1.takephoto.R;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;

public class CameraActivity extends Activity {

    private static final String CAMERA_PICTURE_DIR_KEY = "CAMERA_PICTURE_DIR_KEY";
    private static final String CAMERA_VIDEO_DIR_KEY = "CAMERA_VIDEO_DIR_KEY";

    private static final String SDCARD = "/sdcard";

    public static Intent buildCameraActivity(Activity activity, String pictureDir, String videoDir) {
        if (activity != null) {

            Intent intent = new Intent(activity, CameraActivity.class);
            if (pictureDir == null) {
                pictureDir = SDCARD;
            }
            if (videoDir == null) {
                videoDir = SDCARD;
            }

            intent.putExtra(CAMERA_PICTURE_DIR_KEY, pictureDir);
            intent.putExtra(CAMERA_VIDEO_DIR_KEY, videoDir);

            return intent;
        }
        return null;
    }


    private ImageButton takeBtn;
    private ImageButton modeSwitchBtn;
    private ImageButton cameraSwitchBtn;
    private CameraSurfaceView surfaceView;
    private TextView timerTv;

    private long  mRecordSeconds;
    private Timer mRecordTimer;
    private TimerTask mRecordTimerTask;

    // region Timer

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
            timerTv.setVisibility(View.GONE);
        }
    }

    // endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);

        modeSwitchBtn = (ImageButton)findViewById(R.id.cameraModeSwitchBtn);
        takeBtn = (ImageButton)findViewById(R.id.takePhotoBtn);
        cameraSwitchBtn = (ImageButton)findViewById(R.id.cameraSwitchBtn);
        timerTv = (TextView)findViewById(R.id.timer_tv);

        String picDir, videoDir;
        if (savedInstanceState != null) {

            picDir = savedInstanceState.getString(CAMERA_PICTURE_DIR_KEY);
            videoDir = savedInstanceState.getString(CAMERA_VIDEO_DIR_KEY);
        } else {
            Intent intent = getIntent();
            picDir = intent.getStringExtra(CAMERA_PICTURE_DIR_KEY);
            videoDir = intent.getStringExtra(CAMERA_VIDEO_DIR_KEY);
        }

        if (picDir == null) {
            picDir = SDCARD;
        }
        if (videoDir == null) {
            videoDir = SDCARD;
        }

        surfaceView = (CameraSurfaceView)findViewById(R.id.cameraSurfaceView);
        surfaceView.setup(picDir, videoDir, new CameraSurfaceView.CameraEventListener() {
            @Override
            public void cameraDidChangedToRecordVideo() {

                modeSwitchBtn.setImageDrawable(getResources().getDrawable(R.drawable.photo_mode));
                takeBtn.setImageDrawable(getResources().getDrawable(R.drawable.start_record));
            }

            @Override
            public void cameraDidChangedToTakePicture() {

                modeSwitchBtn.setImageDrawable(getResources().getDrawable(R.drawable.record_mode));
                takeBtn.setImageDrawable(getResources().getDrawable(R.drawable.take_photo));
            }

            @Override
            public void cameraDidChanged(boolean isFront) {

            }

            @Override
            public void cameraDidStartRecordVideo() {

                startTimer();
                takeBtn.setImageDrawable(getResources().getDrawable(R.drawable.stop_record));
            }

            @Override
            public void cameraDidStopRecordVideo() {

                stopTimer();
                takeBtn.setImageDrawable(getResources().getDrawable(R.drawable.start_record));
            }

            @Override
            public void cameraDidTakePicture() {

            }

            @Override
            public void cameraDidChangedFlashMode(int flashMode) {

            }
        });

        surfaceView.errorHandler = new CameraSurfaceView.CameraErrorHandler() {
            @Override
            public void recordError(MediaRecorder mr) {
                Log.d("Camera Record", "recordError: "+mr);
            }

            @Override
            public void initCameraError(String error) {
                Log.d("Camera init", "initCameraError: "+error);
            }
        };

        modeSwitchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!surfaceView.isRecordVideo()) {

                    surfaceView.switchToRecordVideo();

                } else {

                    surfaceView.switchToTakePhoto();
                }
            }
        });


        takeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (surfaceView.isRecordVideo()) {

                    if (surfaceView.isRecording()) {

                        surfaceView.stopRecordVideo();


                    } else  {
                        surfaceView.recordVideo(new CameraSurfaceView.RecordVideoCallback() {
                            @Override
                            public void recordVideoCompletion(String path) {

                                Log.d(TAG, "recordVideoCompletion: " + path);
                            }
                        });

                    }

                } else {

                    surfaceView.takePhotoToFile(new CameraSurfaceView.CaptureImageCallback() {
                        @Override
                        public void captureImageCompletion( byte[] data, String path) {

                            Log.d(TAG, "captureImageCompletion: " + path);
                        }
                    });
                }

            }
        });


        cameraSwitchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                surfaceView.switchCamera();
            }
        });

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(CAMERA_PICTURE_DIR_KEY, surfaceView.getPictureDir());
        outState.putString(CAMERA_VIDEO_DIR_KEY, surfaceView.getVideoDir());
    }

    @Override
    protected void onStop() {
        super.onStop();

        surfaceView.stopPreview();

    }

    @Override
    protected void onStart() {
        super.onStart();

        surfaceView.startPreview();
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
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        surfaceView.startPreview();
    }
}
