package com.dragon.ultracamera;
import android.annotation.SuppressLint;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.ScaleGestureDetector;

import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;


import com.dragon.ultracamera.adapters.ViewPagerAdapter;
import com.google.common.util.concurrent.ListenableFuture;
import com.squareup.picasso.Picasso;

import java.io.File;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class cameraActivity extends AppCompatActivity {

    private ImageCapture imageCapture = null;
    private int REQUEST_CODE_PERMISSION = 101;
    ExecutorService cameraExecutor;
    File outputDirectory;
    private int ASK_MULTIPLE_PERMISSION_REQUEST_CODE = 44;
    private String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA","android.permission.WRITE_EXTERNAL_STORAGE","android.permission.READ_EXTERNAL_STORAGE"};
    public CameraControl cControl;
    public CameraInfo cInfo;
    private ProcessCameraProvider cameraProvider;
    private int flashMode = ImageCapture.FLASH_MODE_OFF;
    private Camera camera;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    public ScaleGestureDetector scaleGestureDetector;
    private SeekBar zoomBar;
    private View focusView;
    private Button cameraFlip;
    private PreviewView viewFinder;
    private Preview preview;
    private View cameraFlash;
    private Button camera_Capture_Button;
    private Handler handler = new Handler();
    private OrientationEventListener orientationEventListener;
    private ImageView ivBitmap;
    private ArrayList<String> bitmapArrayFileNames = new ArrayList<String>();
    private TextView noOfImages;
    private CardView  imageCardView;
    private int imagesCounter = 0;
    private Runnable focusingTOInvisible = new Runnable() {
        @Override
        public void run() {
            focusView.setVisibility(View.INVISIBLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_view);
        try {
            getSupportActionBar().hide();
        } catch (NullPointerException e) {

        }

        zoomBar = findViewById(R.id.zoomBar);
        zoomBar.setMax(100);
        zoomBar.setProgress(0);
        focusView = findViewById(R.id.focus);
        cameraFlip = findViewById(R.id.camera_flip);
        cameraFlash = findViewById(R.id.camera_flash);
        viewFinder = findViewById(R.id.viewFinder1);
        camera_Capture_Button = findViewById(R.id.camera_capture_button);
        ivBitmap = findViewById(R.id.ivBitmap);
        noOfImages = findViewById(R.id.txt_numberOfImages);
        imageCardView = findViewById(R.id.cardView);
        //Request Camera permission
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, ASK_MULTIPLE_PERMISSION_REQUEST_CODE);
        }




//--------------------------------->Capture Photo<--------------------------------------------------
        camera_Capture_Button.setOnClickListener(v -> {
            takePhotos();
            for(int i=0;i<bitmapArrayFileNames.size();i++){
                File file = new File(bitmapArrayFileNames.get(i));
                if(file.exists()){
                    imagesCounter++;
                }else{
                    imagesCounter--;
                }
            }
            noOfImages.setText(Integer.toString(imagesCounter));
        });

//---------------------------------->Switch to front Camera<----------------------------------------
        cameraFlip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA){
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                }else{
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                }
                try{
                    cameraProvider.unbindAll();
                    startCamera();
                }catch (Exception e){

                }
            }
        });
//--------------------------------------------------------------------------------------------------


        cameraFlash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (flashMode){
                        case ImageCapture.FLASH_MODE_OFF:
                            flashMode = ImageCapture.FLASH_MODE_ON;
                            cameraFlash.setBackground(ContextCompat.getDrawable(getBaseContext(),R.drawable.ic_flash_on));
                            break;
                        case ImageCapture.FLASH_MODE_ON:
                            flashMode = ImageCapture.FLASH_MODE_AUTO;
                            cameraFlash.setBackground(ContextCompat.getDrawable(getBaseContext(),R.drawable.ic_flash_auto));
                            break;
                        default:
                            flashMode = ImageCapture.FLASH_MODE_OFF;
                            cameraFlash.setBackground(ContextCompat.getDrawable(getBaseContext(),R.drawable.ic_flash_off));
                            break;
                    }
                    try{
                        imageCapture.setFlashMode(flashMode);
                    }catch (Exception e){

                    }
                }


        });
        imageCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(cameraActivity.this, imageEditorActivity.class);
                myIntent.putExtra("bitmapImages",bitmapArrayFileNames);
                startActivity(myIntent);
            }
        });
        outputDirectory = getOutputDirectory();
        cameraExecutor = Executors.newSingleThreadExecutor();

    }


//    private void takeBitmapPhotos() {
//        imageCapture.takePicture(ContextCompat.getMainExecutor(getBaseContext()), new ImageCapture.OnImageCapturedCallback() {
//
//            @Override
//            public void onCaptureSuccess(@NonNull ImageProxy image) {
//                bitmap = imageProxyToBitmap(image);
//                Glide.with(getBaseContext()).asBitmap().load(bitmap).into(ivBitmap);
//                fileName = "Images_"+System.currentTimeMillis()+".jpg";
//                bitmapArrayFileNames.add(fileName);
//
//                TakePhoto takePhoto = new TakePhoto();
//                takePhoto.execute();
//            }
//
//            @Override
//            public void onError(@NonNull ImageCaptureException exception) {
//                super.onError(exception);
//            }
//        });
//    }

//    private Bitmap imageProxyToBitmap(ImageProxy image) {
//        ImageProxy.PlaneProxy planeProxy = image.getPlanes()[0];
//        ByteBuffer buffer = planeProxy.getBuffer();
//        byte[] bytes = new byte[buffer.remaining()];
//        buffer.get(bytes);
//
//        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//    }


// -------------------->Enable to use normal take photo instead of asyncTask<-----------------------

    private void takePhotos() {
        focusView.setVisibility(View.INVISIBLE);
        File photoFile = new File(outputDirectory, "Image_"+System.currentTimeMillis()+".jpg");

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(getBaseContext()), new ImageCapture.OnImageSavedCallback() {
            Bitmap bitmap = viewFinder.getBitmap();
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                String absPath = photoFile.getAbsolutePath();
                bitmapArrayFileNames.add(absPath);
                ivBitmap.setImageBitmap(bitmap);
                imageCardView.setVisibility(View.VISIBLE);
                noOfImages.setVisibility(View.VISIBLE);
            }
            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(getBaseContext(),"Error Saving Image"+photoFile.getAbsolutePath(),Toast.LENGTH_SHORT).show();
            }
        });
    }

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


    @SuppressLint({"ClickableViewAccessibility", "RestrictedApi"})
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        try {
            orientationEventListener.enable();
        }catch (Exception e){

        }
        cameraProviderFuture.addListener(()->{
            try{
                cameraProvider = cameraProviderFuture.get();
                //bind Camera Preview to Surface provider ie:viewFinder in my case
                preview = new Preview.Builder().build();

                imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setTargetAspectRatio(AspectRatio.RATIO_4_3).setFlashMode(flashMode).build();
                Preview.SurfaceProvider surfaceProvider = viewFinder.getSurfaceProvider();
                preview.setSurfaceProvider(surfaceProvider);

                try {
                    cameraProvider.unbindAll();
                    camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                    cControl = camera.getCameraControl();
                    cInfo = camera.getCameraInfo();
                    //AutoFocus Every X Seconds
                    MeteringPointFactory AFfactory = new SurfaceOrientedMeteringPointFactory((float)viewFinder.getWidth(),(float)viewFinder.getHeight());
                    float centerWidth = (float)viewFinder.getWidth()/2;
                    float centerHeight = (float)viewFinder.getHeight()/2;
                    MeteringPoint AFautoFocusPoint = AFfactory.createPoint(centerWidth, centerHeight);
                    try {
                        FocusMeteringAction action = new FocusMeteringAction.Builder(AFautoFocusPoint,FocusMeteringAction.FLAG_AF).setAutoCancelDuration(1,TimeUnit.SECONDS).build();
                        cControl.startFocusAndMetering(action);
                    }catch (Exception e){

                    }

                    //AutoFocus CameraX
                    viewFinder.setOnTouchListener((v, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            handler.removeCallbacks(focusingTOInvisible);
                            focusView.setBackground(ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_focus));
                            focusView.setVisibility(View.VISIBLE);
                            return true;
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            MeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory((float) viewFinder.getWidth(), (float)viewFinder.getHeight());
                            MeteringPoint autoFocusPoint = factory.createPoint(event.getX(), event.getY());
                            FocusMeteringAction action = new FocusMeteringAction.Builder(autoFocusPoint,FocusMeteringAction.FLAG_AF).setAutoCancelDuration(5,TimeUnit.SECONDS).build();
                            ListenableFuture future = cControl.startFocusAndMetering(action);

                            future.addListener(()->{
                                handler.postDelayed(focusingTOInvisible,3000);
                                try{
                                    FocusMeteringResult result = (FocusMeteringResult) future.get();
                                    if(result.isFocusSuccessful()){
                                        focusView.setBackground(ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_focus_green));

                                    }
                                }catch (Exception e){

                                }
                            },cameraExecutor);



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

//    private class TakePhoto extends AsyncTask<String, String, String> {
//
//        @Override
//        protected String doInBackground(String... strings) {
//            try {
//                FileOutputStream stream = cameraActivity.this.openFileOutput(fileName,Context.MODE_PRIVATE);
//                bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);
//                stream.close();
//                //bitmap.recycle();
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            return null;
//        }
//    }


}