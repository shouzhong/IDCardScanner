package com.shouzhong.idcardscanner;

import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

class CameraHandlerThread extends HandlerThread {

    private IDCardScannerView mScannerView;

    public CameraHandlerThread(IDCardScannerView scannerView) {
        super("CameraHandlerThread");
        mScannerView = scannerView;
        start();
    }

    /**
     * 打开系统相机，并进行基本的初始化
     */
    public void startCamera(final int cameraId) {
        Handler localHandler = new Handler(getLooper());
        localHandler.post(new Runnable() {
            @Override
            public void run() {
                // 第一次初始化会出现失败
                boolean boo = Utils.initDict(mScannerView.getContext());
                Log.e("==============", "init=" + boo);
                if (!boo) {
                    boo = Utils.initDict(mScannerView.getContext());
                    Log.e("==============", "init=" + boo);
                }
                final Camera camera = CameraUtils.getCamera(cameraId);//打开camera
                Handler mainHandler = new Handler(Looper.getMainLooper());//切换到主线程
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mScannerView.setupCameraPreview(CameraWrapper.getWrapper(camera, cameraId));
                    }
                });
            }
        });
    }
}
