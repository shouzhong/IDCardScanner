package com.shouzhong.idcardscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;

import exocr.exocrengine.EXOCREngine;

public class IDCardUtils {

    /**
     * 识别图片
     *
     * @param bmp
     * @return
     */
    public static Result decode(Context context, Bitmap bmp) {
        if (bmp == null) return null;
        boolean boo = Utils.initDict(context);
        if (!boo) Utils.initDict(context);
        final byte[] obtain = new byte[4096];
        int len = EXOCREngine.nativeRecoIDCardBitmap(bmp, obtain, obtain.length);
        if (len <= 0) {
            Matrix m = new Matrix();
            m.setRotate(90, (float) bmp.getWidth() / 2, (float) bmp.getHeight() / 2);
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            len = EXOCREngine.nativeRecoIDCardBitmap(bmp, obtain, obtain.length);
        }
        Utils.clearDict();
        if (len <= 0) return null;
        return Utils.decode(obtain, len);
    }
}
