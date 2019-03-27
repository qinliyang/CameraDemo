package com.demo.aswitch.camerademo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.view.SurfaceView;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

@SuppressLint("NewApi")
public class CameraHelper {

    private Context mContext;
    private int mPictureWidth = 640;
    private int mPictureHeight = 480;
    private float mCameraFrameRates = 40.0f;

    private int mMediaBlockNumber = 3;
    private int mMediaBlockSize = 1024 * 512;
    private int voice_rate = 8000;
    boolean inProcessing = false;

    public CameraView getmCameraView() {
        return mCameraView;
    }

    private CameraView mCameraView = null;
    private AudioRecord mAudioRecord = null;
    private ExecutorService mExecutorService = Executors.newFixedThreadPool(3);
    private VideoEncodingTask mVideoEncodingTask = new VideoEncodingTask();
    private VideoHardEncodingTask mVideoHardEncodingTask = new VideoHardEncodingTask();
    private ReentrantLock previewLock = new ReentrantLock();
    ///////////
    //byte[] yuvFrame = new byte[1920 * 1280 * 3 / 2];
    byte[] yuvFrame = new byte[getYuvBuffer(640, 480)];
    MediaBlock[] mMediaBlocks = new MediaBlock[mMediaBlockNumber];
    int mediaWriteIndex = 0, mediaReadIndex = 0;

    private static CameraHelper instance = new CameraHelper();
    private boolean isSoft;
    private AvcEncoder2 avcCodec2;

    private CameraHelper() {
    }

    //计算YUV的buffer的函数，需要根据文档计算，而不是简单“*3/2”
    public int getYuvBuffer(int width, int height) {
        // stride = ALIGN(width, 16)
        int stride = (int) Math.ceil(width / 16.0) * 16;
        // y_size = stride * height
        int y_size = stride * height;
        // c_stride = ALIGN(stride/2, 16)
        int c_stride = (int) Math.ceil(width / 32.0) * 16;
        // c_size = c_stride * height/2
        int c_size = c_stride * height / 2;
        // size = y_size + c_size * 2
        return y_size + c_size * 2;
    }

    public static CameraHelper getInstance() {
        return instance;
    }

    public void initCamera(Context context, SurfaceView captureView,int VideoType) {
        mContext = context;
        this.VideoType = VideoType;
        //初始化摄像头推流参数
//        initPushConfig();
        resetMediaBuffer();
        if (isMuxAudio) {
            initAudio();
        }
        // 初始化配置信息
        mCameraView = new CameraView(captureView, VideoType);
        mCameraView.setCameraReadyCallback(new CameraView.CameraReadyCallback() {
            @Override
            public void onCameraReady(boolean isCameraOpen) {

                mCameraView.stopPreview();
                mCameraView.setupCamera(mPictureWidth, mPictureHeight, 4, mCameraFrameRates, previewCb);
                // 初始化音视频参数
                initCameraPushParamSo();
                // TCP方式推送过来的码流数据
                if (mAudioRecord != null) {
                    mAudioRecord.startRecording();
                    AudioEncoder audioEncoder = new AudioEncoder();
                    audioEncoder.start();
                }

                if (EncodeType == 1) {
                    //初始化硬编码器
//                    avcCodec2 = new AvcEncoder2(mPictureWidth, mPictureHeight, fps, bitrate);
                    avcCodec = new AvcEncoder(mPictureWidth, mPictureHeight, fps, bitrate);
                    avcCodec.StartEncoderThread();
                }
                mCameraView.startPreview();
            }
        });
    }

    //so 对接参数 Video
    private int VideoType;      //视频源类型
    private int EncodeType;
    private boolean isMuxVideo;  //是否复用视频
    private int mVideoResolution;  //分辨率
    private int fps;  //帧率
    private int bitrate; //码率 ;
    private int mScreenOrientation; // 0为横屏
    private boolean isEncode; // 是否编码
    // Audio
    private boolean isMuxAudio; // 是否复用音频
    private int mAudioChannels; // 音频声道数
    private int mAudioSampleSize; // 采样位宽
    private int mAudioBitrate; // 音频编码率
    private int mAudioSampleRate; // 音频采样率


    private void initPushConfig() {
        //初始化视频推流
        if (EncodeType == 0) {
            isEncode = true;
        }
        if (EncodeType == 1) {
            isEncode = false;
        }
        if (EncodeType == 2) {
            //自动选择使用软编码  因为部分机型可能不能使用硬编码造成崩溃
            isEncode = true;
        }

        if (VideoType == 0) {
            // 说明是视频源是录屏
            // 录屏不用编码
            isEncode = false;
        }
    }

    private void initCameraPushParamSo() {

        int is_Mux_Video;
        int is_Encode;
        if (isMuxVideo) {
            is_Mux_Video = 1;
        } else {
            is_Mux_Video = 0;
        }
        if (isEncode) {
            is_Encode = 1;
        } else {
            is_Encode = 0;
        }
        int is_Mux_Audio;
        if (isMuxAudio) {
            is_Mux_Audio = 1;
        } else {
            is_Mux_Audio = 0;
        }
        int channels_num = mAudioChannels;
        int sample_size = mAudioSampleSize;
        int sample_rate = mAudioSampleRate;
        //拿到port
//        String transPort = mlivePusher.transPort;
        int video_width = 640;
        int video_height = 480;
        //拿到port
//        LogUtils.d("ScreenRecorder ===>getTransPort " + transPort);  //TODO

        LiveNative.InitVideoParam(is_Mux_Video, VideoType, is_Encode, video_width, video_height, fps, bitrate);
//        LogUtils.d("LiveNative ====>InitVideoParam " + VideoType + "is_Mux_Video =" + is_Mux_Video + "is_Encode = " + is_Encode);
//        LogUtils.d("AudioRecorder ====>LiveNative==>InitAudioParam ");

        LiveNative.InitAudioParam(is_Mux_Audio, channels_num, sample_size, sample_rate);

        //让代理传送数据
//        LiveNative.startTransfer("127.0.0.1", Integer.parseInt(mlivePusher.transPort));
    }

    /**
     * 初始化采集音频
     */
    public void initAudio() {
        int minBufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        int targetSize = 16000 * 2;
        if (targetSize < minBufferSize) {
            targetSize = minBufferSize;
        }
        if (mAudioRecord == null) {
            try {
                mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, targetSize);
            } catch (IllegalArgumentException e) {
                mAudioRecord = null;
                e.printStackTrace();
            }
        }
    }

    public void switchCamera() {
        mCameraView.switchCamera();
    }

    private static int yuvqueuesize = 10;

    public static ArrayBlockingQueue<byte[]> YUVQueue = new ArrayBlockingQueue<byte[]>(yuvqueuesize);

    private AvcEncoder avcCodec;


    private byte[] h264;

    public PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] frame, Camera c) {
            previewLock.lock();
//            LogUtils.d("PreviewCallback  frame " + frame.length);
             /*
             判断执行软硬编码
             */
            if (EncodeType == 1) {
//                LogUtils.d("doVideoEncode----->执行硬编码");
//                doHardVideoEncode(frame);
                putYUVData(frame, frame.length);
            } else {
//                LogUtils.d("doVideoEncode----->执行软编码");

                doVideoEncode(frame);
            }

            c.addCallbackBuffer(frame);
            previewLock.unlock();
        }
    };


    public void putYUVData(byte[] buffer, int length) {
        if (YUVQueue.size() >= 10) {
            YUVQueue.poll();
        }
        YUVQueue.add(buffer);
    }

    // 视频编码
    public void doVideoEncode(byte[] frame) {
        if (mCameraView == null) {
            return;
        }
        if (inProcessing == true) {
            return;
        }
        inProcessing = true;
//		Util.save(frame, 0, frame.length, path, true);
//		int picWidth = mCameraView.Width();
//		int picHeight = mCameraView.Height();
        int picWidth = 640;
        int picHeight = 480;
        int size = picHeight * picWidth + picWidth * picHeight / 2;


        System.arraycopy(frame, 0, yuvFrame, 0, size);
        mExecutorService.execute(mVideoEncodingTask);
    }

    // 视频编码
    public void doHardVideoEncode(byte[] frame) {
        if (mCameraView == null) {
            return;
        }
        if (inProcessing == true) {
            return;
        }
        inProcessing = true;
//		Util.save(frame, 0, frame.length, path, true);
//		int picWidth = mCameraView.Width();
//		int picHeight = mCameraView.Height();
        int picWidth = 640;
        int picHeight = 480;
        int size = picHeight * picWidth + picWidth * picHeight / 2;


        System.arraycopy(frame, 0, yuvFrame, 0, size);
        mExecutorService.execute(mVideoHardEncodingTask);
    }

    public class VideoEncodingTask implements Runnable {
        private byte[] videoHeader = new byte[8];

        public VideoEncodingTask() {
            videoHeader[0] = (byte) 0x19;
            videoHeader[1] = (byte) 0x79;
        }

        @Override
        public void run() {
            MediaBlock currentBlock = mMediaBlocks[mediaWriteIndex];
            if (currentBlock.flag == 1) {
                inProcessing = false;
                return;
            }
            int retValue = LiveNative.PushYUVData(yuvFrame, yuvFrame.length);
            //保存到本地
//            LogUtils.d("doVideoEncode----->LiveNative.PushYUVData");
            if (retValue < 0) {
                return;
            }
            inProcessing = false;
        }
    }

    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

    public class VideoHardEncodingTask implements Runnable {

        public VideoHardEncodingTask() {
        }

        @Override
        public void run() {
            MediaBlock currentBlock = mMediaBlocks[mediaWriteIndex];
            if (currentBlock.flag == 1) {
                inProcessing = false;
                return;
            }
            int i = avcCodec2.offerEncoder(yuvFrame, h264);
            if (i > 0) {
//                LogUtils.d("doVideoHardEncode----->LiveNative.PushEncodeData");
                LiveNative.PushEncodeData(h264, h264.length, bufferInfo.presentationTimeUs);
            }

            inProcessing = false;
        }
    }

    public class AudioEncoder extends Thread {
        private byte[] audioPCM = new byte[1024 * 32];
        private byte[] audioHeader = new byte[8];

        // old
        // private int packageSize = 320;
        // private int packageSize = 16000;
        private int packageSize = 2048;

        public AudioEncoder() {
            audioHeader[0] = (byte) 0x19;
            audioHeader[1] = (byte) 0x82;
        }

        @Override
        public void run() {
            while (true) {
                if (mAudioRecord == null) {
                    break;
                }
                int retValue = mAudioRecord.read(audioPCM, 0, packageSize);
                if (retValue == AudioRecord.ERROR_INVALID_OPERATION || retValue == AudioRecord.ERROR_BAD_VALUE) {
                    break;
                }

                if (retValue <= 0) {
                    continue;
                }
//				Lg.d("PushPCMData start currentime = " +System.currentTimeMillis());
//				retValue = PushPCMData(audioPCM, retValue);

                retValue = LiveNative.PushPCMData(audioPCM, retValue, null, 0);

                if (retValue < 0) {
                    break;
                }

                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
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

/*	static {
        System.loadLibrary("ffmpeg"); //软编码库
//		System.loadLibrary("da");	 //编码库应用层
	}
	
	public native int InitAVPara(int audio_sample_rate, int video_width, int video_height, int fps);

	public native int DataTCPTransfer(String server, int port);

	public native int PushYUVData(byte[] video_data_stream);

	public native int PushPCMData(byte[] audio_data_stream, int audio_data_size);*/

    public void releaseAV() {
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            // 彻底释放资源
            mAudioRecord.release();
            mAudioRecord = null;
        }

        if (mCameraView != null) {
            previewLock.lock();
            mCameraView.stopPreview();
            mCameraView.releaseCamera();
            previewLock.unlock();
            mCameraView = null;
        }
    }

}
