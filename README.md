# IDCardScanner
## 说明
扫描身份证获取身份证信息，你可以定义任何位置任何尺寸的预览而不会导致摄像头预览变形
## 效果图

<img width="480" height="270" src="https://github.com/shouzhong/IDCardScanner/blob/master/Screenshots/1.jpg"/>

## 使用
### 依赖
```
implementation 'com.shouzhong:IDCardScanner:1.0.0'
```
### 代码
xml
```
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <com.shouzhong.idcardscanner.IDCardScannerView
        android:id="@+id/card"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#000000"/>
</RelativeLayout>
```
java
```
@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_scanner);
    idCardScannerView = findViewById(R.id.card);
    idCardScannerView.setViewFinder(new ViewFinder(this));
    idCardScannerView.setCallback(new Callback() {
        @Override
        public void result(Result result) {
            Log.e("==================", result.toString());
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
```
这里没给默认的预览页面，需要自己自定义，下面给个例子
```
class ViewFinder extends View implements IViewFinder {
    private Rect framingRect;//扫码框所占区域
    private float widthHeightRatio = 1.6f;//扫码框宽度占view总宽度的比例
    private float heightRatio = 0.7f;// 扫码框高度占view总宽度的比例
    private int leftOffset = -1;//扫码框相对于左边的偏移量，若为负值，则扫码框会水平居中
    private int topOffset = -1;//扫码框相对于顶部的偏移量，若为负值，则扫码框会竖直居中

    private int maskColor = 0x60000000;// 阴影颜色
    private int borderColor = 0xcc303853;// 边框颜色
    private int borderStrokeWidth = 12;// 边框宽度
    private int borderLineLength = 72;// 边框长度

    private Paint maskPaint;// 阴影遮盖画笔
    private Paint borderPaint;// 边框画笔

    public ViewFinder(Context context) {
        super(context);
        setWillNotDraw(false);//需要进行绘制
        maskPaint = new Paint();
        maskPaint.setColor(maskColor);
        borderPaint = new Paint();
        borderPaint.setColor(borderColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(borderStrokeWidth);
        borderPaint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        updateFramingRect();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (getFramingRect() == null) {
            return;
        }
        drawViewFinderMask(canvas);
        drawViewFinderBorder(canvas);
    }

    /**
     * 绘制扫码框四周的阴影遮罩
     */
    private void drawViewFinderMask(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        Rect framingRect = getFramingRect();
        canvas.drawRect(0, 0, width, framingRect.top, maskPaint);//扫码框顶部阴影
        canvas.drawRect(0, framingRect.top, framingRect.left, framingRect.bottom, maskPaint);//扫码框左边阴影
        canvas.drawRect(framingRect.right, framingRect.top, width, framingRect.bottom, maskPaint);//扫码框右边阴影
        canvas.drawRect(0, framingRect.bottom, width, height, maskPaint);//扫码框底部阴影
    }

    /**
     * 绘制扫码框的边框
     */
    private void drawViewFinderBorder(Canvas canvas) {
        Rect framingRect = getFramingRect();

        // Top-left corner
        Path path = new Path();
        path.moveTo(framingRect.left, framingRect.top + borderLineLength);
        path.lineTo(framingRect.left, framingRect.top);
        path.lineTo(framingRect.left + borderLineLength, framingRect.top);
        canvas.drawPath(path, borderPaint);

        // Top-right corner
        path.moveTo(framingRect.right, framingRect.top + borderLineLength);
        path.lineTo(framingRect.right, framingRect.top);
        path.lineTo(framingRect.right - borderLineLength, framingRect.top);
        canvas.drawPath(path, borderPaint);

        // Bottom-right corner
        path.moveTo(framingRect.right, framingRect.bottom - borderLineLength);
        path.lineTo(framingRect.right, framingRect.bottom);
        path.lineTo(framingRect.right - borderLineLength, framingRect.bottom);
        canvas.drawPath(path, borderPaint);

        // Bottom-left corner
        path.moveTo(framingRect.left, framingRect.bottom - borderLineLength);
        path.lineTo(framingRect.left, framingRect.bottom);
        path.lineTo(framingRect.left + borderLineLength, framingRect.bottom);
        canvas.drawPath(path, borderPaint);
    }

    /**
     * 设置framingRect的值（扫码框所占的区域）
     */
    private synchronized void updateFramingRect() {
        Point viewSize = new Point(getWidth(), getHeight());
        int width, height;
        height = (int) (getHeight() * heightRatio);
        width = (int) (height * widthHeightRatio);

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
```
## 方法说明

IDCardScannerView

方法名 | 说明
------------ | -------------
setViewFinder | 扫描区域
setCallback | 扫码成功后的回调
onResume | 开启扫描
onPause | 停止扫描
restartPreviewAfterDelay | 设置多少毫秒后重启扫描
setFlash | 开启/关闭闪光灯
toggleFlash | 切换闪光灯的点亮状态
isFlashOn | 闪光灯是否被点亮
setShouldAdjustFocusArea | 设置是否要根据扫码框的位置去调整对焦区域的位置，部分手机不支持
setBackSide | 设置识别是否为反面（国徽）
setSaveBmp | 设置是否保存图片

IDCardUtils

方法名 | 说明
------------ | -------------
decode | 识别图片

## 返回值说明

Result

属性 | 说明
------------ | -------------
type | 1为正面，2为反面
cardNum | 身份证号
name | 名字
sex | 性别
address | 地址
nation | 民族
birth | 出生年月日：yyyy-MM-dd
office | 签发机关
validDate | 有限期限：yyyyMMdd-yyyyMMdd
path | 图片路径


## 混淆
```
-dontwarn exocr.exocrengine.**
-keep class exocr.exocrengine.** {*;}
```
