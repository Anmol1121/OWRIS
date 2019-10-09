package com.owris.textrecognition;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.view.SurfaceView;
import android.widget.TextView;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    Button mCapture;
    SurfaceView mCameraView;
    TextView mTextView;
    CameraSource mCameraSource;
    private static final String TAG = "MainActivity";
    private static final int requestPermissionID = 101;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraView = findViewById(R.id.surfaceView);
        mTextView = findViewById(R.id.text_view);
        mTextView.setMovementMethod(new ScrollingMovementMethod());
        mCapture = (Button) findViewById(R.id.capture);
        startCameraSource();

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != requestPermissionID) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                mCameraSource.start(mCameraView.getHolder());


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private void startCameraSource() {

        final TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Detector dependencies not loaded yet");
        } else {

            mCameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(2.0f)
                    .build();

            mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    try {

                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    requestPermissionID);
                            return;
                        }
                        mCameraSource.start(mCameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mCameraSource.stop();
                }
            });

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {
                }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();

                    if (items.size() != 0) {
                        Thread updateThread = new Thread() {

                            @Override
                            public void run() {
                                try {
                                    while (!isInterrupted()) {
                                        Thread.sleep(1000);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                updateTextView(items);
                                            }
                                        });
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        };


                        updateThread.start();

                    }
                }
            });

        }
    }

    private void updateTextView(SparseArray<TextBlock> itms) {
        final StringBuilder stringBuilder = new StringBuilder();


        for (int i = 0; i < itms.size(); i++) {

            TextBlock item = itms.valueAt(i);
            stringBuilder.append(item.getValue());
            stringBuilder.append("\n");

        }
        mTextView.setText(stringBuilder.toString());
        mCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {

                    File root = new File(Environment.getExternalStorageDirectory() +  java.io.File.separator + "OWRIS");
                    if (!root.exists()) {
                        Toast.makeText(getApplicationContext(),
                                (root.mkdirs() ? "Directory has been created" : "Directory not created"),
                                Toast.LENGTH_SHORT).show();
                        Log.i("working","not working");

                    }
                    else{
                        Toast.makeText(getApplicationContext(), "Directory exists", Toast.LENGTH_SHORT).show();
                        root.mkdirs();
                        Log.i("","");
                    }
                    String sFile=UUID.randomUUID().toString();
                    Context context=getApplicationContext();
                    String sBody=stringBuilder.toString();
                    File gpxfile = new File(root, sFile);
                    FileWriter writer = new FileWriter(gpxfile);
                    writer.append(sBody);
                    writer.flush();
                    writer.close();
                    Toast.makeText(context, "Saved", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
        });
    }
}



