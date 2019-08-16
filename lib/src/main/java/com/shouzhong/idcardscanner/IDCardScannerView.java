package com.shouzhong.idcardscanner;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import exocr.exocrengine.EXOCREngine;

/**
 *
 *
 *
 */
public class IDCardScannerView extends FrameLayout implements Camera.PreviewCallback, CameraPreview.FocusAreaSetter {

    public static final String TAG = "IDCardScannerView";

    private CameraWrapper cameraWrapper;
    private IViewFinder viewFinderView;
    private CameraPreview cameraPreview;
    private Rect scaledRect;
    private ArrayList<Camera.Area> focusAreas;
    private CameraHandlerThread cameraHandlerThread;
    private boolean shouldAdjustFocusArea;//是否需要自动调整对焦区域
    private Callback callback;
    private int[] previewSize;
    private boolean isBackSide;
    private boolean isSaveBmp;
    private byte[] obtain;

    public IDCardScannerView(Context context) {
        this(context, null);
    }

    public IDCardScannerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IDCardScannerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Called as preview frames are displayed.<br/>
     * This callback is invoked on the event thread open(int) was called from.<br/>
     * (此方法与Camera.open运行于同一线程，在本项目中，就是CameraHandlerThread线程)
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (callback == null) return;
        try {
            Camera.Parameters parameters = camera.getParameters();
            int previewWidth = parameters.getPreviewSize().width;
            int previewHeight = parameters.getPreviewSize().height;
            //根据ViewFinderView和preview的尺寸之比，缩放扫码区域
            Rect rect = getScaledRect(previewWidth, previewHeight);
            int rotationCount = getRotationCount();//相机图像需要被顺时针旋转几次（每次90度）
            boolean isRotated = rotationCount == 1 || rotationCount == 3;
            byte[] matrix = getMatrix(data, rect, previewWidth, previewHeight);
            if (isRotated) matrix = rotateData(matrix, rect.width(), rect.height());
            int width = isRotated ? rect.height() : rect.width();
            int height = isRotated ? rect.width() : rect.height();
            int len = EXOCREngine.nativeRecoIDCardRawdat(matrix, width, height, width, 1, obtain, obtain.length);
            if (len <= 0) {
                getOneMoreFrame();
                return;
            }
            final Result result = Utils.decode(obtain, len);
            if (result == null || result.type != 1 && result.type != 2 || result.type == 1 && isBackSide || result.type == 2 && !isBackSide) {
                getOneMoreFrame();
                return;
            }
            if (isSaveBmp) {
                Bitmap bmp = Utils.nv21ToBitmap(data, previewWidth, previewHeight);
                bmp= Bitmap.createBitmap(bmp, rect.left, rect.top, rect.width(), rect.height());
                if (rotationCount != 0) {
                    Matrix m = new Matrix();
                    m.setRotate(rotationCount * 90, (float) bmp.getWidth() / 2, (float) bmp.getHeight() / 2);
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
                }
                Utils.saveBitmap(getContext(), result, bmp);
                if (TextUtils.isEmpty(result.path)) {
                    getOneMoreFrame();
                    return;
                }
            }
            post(new Runnable() {
                @Override
                public void run() {
                    if (callback == null) return;
                    if (isBackSide) callback.back(result);
                    else callback.face(result);
                }
            });
        } catch (Exception e) {
            getOneMoreFrame();
        }
    }

    @Override
    public void setAutoFocusArea() {
        //设置对焦区域
        if (!shouldAdjustFocusArea || cameraWrapper == null) return;
        Camera.Parameters parameters = cameraWrapper.camera.getParameters();
        if (parameters.getMaxNumFocusAreas() <= 0) {
            Log.e(TAG, "不支持设置对焦区域");
            return;
        }
        if (focusAreas == null) {
            int width = 2000, height = 2000;
            Rect framingRect = viewFinderView.getFramingRect();//获得扫码框区域
            if (framingRect == null) return;
            int viewFinderViewWidth = ((View) viewFinderView).getWidth();
            int viewFinderViewHeight = ((View) viewFinderView).getHeight();
            //1.根据ViewFinderView和2000*2000的尺寸之比，缩放对焦区域
            Rect scaledRect = new Rect(framingRect);
            scaledRect.left = scaledRect.left * width / viewFinderViewWidth;
            scaledRect.right = scaledRect.right * width / viewFinderViewWidth;
            scaledRect.top = scaledRect.top * height / viewFinderViewHeight;
            scaledRect.bottom = scaledRect.bottom * height / viewFinderViewHeight;
            //2.旋转对焦区域
            Rect rotatedRect = new Rect(scaledRect);
            int rotationCount = getRotationCount();
            if (rotationCount == 1) {//若相机图像需要顺时针旋转90度，则将扫码框逆时针旋转90度
                rotatedRect.left = scaledRect.top;
                rotatedRect.top = 2000 - scaledRect.right;
                rotatedRect.right = scaledRect.bottom;
                rotatedRect.bottom = 2000 - scaledRect.left;
            } else if (rotationCount == 2) {//若相机图像需要顺时针旋转180度,则将扫码框逆时针旋转180度
                rotatedRect.left = 2000 - scaledRect.right;
                rotatedRect.top = 2000 - scaledRect.bottom;
                rotatedRect.right = 2000 - scaledRect.left;
                rotatedRect.bottom = 2000 - scaledRect.top;
            } else if (rotationCount == 3) {//若相机图像需要顺时针旋转270度，则将扫码框逆时针旋转270度
                rotatedRect.left = 2000 - scaledRect.bottom;
                rotatedRect.top = scaledRect.left;
                rotatedRect.right = 2000 - scaledRect.top;
                rotatedRect.bottom = scaledRect.right;
            }
            //3.坐标系平移
            Rect rect = new Rect(rotatedRect.left - 1000, rotatedRect.top - 1000, rotatedRect.right - 1000, rotatedRect.bottom - 1000);
            Camera.Area area = new Camera.Area(rect, 1000);
            focusAreas = new ArrayList<>();
            focusAreas.add(area);
        }
        parameters.setFocusAreas(focusAreas);
        cameraWrapper.camera.setParameters(parameters);
    }

    // ******************************************************************************
    //
    // ******************************************************************************

    /**
     * 回调
     *
     * @param callback
     */
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    /**
     * 扫描区域
     *
     * @param viewFinderView
     */
    public void setViewFinder(IViewFinder viewFinderView) {
        if (viewFinderView == null || !(viewFinderView instanceof View)) throw new IllegalArgumentException("viewFinderView必须是View对象");
        this.viewFinderView = viewFinderView;
    }

    /**
     * 开启扫描
     *
     */
    public void onResume() {
        startCamera();
    }

    /**
     * 停止扫描
     *
     */
    public void onPause() {
        stopCamera();
    }

    /**
     * 重启
     *
     * @param delayMillis
     */
    public void restartPreviewAfterDelay(long delayMillis) {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                getOneMoreFrame();
            }
        }, delayMillis);
    }

    /**
     * 开启/关闭闪光灯
     */
    public void setFlash(boolean flag) {
        if (cameraWrapper == null || !CameraUtils.isFlashSupported(cameraWrapper.camera)) return;
        Camera.Parameters parameters = cameraWrapper.camera.getParameters();
        if (TextUtils.equals(parameters.getFlashMode(), Camera.Parameters.FLASH_MODE_TORCH) && flag)
            return;
        if (TextUtils.equals(parameters.getFlashMode(), Camera.Parameters.FLASH_MODE_OFF) && !flag)
            return;
        parameters.setFlashMode(flag ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
        cameraWrapper.camera.setParameters(parameters);
    }

    /**
     * 切换闪光灯的点亮状态
     */
    public void toggleFlash() {
        if (cameraWrapper == null || !CameraUtils.isFlashSupported(cameraWrapper.camera)) return;
        Camera.Parameters parameters = cameraWrapper.camera.getParameters();
        if (TextUtils.equals(parameters.getFlashMode(), Camera.Parameters.FLASH_MODE_TORCH)) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        } else {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        }
        cameraWrapper.camera.setParameters(parameters);
    }

    /**
     * 闪光灯是否被点亮
     */
    public boolean isFlashOn() {
        if (cameraWrapper == null || !CameraUtils.isFlashSupported(cameraWrapper.camera)) return false;
        Camera.Parameters parameters = cameraWrapper.camera.getParameters();
        return TextUtils.equals(parameters.getFlashMode(), Camera.Parameters.FLASH_MODE_TORCH);
    }

    /**
     * 设置是否要根据扫码框的位置去调整对焦区域的位置<br/>
     * 默认值为false，即不调整，会使用系统默认的配置，那么对焦区域会位于预览画面的中央<br/>
     * <br/>
     * (经测试，此功能对少数机型无效，待优化)
     */
    public void setShouldAdjustFocusArea(boolean shouldAdjustFocusArea) {
        this.shouldAdjustFocusArea = shouldAdjustFocusArea;
    }

    /**
     * 设置是否扫描正面，非为反面
     *
     * @param b
     */
    public void setBackSide(boolean b) {
        isBackSide = b;
    }

    /**
     * 是否保存图片
     *
     * @param b
     */
    public void setSaveBmp(boolean b) {
        isSaveBmp = b;
    }

    // ******************************************************************************
    //
    // ******************************************************************************

    /**
     * 再获取一帧图像数据进行识别（会再次触发onPreviewFrame方法）
     */
    private void getOneMoreFrame() {
        if (cameraWrapper != null) {
            try {
                cameraWrapper.camera.setOneShotPreviewCallback(this);
            } catch (Exception e) {}
        }
    }

    void setupCameraPreview(final CameraWrapper cameraWrapper) {
        this.cameraWrapper = cameraWrapper;
        if (this.cameraWrapper == null) return;
        removeAllViews();
        if (previewSize == null) previewSize = getOptimalPreviewSize(getMeasuredWidth(), getMeasuredHeight());
        cameraPreview = new CameraPreview(getContext(), previewSize[0], previewSize[1], cameraWrapper, this, this);
        addView(cameraPreview);
        addView(((View) viewFinderView));
    }

    /**
     * 打开系统相机，并进行基本的初始化
     */
    private void startCamera() {
        if (cameraHandlerThread == null) {
            cameraHandlerThread = new CameraHandlerThread(this);
        }
        cameraHandlerThread.startCamera(CameraUtils.getDefaultCameraId());
        obtain = new byte[4396];
    }

    /**
     * 释放相机资源等各种资源
     */
    private void stopCamera() {
        if (cameraHandlerThread != null) {
            cameraHandlerThread.quit();
            cameraHandlerThread = null;
        }
        if (cameraWrapper != null) {
            cameraPreview.stopCameraPreview();//停止相机预览并置空各种回调
            cameraPreview = null;
            cameraWrapper.camera.release();//释放资源
            cameraWrapper = null;
        }
        removeAllViews();
        Utils.clearDict();
        scaledRect = null;
        obtain = null;
    }

    /**
     * 根据ViewFinderView和preview的尺寸之比，缩放扫码区域
     */
    private Rect getScaledRect(int previewWidth, int previewHeight) {
        if (scaledRect == null) {
            Rect framingRect = viewFinderView.getFramingRect();//获得扫码框区域
            int viewFinderViewWidth = ((View) viewFinderView).getWidth();
            int viewFinderViewHeight = ((View) viewFinderView).getHeight();
            scaledRect = new Rect(framingRect);
            Point p = new Point();
            ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(p);
            int o = p.x == p.y ? 0 : p.x < p.y ? Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE;
            float ratio = o == Configuration.ORIENTATION_PORTRAIT ? previewHeight * 1f / previewWidth : previewWidth * 1f / previewHeight;
            float r = viewFinderViewWidth * 1f / viewFinderViewHeight;
            if (ratio < r){
                int width = o == Configuration.ORIENTATION_PORTRAIT ? previewHeight : previewWidth;
                scaledRect.left = scaledRect.left * width / viewFinderViewWidth;
                scaledRect.right = scaledRect.right * width / viewFinderViewWidth;
                scaledRect.top = scaledRect.top * width / viewFinderViewWidth;
                scaledRect.bottom = scaledRect.bottom * width / viewFinderViewWidth;
            } else {
                int height = o == Configuration.ORIENTATION_PORTRAIT ? previewWidth : previewHeight;
                scaledRect.left = scaledRect.left * height / viewFinderViewHeight;
                scaledRect.right = scaledRect.right * height / viewFinderViewHeight;
                scaledRect.top = scaledRect.top * height / viewFinderViewHeight;
                scaledRect.bottom = scaledRect.bottom * height / viewFinderViewHeight;
            }
            int rotationCount = getRotationCount();//相机图像需要被顺时针旋转几次（每次90度）
            if (rotationCount == 1 || rotationCount == 3) {//相机图像需要顺时针旋转90度或270度
                int temp1 = scaledRect.left;
                scaledRect.left = scaledRect.top;
                scaledRect.top = temp1;
                int temp2 = scaledRect.right;
                scaledRect.right = scaledRect.bottom;
                scaledRect.bottom = temp2;
            }
            if (scaledRect.left < 0)  scaledRect.left = 0;
            if (scaledRect.top < 0) scaledRect.top = 0;
            if (scaledRect.right > previewWidth) scaledRect.right = previewWidth;
            if (scaledRect.bottom > previewHeight) scaledRect.bottom = previewHeight;
        }
        return scaledRect;
    }

    /**
     * 旋转数据
     *
     * @param data
     * @param width
     * @param height
     * @return
     */
    private byte[] rotateData(byte[] data, int width, int height) {
        int rotationCount = getRotationCount();
        if (rotationCount == 0 || rotationCount == 2) return data;
        byte[] bs = new byte[data.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bs[x * height + height - y - 1] = data[x + y * width];
            }
        }
        return bs;
    }

    /**
     * 获取矩形框的数据
     *
     * @param data
     * @param rect
     * @param previewWidth
     * @param previewHeight
     * @return
     */
    private byte[] getMatrix(byte[] data, Rect rect, int previewWidth, int previewHeight) {
        if (rect.width() == previewWidth && rect.height() == previewHeight) return data;
        int area = rect.width() * rect.height();
        byte[] matrix = new byte[area];
        int inputOffset = rect.top * previewWidth + rect.left;
        if (rect.width() == previewWidth) {
            System.arraycopy(data, inputOffset, matrix, 0, area);
            return matrix;
        }
        for (int y = 0; y < rect.height(); y++) {
            int outputOffset = y * rect.width();
            System.arraycopy(data, inputOffset, matrix, outputOffset, rect.width());
            inputOffset += previewWidth;
        }
        return matrix;
    }

    /**
     * 获取（旋转角度/90）
     */
    private int getRotationCount() {
        int displayOrientation = cameraPreview.getDisplayOrientation();
        return displayOrientation / 90;
    }

    /**
     * 找到一个合适的previewSize（根据控件的尺寸）
     *
     * @param width 控件宽度
     * @param height 控件高度
     */
    private int[] getOptimalPreviewSize(int width, int height) {
        if (cameraWrapper == null) return new int[] {0, 0};
        //相机图像默认都是横屏(即宽>高)
        List<Camera.Size> sizes = cameraWrapper.camera.getParameters().getSupportedPreviewSizes();
        if (sizes == null) return new int[] {0, 0};
        int w, h;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            w = width;
            h = height;
        } else {
            w = height;
            h = width;
        }
        double targetRatio = w * 1.0 / h;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        double aspectTolerance = Double.MAX_VALUE;
        int targetHeight = h;

        // 获取最佳尺寸
        for (Camera.Size size : sizes) {
            double ratio = size.width * 1.0 / size.height;
            if (Math.abs(ratio - targetRatio) > aspectTolerance) continue;
            if (Math.abs(size.height - targetHeight) <= minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
                aspectTolerance = Math.abs(ratio - targetRatio);
            }
        }
        return new int[] {optimalSize.width, optimalSize.height};
    }
}