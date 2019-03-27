package com.demo.aswitch.camerademo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView tvF;
    private TextView tvB;
    private SurfaceView sfv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        CameraHelper.getInstance().initCamera(this, sfv,0);
        CameraHelper.getInstance().previewCb
    }

    private void initView() {
        tvF = (TextView) findViewById(R.id.tvF);
        tvB = (TextView) findViewById(R.id.tvB);
        sfv = (SurfaceView) findViewById(R.id.sfv);

    }
}
