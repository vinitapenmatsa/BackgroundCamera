package com.eyedentify.eyedentify_background_camera.service;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.IntentService;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.Semaphore;

/**
 * Created by vinitapenmatsa on 2/16/18.
 */

public class BackgroundCameraService extends IntentService {

    /**
     * MediaRecorder
     */
    private MediaRecorder mMediaRecorder;

    /**
     * A refernce to the opened {@link android.hardware.camera2.CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };


    String backFacingCamera;
    private String mNextVideoAbsolutePath;
    private Surface mRecorderSurface;

    /**
     * Whether the app is recording video now
     */
    private boolean mIsRecordingVideo;
    private static final int REQUEST_VIDEO_PERMISSIONS = 1;


    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
           try {
               mCameraDevice = camera;
               mCameraOpenCloseLock.release();
               startRecordingVideo();
               Thread.sleep(10000);
               stopRecordingVideo();
           }catch (Exception e){
               e.printStackTrace();
           }

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
           mCameraOpenCloseLock.release();
           camera.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            mCameraOpenCloseLock.release();
            camera.close();

        }
    };

    private static final String TAG = "BACKGROUND_CAMERA_SERVICE";

    public BackgroundCameraService(){
        super("BackgroundCameraService");
    }

    @Override
    protected void onHandleIntent(Intent workIntent){
try {
    if (!mIsRecordingVideo) {
        openCamera();

    }
}catch (Exception e){
    e.printStackTrace();
}


    }

    public void startRecordingVideo(){
       if(null==mCameraDevice)
           return;
       try {
           setUpMediaRecorder();

           List<Surface> surfaces = new ArrayList<>();
           // Set up Surface for the MediaRecorder
           mRecorderSurface = mMediaRecorder.getSurface();
           surfaces.add(mRecorderSurface);
           mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

           mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
               @Override
               public void onConfigured(@NonNull CameraCaptureSession session) {
                   mIsRecordingVideo = true;
                   mMediaRecorder.start();
               }

               @Override
               public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                   Toast.makeText(getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();
               }
           }, new Handler());

       }catch(Exception e){
           e.printStackTrace();
       }


    }

    private void stopRecordingVideo(){
        mIsRecordingVideo = false;
        mMediaRecorder.stop();
        mMediaRecorder.reset();

        mNextVideoAbsolutePath = null;
    }

    public void openCamera(){


        try{

        CameraManager mCameraManager = getApplicationContext().getSystemService(CameraManager.class);
        backFacingCamera = getBackFacingCamera(mCameraManager);
        CameraCharacteristics backCameraCharacteristics = mCameraManager.getCameraCharacteristics(backFacingCamera);
        mMediaRecorder = new MediaRecorder();
        mCameraManager.openCamera(backFacingCamera,mStateCallback,null);

        }catch(SecurityException e){
            e.printStackTrace();
        }
        catch (Exception e){
            e.printStackTrace();
        }


    }

    private void closeCamera(){
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void setUpMediaRecorder() throws IOException{
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        if (mNextVideoAbsolutePath == null || mNextVideoAbsolutePath.isEmpty()) {
            mNextVideoAbsolutePath = getVideoFilePath(getApplicationContext());
        }
        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.prepare();
    }

    private String getVideoFilePath(Context context) {
        return context.getExternalFilesDir(null).getAbsolutePath() + "/"
                + System.currentTimeMillis() + ".mp4";
    }

    public String getBackFacingCamera(CameraManager mCameraManager){

        String[] cameraList;
        String backFacingCameraId;

        try {
            cameraList =  mCameraManager.getCameraIdList();
            for(int i=0; i < cameraList.length; i++){

                CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraList[i]);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK){
                    backFacingCameraId = cameraList[i];
                    return  backFacingCameraId;
                }

            }
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
      return null;
    }



}
