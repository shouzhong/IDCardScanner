package com.shouzhong.idcardscanner.demo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.shouzhong.idcardscanner.IDCardUtils;
import com.shouzhong.idcardscanner.Result;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClick2(View v) {
        Intent intent = new Intent(this, PortraitScannerActivity.class);
        startActivity(intent);
    }

    public void onClick1(View v) {
        Intent intent = new Intent(this, ScannerActivity.class);
        startActivity(intent);
    }

    public void onClick3(View v){
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.mipmap.img1);
        Result result = IDCardUtils.decode(this, bmp);
        Log.e("==============", result == null ? "识别失败" : result.toString());
    }
}
