package com.example.macmini1.takephoto.Crop;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.example.macmini1.takephoto.R;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class CropImageActivity extends Activity {


    private static String OUTPUT_X = "outputX";
    private static String OUTPUT_Y = "outputY";
    private static String OUTPOT_FILE = "outputFile";
    private static String DATA_FILE = "dataFile";

    private CropImageView mView;
    private Button cropBtn;
    private Button cancelBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image);

        Intent intent = getIntent();
        final int outputX = intent.getIntExtra(OUTPUT_X,500);
        final int outputY = intent.getIntExtra(OUTPUT_Y,500);
        final File src = (File) intent.getExtras().get(DATA_FILE);
        final File des = (File) intent.getExtras().get(OUTPOT_FILE);
        if (src == null) {
            setResult(RESULT_CANCELED);
            finish();
        }

        mView = (CropImageView) findViewById(R.id.cropimage);

        if (src.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(src.getAbsolutePath());
            //设置资源和默认长宽
//            mView.setDrawable(new BitmapDrawable(bitmap), outputX, outputY);

            mView.setDrawableFile(src, outputX, outputY);

        } else {
            setResult(RESULT_CANCELED);
            finish();
        }

        cropBtn = (Button) findViewById(R.id.crop_btn);
        cropBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //调用该方法得到剪裁好的图片
                Bitmap mBitmap = mView.getCropImage();
                if (mBitmap != null) {
                    savePhotoToFile(mBitmap,des);
                }
            }
        });

        cancelBtn = (Button) findViewById(R.id.cancel_btn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    private void savePhotoToFile(Bitmap bitmap, File photoFile) {
        if (photoFile == null || bitmap == null) {
            return;
        }
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(photoFile));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();

            setResult(RESULT_OK);

        } catch (IOException e) {
            e.printStackTrace();

            setResult(RESULT_CANCELED);

        } finally {
            finish();
        }
    }

    public static class Builder {
        private int width;
        private int height;
        private File input;
        private File output;

        public Builder(final int outputX, final int outputY, final File src, final File des) {
            super();
            width = outputX;
            height = outputY;
            input = src;
            output = des;
        }

        public Intent getIntent(final Context ctx) {
            final Intent intent = new Intent(ctx,CropImageActivity.class);

            intent.putExtra(OUTPUT_X,width);
            intent.putExtra(OUTPUT_Y,height);
            intent.putExtra(DATA_FILE,input);
            intent.putExtra(OUTPOT_FILE,output);

            return intent;
        }

    }
}
