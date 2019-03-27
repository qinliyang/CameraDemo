package com.demo.aswitch.camerademo;
/**
 * 编码传输JNI调用类
 * @author ly
 *
 */
public class LiveNative {
	static {
		System.loadLibrary("yuv"); //软编码库
		System.loadLibrary("ffmpeg"); //软编码库
		System.loadLibrary("da");	 //编码库应用层
	}
	public static native int InitVideoParam(int isMuxVideo, int VideoType, int isEncode,int video_width,int video_height,int fps,int bitrate);
	//PushYUVData   PushVideoData
	public static native int PushYUVData(byte[] video_data, int video_data_size);

	public static native int PushEncodeData(byte[] video_data, int video_data_size,long video_timestamp);
	
	public static native int InitAudioParam(int isMuxAudio,int channels_num,int sample_size,int sample_rate);
	//PushPCMData  PushAudioData
	public static native int PushPCMData(byte[] audio1_data, int audio1_len,byte[] audio2_data, int audio2_len);

	public static native int startTransfer(String server, int port);
	
	public static native int stopTransfer(int agent_port,String token,String packname,int cid,int worktype);

	public static native int waitNextTimestmp(int next_ms);

	public static native long getCurrentSystemtime();

	public static native byte[] ConvertABGRPToYUV(byte[] bufferData, byte[] outbufferData,int length, int width, int height, int pixelStride,int rowPadding) ;

	public static native int setAudioToogle(int state);

}
