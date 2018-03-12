package com.eyedentify.eyedentify_background_camera;

import android.Manifest;
import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;

import com.eyedentify.eyedentify_background_camera.service.BackgroundCameraService;
import com.eyedentify.eyedentify_background_camera.service.BackgroundVideoRecorder;
import com.eyedentify.eyedentify_background_camera.service.CameraBoundService;

public class BackgroundCameraActivity extends AppCompatActivity {

    Button startCameraButton;
    Button stopCameraButton;

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };

 //   private Boolean isServiceBound = false;
 //   private CameraBoundService cameraBoundService;

   /* private ServiceConnection myCameraService = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            CameraBoundService.CameraServiceBinder cameraServiceBinder =
                    (CameraBoundService.CameraServiceBinder) iBinder;
            cameraBoundService = cameraServiceBinder.getServiceInstance();

            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };
*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background_camera_acticity);


        //final Intent cameraServiceIntent = new Intent(this, CameraBoundService.class);
       // bindService(cameraServiceIntent, myCameraService, Context.BIND_AUTO_CREATE);

        final Intent cameraServiceIntent = new Intent(this, BackgroundVideoRecorder.class);

        startCameraButton = (Button) findViewById(R.id.startCameraButton);
        stopCameraButton = (Button) findViewById(R.id.stopCameraButton);

        startCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //cameraBoundService.startCameraService();
                startService(cameraServiceIntent);

            }
        });

        stopCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                BackgroundVideoRecorder.shouldContinue = false;

            }
        });

    }

}
