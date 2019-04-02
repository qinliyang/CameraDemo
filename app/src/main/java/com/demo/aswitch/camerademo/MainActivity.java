package com.demo.aswitch.camerademo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView tvF;
    private TextView tvB;
    private SurfaceView sfv;
    private static final int REQUEST_CAMERA_PERMISSIONS = 931;
    private TextView tvA;
    private TextView tvC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        if (Build.VERSION.SDK_INT > 15) {
            final String[] permissions = {
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE};

            final List<String> permissionsToRequest = new ArrayList<>();
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[permissionsToRequest.size()]), REQUEST_CAMERA_PERMISSIONS);
            } else {

                if (checkCameraHardware(this)) {
                    CameraHelper.getInstance().initCamera(this, sfv, 0);
                } else {
                    Toast.makeText(this, "没有摄像机硬件", Toast.LENGTH_SHORT);
                }

            }
        } else {
            CameraHelper.getInstance().initCamera(this, sfv, 0);
        }

    }

    int type = 0;

    private void initView() {
        tvA = (TextView) findViewById(R.id.tvA);
        tvB = (TextView) findViewById(R.id.tvB);
        tvC = (TextView) findViewById(R.id.tvC);
        sfv = (SurfaceView) findViewById(R.id.sfv);

        tvA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, InfoActivity.class);
                startActivity(intent);
            }
        });
        tvB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CameraHelper.getInstance().switchCamera();
            }
        });

        tvC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                type = 2 - type;
                CameraHelper.getInstance().setOnOrOff(type);
                tvC.setText(type == 2 ? "闪光灯已打开" : "闪光灯已关闭");
            }
        });

    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }
}
