package com.eyedentify.eyedentify_background_camera.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.eyedentify.eyedentify_background_camera.R;

import java.io.File;

/**
 * Created by vinitapenmatsa on 3/10/18.
 */

public class BackgroundVideoRecorder extends IntentService implements SurfaceHolder.Callback {

    public static volatile boolean shouldContinue = true;

    private WindowManager windowManager;
    private SurfaceView surfaceView;
    private Camera camera = null;
    private MediaRecorder mediaRecorder = null;
    private Camera.Size pictureSize;

    boolean isFrontFacing = false;

    public  BackgroundVideoRecorder(){
         super("Background Video Recorder");
    }

    @Override
    public void onHandleIntent(Intent intent){

        if(!shouldContinue){
            stopRecording();
        }
    }

    @Override
    public void onCreate(){

        super.onCreate();

        // Start foreground service to avoid unexpected kill
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN){
            Notification notification = new Notification.Builder(this)
                    .setContentTitle("Recording Video")
                    .setContentText("recording...")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .build();

            startForeground(1234,notification);

        }

        // Create new SurfaceView, set its size to 1x1, move it to the top left corner and set this service as a callback
        windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        surfaceView = new SurfaceView(this);

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(1,1, Build.VERSION.SDK_INT < Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY :
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        layoutParams.gravity = Gravity.LEFT|Gravity.TOP;
        windowManager.addView(surfaceView,layoutParams);
        surfaceView.getHolder().addCallback(this);


    }

    private Camera openFrontFacingCameraGingerbread(){
        if(camera!=null){
            camera.stopPreview();
            camera.release();
        }
        Camera cam = null;

        if(isFrontFacing && checkFrontCamera(BackgroundVideoRecorder.this)){
            int cameraCount = 0;
            cam = null;
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            cameraCount = Camera.getNumberOfCameras();

            for (int camIdx = 0; camIdx < cameraCount; camIdx++)
            {
                Camera.getCameraInfo(camIdx, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                {
                    try
                    {
                        cam = Camera.open(camIdx);
                    }
                    catch (RuntimeException e)
                    {
                        Log.e("Camera",
                                "Camera failed to open: " + e.getLocalizedMessage());

                    }
                }
            }
        }
        else
        {
            cam = Camera.open();
        }
        return cam;

    }


    private Camera.Size getBiggesttPictureSize(Camera.Parameters parameters)
    {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedVideoSizes())
        {
            if (result == null)
            {
                result = size;
            }
            else
            {
                int resultArea = result.width * result.height;
                int newArea    = size.width * size.height;

                if (newArea > resultArea)
                {
                    result = size;
                }
            }
        }

        return (result);
    }

    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        camera = openFrontFacingCameraGingerbread();

        mediaRecorder = new MediaRecorder();
        camera.unlock();


        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile.get
                (CamcorderProfile.QUALITY_LOW));

        File imagesFolder = new File(
                Environment.getExternalStorageDirectory(), "foldername");
        if (!imagesFolder.exists())
            imagesFolder.mkdirs(); // <----


        File image = new File(imagesFolder, System.currentTimeMillis()
                + ".mp4");  //file name + extension is .mp4


        mediaRecorder.setOutputFile(image.getAbsolutePath());

        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
        }
        try {
            mediaRecorder.start();
        } catch (Exception e) {

        }
    }


    // Stop recording and remove SurfaceView

    public void stopRecording()
    {
        mediaRecorder.stop();
        mediaRecorder.reset();
        mediaRecorder.release();

        camera.lock();
        camera.release();

        windowManager.removeView(surfaceView);
    }

   @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
       // Bundle extras = intent.getExtras();

//you can pass using intent,that which camera you want to use front/rear
        isFrontFacing = false;//extras.getBoolean("Front_Request");

        return super.onStartCommand(intent, flags, startId);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height)
    {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder)
    {
    }





    private boolean checkFrontCamera(Context context){
        if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)){
            return true;
        }else{
            return false;
        }
    }
}
