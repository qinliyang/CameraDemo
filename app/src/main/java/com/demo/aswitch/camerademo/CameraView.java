package com.demo.aswitch.camerademo;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class CameraView implements SurfaceHolder.Callback {

    public Camera mCamera = null;
    private List<Camera.Size> mSupportedSizes;


    public SurfaceView mSurfaceView = null;
    public SurfaceHolder mSurfaceHolder = null;
    private Camera.Size procSize_;
    private CameraReadyCallback mCameraReadyCallback = null;
    private boolean isCameraOpen;
    public Camera.Parameters parameters;
    private int cameraId = 0;


    public CameraView(SurfaceView surfaceView, int VideoType) {
        mSurfaceView = surfaceView;
        mSurfaceHolder = mSurfaceView.getHolder();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);
        if (VideoType == 1) {
            cameraId = 1;
        } else if (VideoType == 2) {
            cameraId = 0;
        }
    }


    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void initCamera() {
        //NOTE(ly):摄像头
        try {
            //cameraId 0，代表后置摄像头 1 代表前置摄像头
            mCamera = Camera.open(cameraId);
            isCameraOpen = true;

        } catch (Exception e1) {
            e1.printStackTrace();
            isCameraOpen = false;

        }
        parameters = mCamera.getParameters();

        //设置预览方向
        mCamera.setDisplayOrientation(90);

        // 加了水印的预览窗口也需要改变大小
        if (mSurfaceView != null) {
            mSurfaceView.getHolder().setFixedSize(640, 480);
        }

        mSupportedSizes = parameters.getSupportedPreviewSizes();
        //预览大小
        parameters.setPreviewSize(640, 480);
        //数据格式  NV21数据的所需空间大小(字节)＝宽 x 高 x 3 / 2 (y=WxH,u=WxH/4,v=WxH/4)
        parameters.setPreviewFormat(ImageFormat.NV21);
        setOnOrOff(parameters,0);
        mCamera.setParameters(parameters);
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupCamera(640, 480, 4, 40.0, CameraHelper.getInstance().previewCb);

        mCamera.startPreview();

    }

    public void setOnOrOff(Camera.Parameters parameters, int type) {
        if (type == 0)
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
        if (type == 1)
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        if (type == 2)
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

        mCamera.setParameters(parameters);
    }

    /*
    切换摄像头
     */
    public void switchCamera() {
        CameraInfo cameraInfo = new CameraInfo();
        Log.d("Camera", "number:" + Camera.getNumberOfCameras());
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {

            } else if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {

            }
        }
        cameraId = 1 - cameraId;
        releaseCamera();
        initCamera();
    }

    public List<Camera.Size> getSupportedPreviewSize() {
        return mSupportedSizes;
    }

    public int Width() {
        return procSize_.width;
    }

    public int Height() {
        return procSize_.height;
    }

    public void setCameraReadyCallback(CameraReadyCallback cb) {
        mCameraReadyCallback = cb;
    }

    public void startPreview() {
        if (mCamera == null)
            return;
        mCamera.startPreview();
    }

    public void stopPreview() {
        if (mCamera == null)
            return;
        mCamera.stopPreview();
    }

    public void releaseCamera() {
        if (mCamera == null) {
            return;
        }
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @SuppressLint("WrongConstant")
    public void setupCamera(int wid, int heigh, int bufNymber, double fps, PreviewCallback callback) {

        procSize_ = mCamera.new Size(0, 0);
        double diff = Math.abs(mSupportedSizes.get(0).width * mSupportedSizes.get(0).height - wid * heigh);
        int targetIndex = 0;
        for (int i = 0; i < mSupportedSizes.size(); i++) {
            double newDiff = Math.abs(mSupportedSizes.get(i).width * mSupportedSizes.get(i).height - wid * heigh);
            if (newDiff < diff) {
                diff = newDiff;
                targetIndex = i;
            }
        }

        procSize_.width = mSupportedSizes.get(targetIndex).width;
        procSize_.height = mSupportedSizes.get(targetIndex).height;

        // 选择合适的预览尺寸
        List<Camera.Size> sizeList_pic = parameters.getSupportedPictureSizes();
        int PictureWidth = 0;
        int PictureHeight = 0;
        if (sizeList_pic.size() > 1) {
            Iterator<Camera.Size> itor = sizeList_pic.iterator();
            while (itor.hasNext()) {
                Camera.Size cur = itor.next();

            }
        }

        // 如果sizeList只有一个我们也没有必要做什么了，因为就他一个别无选择
        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
        int PreviewWidth = 0;
        int PreviewHeight = 0;
        if (sizeList.size() > 1) {
            Iterator<Camera.Size> itor = sizeList.iterator();
            while (itor.hasNext()) {
                Camera.Size cur = itor.next();

                if (cur.width >= PreviewWidth
                        && cur.height >= PreviewHeight) {
                    PreviewWidth = cur.width;
                    PreviewHeight = cur.height;
                    break;
                }
            }
        }
        //double diff = Math.abs(mSupportedSizes.get(0).width * mSupportedSizes.get(0).height -  wid * heigh);
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(procSize_.width, procSize_.height);
        parameters.setPreviewFormat(ImageFormat.NV21);

    	/*
        PictureWidth = 1920;
	     PictureHeight = 1080;
	    parameters.setPreviewSize(PreviewWidth, PreviewHeight); //获得摄像区域的大小 
	    parameters.setPictureSize(PictureWidth, PictureHeight);
	    parameters.setPreviewFormat(ImageFormat.NV21);
	    */

        //设置预览方向
        //mCamera.setDisplayOrientation(90);
        mCamera.setParameters(parameters);
        PixelFormat pixelFormat = new PixelFormat();
        /**
         * 像素格式
         */
        PixelFormat.getPixelFormatInfo(ImageFormat.NV21, pixelFormat);
        int bufSize = procSize_.width * procSize_.height * pixelFormat.bitsPerPixel / 8;

        byte[] buffer = null;
        for (int i = 0; i < bufNymber; i++) {
            buffer = new byte[bufSize];
            mCamera.addCallbackBuffer(buffer);
        }
        mCamera.setPreviewCallbackWithBuffer(callback);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        initCamera();

        if (mCameraReadyCallback != null)
            mCameraReadyCallback.onCameraReady(isCameraOpen);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    /**
     * 停止预览
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    public interface CameraReadyCallback {
        void onCameraReady(boolean isCameraOpen);
    }
}
