package com.demo.aswitch.camerademo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceView;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

@SuppressLint("NewApi")
public class CameraHelper {
    public static final String TAG = "CameraHelper";
    private Context mContext;
    private int mPictureWidth = 640;
    private int mPictureHeight = 480;
    private float mCameraFrameRates = 40.0f;

    private int mMediaBlockNumber = 3;
    private int mMediaBlockSize = 1024 * 512;
    private int voice_rate = 8000;
    boolean inProcessing = false;
    private int VideoType;


    private CameraView mCameraView = null;
    private ExecutorService mExecutorService = Executors.newFixedThreadPool(3);
    private ReentrantLock previewLock = new ReentrantLock();
    byte[] yuvFrame = new byte[getYuvBuffer(640, 480)];
    MediaBlock[] mMediaBlocks = new MediaBlock[mMediaBlockNumber];
    int mediaWriteIndex = 0, mediaReadIndex = 0;
    private static CameraHelper instance = new CameraHelper();
    private boolean isSoft;
    private AvcEncoder avcCodec;
    private int fps = 10;  //帧率
    private int bitrate = 12500; //码率

    private CameraHelper() {
    }

    //计算YUV的buffer的函数，需要根据文档计算，而不是简单“*3/2”
    public int getYuvBuffer(int width, int height) {
        int stride = (int) Math.ceil(width / 16.0) * 16;
        int y_size = stride * height;
        int c_stride = (int) Math.ceil(width / 32.0) * 16;
        int c_size = c_stride * height / 2;
        return y_size + c_size * 2;
    }

    public static CameraHelper getInstance() {
        return instance;
    }

    public void initCamera(Context context, SurfaceView captureView, int VideoType) {
        mContext = context;
        this.VideoType = VideoType;
        ;
        resetMediaBuffer();
        // 初始化配置信息
        mCameraView = new CameraView(captureView, VideoType);
        mCameraView.setCameraReadyCallback(new CameraView.CameraReadyCallback() {
            @Override
            public void onCameraReady(boolean isCameraOpen) {

                mCameraView.stopPreview();
                mCameraView.setupCamera(mPictureWidth, mPictureHeight, 4, mCameraFrameRates, previewCb);
                //初始化硬编码器
                avcCodec = new AvcEncoder(mPictureWidth, mPictureHeight, fps, bitrate);
                avcCodec.StartEncoderThread();
                mCameraView.startPreview();
            }
        });
    }


    public void switchCamera() {
        mCameraView.switchCamera();
    }

    private static int yuvqueuesize = 10;
    public static ArrayBlockingQueue<byte[]> YUVQueue = new ArrayBlockingQueue<byte[]>(yuvqueuesize);
    /**
     * 采集到的视频帧
     */
    public PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] frame, Camera c) {
            previewLock.lock();
            Log.d(TAG, "onPreviewFrame: " + frame.toString());

            putYUVData(frame, frame.length);//硬编码
            c.addCallbackBuffer(frame);//这里要添加一次缓冲，否则onPreviewFrame可能不会再被回调
            previewLock.unlock();
        }
    };


    public void putYUVData(byte[] buffer, int length) {
        if (YUVQueue.size() >= 10) {
            YUVQueue.poll();
        }
        YUVQueue.add(buffer);
    }


    public void resetMediaBuffer() {
        for (int i = 0; i < mMediaBlockNumber; i++) {
            mMediaBlocks[i] = new MediaBlock(mMediaBlockSize);
        }
        synchronized (CameraHelper.this) {
            for (int i = 0; i < mMediaBlockNumber; i++) {
                mMediaBlocks[i].reset();
            }
            mediaReadIndex = 0;
            mediaWriteIndex = 0;
        }
    }

    public void releaseAV() {
        if (mCameraView != null) {
            previewLock.lock();
            mCameraView.stopPreview();
            mCameraView.releaseCamera();
            previewLock.unlock();
            mCameraView = null;
        }
    }

}
