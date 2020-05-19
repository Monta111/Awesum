package com.monta.awesum.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;
import com.monta.awesum.R;
import com.monta.awesum.activity.AddPostActivity;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraFragment extends Fragment {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider provider;

    private PreviewView previewView;
    private ImageView capture;
    private ImageView flash;
    private ImageView switchCamera;
    private ImageView close;

    private int itemType;

    private Executor executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        previewView = view.findViewById(R.id.preview_view);
        capture = view.findViewById(R.id.capture);
        flash = view.findViewById(R.id.flash);
        switchCamera = view.findViewById(R.id.switch_camera);
        close = view.findViewById(R.id.close);

        if (getArguments() != null) {
            itemType = getArguments().getInt("itemType", 0);
        }

        setCloseAction();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());
        startCamera();
        setSwitchCamera();

    }

    private void setCloseAction() {
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
    }

    private void startCamera() {

        cameraProviderFuture.addListener(() -> {
            try {
                provider = cameraProviderFuture.get();
                bindPreview(provider, true);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }


    private void bindPreview(ProcessCameraProvider cameraProvider, boolean isFacingBack) {
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector;

        if (isFacingBack)
            cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();
        else
            cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build();

        ImageCapture imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();

        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        CameraControl cameraControl = camera.getCameraControl();
        CameraInfo cameraInfo = camera.getCameraInfo();

        preview.setSurfaceProvider(previewView.createSurfaceProvider(cameraInfo));

        capture.setOnClickListener(v -> {
            File temp = null;
            try {
                temp = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Error!", Toast.LENGTH_SHORT).show();
            }

            if (temp != null) {
                ImageCapture.OutputFileOptions outputFileOptions
                        = new ImageCapture.OutputFileOptions.Builder(temp).build();
                File finalTemp = temp;

                WeakReference<Context> weakContext = new WeakReference<>(getContext());
                imageCapture.takePicture(outputFileOptions, executor,
                        new ImageCapture.OnImageSavedCallback() {
                            @Override
                            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                                Intent intent = new Intent(weakContext.get(), AddPostActivity.class);
                                intent.putExtra("itemType", itemType);
                                ArrayList<String> uriStringList = new ArrayList<>();
                                Uri uri = Uri.fromFile(finalTemp);
                                uriStringList.add(uri.toString());
                                intent.putStringArrayListExtra("selectedList", uriStringList);
                                startActivity(intent);
                                getActivity().finish();
                            }

                            @Override
                            public void onError(@NonNull ImageCaptureException exception) {

                            }
                        });
            }

        });

        if (isFacingBack)
            flash.setOnClickListener(v -> {
                if (cameraInfo.getTorchState().getValue() != null) {
                    if (cameraInfo.getTorchState().getValue() == TorchState.ON) {
                        Glide.with(getContext()).load(R.drawable.ic_flash_off).into(flash);
                        cameraControl.enableTorch(false);
                    } else {
                        Glide.with(getContext()).load(R.drawable.ic_flash_on).into(flash);
                        cameraControl.enableTorch(true);
                    }
                }
            });
        else
            flash.setVisibility(View.GONE);
    }

    private void setSwitchCamera() {
        switchCamera.setOnClickListener(v -> {
            provider.unbindAll();
            bindPreview(provider, false);
        });
    }

    private File createImageFile() throws IOException {
        File storageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("temp", ".jpg", storageDir);
    }

    @Override
    public void onStop() {
        super.onStop();
        provider.unbindAll();
    }
}
