package com.example.macmini1.takephoto;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;

public class PhotoEditActivity extends AppCompatActivity {

    private ImageView iv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_edit);

        iv = (ImageView)findViewById(R.id.photoView);

        Intent intent = getIntent();

        final File src = (File) intent.getExtras().get("file");
        Bitmap bitmap = BitmapFactory.decodeFile(src.getAbsolutePath());

//        final float rotate = intent.getFloatExtra("rotate",90);
//
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Matrix matrix = new Matrix();
//                matrix.postRotate(rotate);
//                final Bitmap img = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        iv.setImageBitmap(bitmap);
//                    }
//                });
//            }
//        }).start();


        iv.setImageBitmap(bitmap);

        Log.d("Photo", "edit activity load file end");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


    }
}
