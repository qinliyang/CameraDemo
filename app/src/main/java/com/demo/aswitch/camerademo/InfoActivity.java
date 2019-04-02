package com.demo.aswitch.camerademo;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.List;

public class InfoActivity extends AppCompatActivity {


    private TextView tvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        initView();
        int i = CameraUtil.HasBackCamera();
        int i1 = CameraUtil.HasFrontCamera();
        String cameraPixels = CameraUtil.getCameraPixels(0);
        String cameraPixels1 = CameraUtil.getCameraPixels(1);

        List cameraSize = CameraUtil.getCameraSize(0);
        List cameraSize2 = CameraUtil.getCameraSize(1);
        StringBuilder stringBuilder = new StringBuilder();

        for (int j = 0; j < cameraSize.size(); j++) {
            Camera.Size size = (Camera.Size) cameraSize.get(j);
            int sizehieght = size.height;
            int sizewidth = size.width;
           stringBuilder
                    .append("\n后置摄像头支持的:")
                    .append("宽")
                    .append(sizewidth)
                    .append("高")
                    .append(sizehieght).toString();

        }

        stringBuilder.append("\n----------------------");
        for (int j = 0; j < cameraSize2.size(); j++) {
            Camera.Size size = (Camera.Size) cameraSize2.get(j);
            int sizehieght = size.height;
            int sizewidth = size.width;

           stringBuilder
                    .append("\n前置摄像头支持的:")
                    .append("宽")
                    .append(sizewidth)
                    .append("高")
                    .append(sizehieght).toString();

        }

        tvInfo.setText("后置摄像头:" + i + "\n前置摄像头:" + i1 +
                "\n后置摄像头像素:" + cameraPixels + "\n前置摄像头像素:" + cameraPixels1 + stringBuilder.toString());
    }

    private void initView() {
        tvInfo = (TextView) findViewById(R.id.tvInfo);
    }
}
