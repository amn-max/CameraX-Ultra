package com.dragon.ultracamera;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ImageCapture imageCapture = null;
    private int REQUEST_CODE_PERMISSION = 101;
    ExecutorService cameraExecutor;
    File outputDirectory;
    private int ASK_MULTIPLE_PERMISSION_REQUEST_CODE = 44;
    private String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA","android.permission.WRITE_EXTERNAL_STORAGE","android.permission.READ_EXTERNAL_STORAGE"};
    public CameraControl cControl;
    public CameraInfo cInfo;
    public ScaleGestureDetector scaleGestureDetector;
    private SeekBar zoomBar;
    private View focusView;
    private Handler handler = new Handler();
    private Runnable newRunnable = new Runnable() {
        @Override
        public void run() {
            focusView.setBackground(ContextCompat.getDrawable(getBaseContext(),R.drawable.ic_focus_green));
        }
    };
    private Runnable newRunnable1 = new Runnable() {
        @Override
        public void run() {
            focusView.setVisibility(View.INVISIBLE);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            getSupportActionBar().hide();
        } catch (NullPointerException e) {

        }
        zoomBar = findViewById(R.id.zoomBar);
        zoomBar.setMax(100);
        zoomBar.setProgress(0);
        focusView = findViewById(R.id.focus);
        //Request Camera permission
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, ASK_MULTIPLE_PERMISSION_REQUEST_CODE);
        }
        findViewById(R.id.camera_capture_button).setOnClickListener(v -> {
            TakePhoto takePhoto = new TakePhoto();
            Runnable takingPhoto = () -> takePhoto.execute();
            handler.postAtFrontOfQueue(takingPhoto);
        });


        outputDirectory = getOutputDirectory();
        cameraExecutor = Executors.newSingleThreadExecutor();

    }
// Enable to use normal take photo instead of asyncTask
//    private void takePhotos() {
//        handler.removeCallbacks(newRunnable);
//        handler.removeCallbacks(newRunnable1);
//        focusView.setVisibility(View.INVISIBLE);
//        File photoFile = new File(outputDirectory, "Image_"+System.currentTimeMillis()+".jpg");
//
//        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
//
//        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(getBaseContext()), new ImageCapture.OnImageSavedCallback() {
//            @Override
//            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
//                Uri.fromFile(photoFile);
//                Toast.makeText(getBaseContext(),"Image Saved"+ photoFile.getAbsolutePath(),Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onError(@NonNull ImageCaptureException exception) {
//                Toast.makeText(getBaseContext(),"Error Saving Image"+photoFile.getAbsolutePath(),Toast.LENGTH_SHORT).show();
//            }
//        });
//    }

    private void pinchToZoom() {
        //Pinch Zoom Camera
        ScaleGestureDetector.SimpleOnScaleGestureListener listener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                LiveData<ZoomState> ZoomRatio = cInfo.getZoomState();
                float currentZoomRatio = 0;
                try {
                    currentZoomRatio = ZoomRatio.getValue().getZoomRatio();
                } catch (NullPointerException e) {

                }
                float linearValue = ZoomRatio.getValue().getLinearZoom();
                float delta = detector.getScaleFactor();
                cControl.setZoomRatio(currentZoomRatio * delta);
                float mat = (linearValue) * (100);
                zoomBar.setProgress((int) mat);
                return true;
            }
        };

        scaleGestureDetector = new ScaleGestureDetector(getBaseContext(), listener);
    }




    private void setUpZoomSlider(){
        zoomBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float mat = (float) (progress) / (100);
                cControl.setLinearZoom(mat);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    private File getOutputDirectory() {
        File directory = new File(Environment.getExternalStorageDirectory()+File.separator+"UltraCamera");
        if(!directory.exists()){
            directory.mkdirs();
        }
        return directory;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }



    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(()->{
            try{
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                //bind Camera Preview to Surface provider ie:viewFinder in my case
                Preview preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                PreviewView viewFinder = findViewById(R.id.viewFinder1);

                Preview.SurfaceProvider surfaceProvider = viewFinder.getSurfaceProvider();
                preview.setSurfaceProvider(surfaceProvider);

                try {
                    cameraProvider.unbindAll();
                    Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                    cControl = camera.getCameraControl();
                    cInfo = camera.getCameraInfo();

                    //AutoFocus CameraX
                    viewFinder.setOnTouchListener((v, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            handler.removeCallbacks(newRunnable);
                            handler.removeCallbacks(newRunnable1);
                            focusView.setVisibility(View.INVISIBLE);
                            return true;
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            MeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(viewFinder.getWidth(), viewFinder.getHeight());
                            MeteringPoint autoFocusPoint = factory.createPoint(event.getX(), event.getY());
                            cControl.startFocusAndMetering(new FocusMeteringAction.Builder(autoFocusPoint, FocusMeteringAction.FLAG_AF).build());
                            focusView.setBackground(ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_focus));
                            focusView.setVisibility(View.VISIBLE);
                            handler.postDelayed(newRunnable, 2000);
                            handler.postDelayed(newRunnable1, 4000);
                            return true;
                        } else {

                            return false;
                        }
                    });


                }catch (Exception e){
                    Toast.makeText(this,"Failed",Toast.LENGTH_SHORT).show();
                }
                pinchToZoom();
                setUpZoomSlider();
            }catch (ExecutionException | InterruptedException e){

            }
        },ContextCompat.getMainExecutor(this));
    }


    private boolean allPermissionsGranted() {
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this,permission)!= PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private class TakePhoto extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {
            File photoFile = new File(outputDirectory, "Image_" + System.currentTimeMillis() + ".jpg");

            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

            imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(getBaseContext()), new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    Uri.fromFile(photoFile);
                    Toast.makeText(getBaseContext(), "Image Saved" + photoFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Toast.makeText(getBaseContext(), "Error Saving Image" + photoFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                }
            });
            return null;
        }
    }

}