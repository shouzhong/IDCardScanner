package com.shouzhong.idcardscanner.demo;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.shouzhong.idcardscanner.Callback;
import com.shouzhong.idcardscanner.IDCardScannerView;
import com.shouzhong.idcardscanner.IViewFinder;
import com.shouzhong.idcardscanner.Result;

public class PortraitScannerActivity extends AppCompatActivity {

    private IDCardScannerView idCardScannerView;
    private Vibrator vibrator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portrait_scanner);
        idCardScannerView = findViewById(R.id.card);
        idCardScannerView.setViewFinder(new ViewFinder(this));
        idCardScannerView.setBackSide(true);
        idCardScannerView.setSaveBmp(true);
        idCardScannerView.setCallback(new Callback() {
            @Override
            public void face(Result result) {
                Log.e("=======正面=========", result.toString());
                startVibrator();
                idCardScannerView.restartPreviewAfterDelay(2000);
            }

            @Override
            public void back(Result result) {
                Log.e("======背面=========", result.toString());
                startVibrator();
                idCardScannerView.restartPreviewAfterDelay(2000);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        idCardScannerView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        idCardScannerView.onPause();
    }

    @Override
    protected void onDestroy() {
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        super.onDestroy();
    }

    private void startVibrator() {
        if (vibrator == null)
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(300);
    }

    class ViewFinder extends View implements IViewFinder {
        private Rect framingRect;//扫码框所占区域
        private float widthRatio = 1f;//扫码框宽度占view总宽度的比例
        private float heightRatio = 1f;// 扫码框高度占view总宽度的比例
        private int leftOffset = 0;//扫码框相对于左边的偏移量，若为负值，则扫码框会水平居中
        private int topOffset = 0;//扫码框相对于顶部的偏移量，若为负值，则扫码框会竖直居中

        public ViewFinder(Context context) {
            super(context);
            setWillNotDraw(false);//需要进行绘制
        }

        @Override
        protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
            updateFramingRect();
        }

        /**
         * 设置framingRect的值（扫码框所占的区域）
         */
        private synchronized void updateFramingRect() {
            Point viewSize = new Point(getWidth(), getHeight());
            int width = (int) (getWidth() * widthRatio);
            int height = (int) (getHeight() * heightRatio);

            int left, top;
            if (leftOffset < 0) {
                left = (viewSize.x - width) / 2;//水平居中
            } else {
                left = leftOffset;
            }
            if (topOffset < 0) {
                top = (viewSize.y - height) / 2;//竖直居中
            } else {
                top = topOffset;
            }
            framingRect = new Rect(left, top, left + width, top + height);
        }

        @Override
        public Rect getFramingRect() {
            return framingRect;
        }
    }
}
