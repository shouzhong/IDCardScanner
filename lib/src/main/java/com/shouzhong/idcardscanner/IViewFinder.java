package com.shouzhong.idcardscanner;

import android.graphics.Rect;

public interface IViewFinder {

    /**
     * 获得扫码区域(识别区域)
     */
    Rect getFramingRect();
}