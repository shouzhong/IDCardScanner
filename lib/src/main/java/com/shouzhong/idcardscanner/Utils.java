package com.shouzhong.idcardscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import exocr.exocrengine.EXOCREngine;

class Utils {
    /**
     * 解析
     *
     * @param bResultBuf
     * @param reslen
     * @return
     */
    static final Result decode(byte[] bResultBuf, int reslen) {
        byte code;
        int i, j, rdcount;
        String content = null;
        Result idcard = new Result();
        rdcount = 0;
        idcard.type = bResultBuf[rdcount++];
        while (rdcount < reslen) {
            code = bResultBuf[rdcount++];
            i = 0;
            j = rdcount;
            while (rdcount < reslen) {
                i++;
                rdcount++;
                if (bResultBuf[rdcount] == 0x20) break;
            }
            try {
                content = new String(bResultBuf, j, i, "GBK");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (code == 0x21) {
                idcard.cardNum = content;
                String year = idcard.cardNum.substring(6, 10);
                String month = idcard.cardNum.substring(10, 12);
                String day = idcard.cardNum.substring(12, 14);
                idcard.birth = year + "-" + month + "-" + day;
            } else if (code == 0x22) {
                idcard.name = content;
            } else if (code == 0x23) {
                idcard.sex = content;
            } else if (code == 0x24) {
                idcard.nation = content;
            } else if (code == 0x25) {
                idcard.address = content;
            } else if (code == 0x26) {
                idcard.office = content;
            } else if (code == 0x27) {
                idcard.validDate = content;
            }
            rdcount++;
        }
        //is it correct, check it!
        if (idcard.type == 1 && (idcard.cardNum == null || idcard.name == null || idcard.nation == null || idcard.sex == null || idcard.address == null) ||
                idcard.type == 2 && (idcard.office == null || idcard.validDate == null) ||
                idcard.type == 0) {
            return null;
        } else {
            if (idcard.type == 1 && (idcard.cardNum.length() != 18 || idcard.name.length() < 2 || idcard.address.length() < 10)) {
                return null;
            }
        }
        return idcard;
    }

    /**
     *  nv21转bitmap
     *
     * @param nv21
     * @param width
     * @param height
     * @return
     */
    static final Bitmap nv21ToBitmap(byte[] nv21, int width, int height){
        Bitmap bitmap = null;
        try {
            YuvImage image = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, width, height), 80, stream);
            bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    /**
     * 保存图片
     *
     * @param context
     * @param result
     * @param bitmap
     */
    static final void saveBitmap(final Context context, Result result, Bitmap bitmap) {
        try {
            final String local;
            long l = System.currentTimeMillis();
            if (result.type == 1) {
                local = context.getExternalCacheDir().getAbsolutePath() + "/card_front_" + l + ".jpg";
            } else if (result.type == 2) {
                local = context.getExternalCacheDir().getAbsolutePath() + "/card_back_" + l + ".jpg";
            } else {
                return;
            }
            final File file = new File(local);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            result.path = local;
            if (null != bitmap) {
                bitmap.recycle();
            }
        } catch (Exception e) { }
    }

    /**
     * 字典初始化
     *
     * @param context
     * @return
     */
    static final boolean initDict(final Context context) {
        final String name = "/zocr0.lib";
        final String path = context.getExternalCacheDir().getPath();
        final String pathname = path + name;
        // step1: 检测字典是否存在
        boolean okFile = checkFile(context, pathname);
        if (!okFile) {
            clearDict();
            return false;
        }
        // step2: 检测字典是否正确
        boolean okDict = checkDict(path);
        if (!okDict) {
            clearDict();
            return false;
        }
        // step3: 检测字典签名
        return checkSign(context);
    }

    /**
     * 清除字典
     *
     */
    static final void clearDict() {
        int code = EXOCREngine.nativeDone();
        Log.e("kalu", "clearDict ==> code = "+code);
    }

    private static final boolean checkSign(final Context context) {
        int code = EXOCREngine.nativeCheckSignature(context);
        Log.e("kalu", "checkSign ==> code = " + code);
        return code == 1;
    }

    private static final boolean checkDict(final String path) {
        final byte[] bytes = path.getBytes();
        final int code = EXOCREngine.nativeInit(bytes);
        Log.e("kalu", "checkDict ==> code = " + code);
        return code >= 0;
    }

    private static final boolean checkFile(final Context context, final String pathname) {
        try {
            //如果文件已存在，则删除文件
            File file = new File(pathname);
            if (file.exists()) return true;
            //在assets资产目录下获取授权文件
            InputStream myInput = context.getResources().openRawResource(R.raw.zocr0);
            //将授权文件写到 data/data/包名 目录下
            OutputStream myOutput = new FileOutputStream(pathname);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }
            myOutput.flush();
            myOutput.close();
            myInput.close();
            return true;
        } catch (Exception e) {
            Log.e("===============", e.getMessage());
            return false;
        }
    }
}


