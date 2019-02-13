package com.example.macmini1.takephoto.crop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


public class CropImageView extends View {
    // 在touch重要用到的点，
    private float mX_1 = 0;
    private float mY_1 = 0;
    // 触摸事件判断
    private final int STATUS_SINGLE = 1;
    private final int STATUS_MULTI_START = 2;
    private final int STATUS_MULTI_TOUCHING = 3;
    // 当前状态
    private int mStatus = STATUS_SINGLE;
    // 默认裁剪的宽高
    private int cropWidth;
    private int cropHeight;
    // 浮层Drawable的四个点
    private final int EDGE_LT = 1;
    private final int EDGE_RT = 2;
    private final int EDGE_LB = 3;
    private final int EDGE_RB = 4;
    private final int EDGE_MOVE_IN = 5;
    private final int EDGE_MOVE_OUT = 6;
    private final int EDGE_NONE = 7;

    public int currentEdge = EDGE_NONE;

    protected float oriRationWH = 0;
    protected final float maxZoomOut = 5.0f;
    protected final float minZoomIn = 0.333333f;

    protected Drawable mDrawable;
    protected FloatDrawable mFloatDrawable;

    protected Rect mDrawableSrc = new Rect();// 图片Rect变换时的Rect
    protected Rect mDrawableDst = new Rect();// 图片Rect
    protected Rect mDrawableFloat = new Rect();// 浮层的Rect
    protected boolean isFrist = true;
    private boolean isTouchInSquare = true;

    protected Context mContext;

    public CropImageView(Context context) {
        super(context);
        init(context);
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CropImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);

    }

    private void init(Context context) {
        this.mContext = context;
        try {
            if (android.os.Build.VERSION.SDK_INT >= 11) {
                this.setLayerType(LAYER_TYPE_SOFTWARE, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mFloatDrawable = new FloatDrawable(context);
    }

    public void setDrawable(Drawable mDrawable, int cropWidth, int cropHeight) {
        this.mDrawable = mDrawable;
        this.cropWidth = cropWidth;
        this.cropHeight = cropHeight;
        this.isFrist = true;
        invalidate();
    }

    private File file;
    public void setDrawableFile(File file,int cropWidth, int cropHeight) {
        this.file = file;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        //设置资源和默认长宽
        setDrawable(new BitmapDrawable(bitmap), cropWidth, cropHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getPointerCount() > 1) {
            if (mStatus == STATUS_SINGLE) {
                mStatus = STATUS_MULTI_START;
            } else if (mStatus == STATUS_MULTI_START) {
                mStatus = STATUS_MULTI_TOUCHING;
            }
        } else {
            if (mStatus == STATUS_MULTI_START
                    || mStatus == STATUS_MULTI_TOUCHING) {
                mX_1 = event.getX();
                mY_1 = event.getY();
            }

            mStatus = STATUS_SINGLE;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mX_1 = event.getX();
                mY_1 = event.getY();
                currentEdge = getTouch((int) mX_1, (int) mY_1);
                isTouchInSquare = mDrawableFloat.contains((int) event.getX(),
                        (int) event.getY());

                break;

            case MotionEvent.ACTION_UP:
                checkBounds();
                break;

            case MotionEvent.ACTION_POINTER_UP:
                currentEdge = EDGE_NONE;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mStatus == STATUS_MULTI_TOUCHING) {

                } else if (mStatus == STATUS_SINGLE) {
                    int dx = (int) (event.getX() - mX_1);
                    int dy = (int) (event.getY() - mY_1);

                    mX_1 = event.getX();
                    mY_1 = event.getY();
                    // 根據得到的那一个角，并且变换Rect
                    if (!(dx == 0 && dy == 0)) {
                        /**限制裁剪框只能在图片区域*/
                        int x = mDrawableDst.left;
                        int y = mDrawableDst.top;
                        int w = mDrawableDst.right;
                        int h = mDrawableDst.bottom;

                        switch (currentEdge) {
                            case EDGE_LT: {
                                int l = mDrawableFloat.left + dx;
                                int t = mDrawableFloat.top + dy;
                                int r = mDrawableFloat.right;
                                int b = mDrawableFloat.bottom;
                                if (l < x || t < y || r > w || b > h) {

                                } else {
                                    mDrawableFloat.set(l, t, r, b);
                                }
                            }
                            break;

                            case EDGE_RT: {
                                int l = mDrawableFloat.left;
                                int t = mDrawableFloat.top + dy;
                                int r = mDrawableFloat.right + dx;
                                int b = mDrawableFloat.bottom;
                                if (l < x || t < y || r > w || b > h) {

                                } else {
                                    mDrawableFloat.set(l, t, r, b);
                                }
                            }
                            break;

                            case EDGE_LB: {
                                int l = mDrawableFloat.left + dx;
                                int t = mDrawableFloat.top;
                                int r = mDrawableFloat.right;
                                int b = mDrawableFloat.bottom + dy;
                                if (l < x || t < y || r > w || b > h) {

                                } else {
                                    mDrawableFloat.set(l, t, r, b);
                                }
                            }
                            break;

                            case EDGE_RB: {
                                int l = mDrawableFloat.left;
                                int t = mDrawableFloat.top;
                                int r = mDrawableFloat.right + dx;
                                int b = mDrawableFloat.bottom + dy;
                                if (l < x || t < y || r > w || b > h) {

                                } else {
                                    mDrawableFloat.set(l, t, r, b);
                                }
                            }
                            break;

                            case EDGE_MOVE_IN:
                                if (isTouchInSquare) {
                                    int l = mDrawableFloat.left + dx;
                                    int t = mDrawableFloat.top + dy;
                                    int r = mDrawableFloat.right + dx;
                                    int b = mDrawableFloat.bottom + dy;
                                    if (l < x || t < y || r > w || b > h) {

                                    } else {
                                        mDrawableFloat.offset((int) dx, (int) dy);
                                    }
                                }
                                break;

                            case EDGE_MOVE_OUT:
                                break;
                        }
                        mDrawableFloat.sort();
                        invalidate();
                    }
                }
                break;
        }

        return true;
    }

    // 根据初触摸点判断是触摸的Rect哪一个角
    public int getTouch(int eventX, int eventY) {
        if (mFloatDrawable.getBounds().left <= eventX
                && eventX < (mFloatDrawable.getBounds().left + mFloatDrawable
                .getBorderWidth())
                && mFloatDrawable.getBounds().top <= eventY
                && eventY < (mFloatDrawable.getBounds().top + mFloatDrawable
                .getBorderHeight())) {
            return EDGE_LT;
        } else if ((mFloatDrawable.getBounds().right - mFloatDrawable
                .getBorderWidth()) <= eventX
                && eventX < mFloatDrawable.getBounds().right
                && mFloatDrawable.getBounds().top <= eventY
                && eventY < (mFloatDrawable.getBounds().top + mFloatDrawable
                .getBorderHeight())) {
            return EDGE_RT;
        } else if (mFloatDrawable.getBounds().left <= eventX
                && eventX < (mFloatDrawable.getBounds().left + mFloatDrawable
                .getBorderWidth())
                && (mFloatDrawable.getBounds().bottom - mFloatDrawable
                .getBorderHeight()) <= eventY
                && eventY < mFloatDrawable.getBounds().bottom) {
            return EDGE_LB;
        } else if ((mFloatDrawable.getBounds().right - mFloatDrawable
                .getBorderWidth()) <= eventX
                && eventX < mFloatDrawable.getBounds().right
                && (mFloatDrawable.getBounds().bottom - mFloatDrawable
                .getBorderHeight()) <= eventY
                && eventY < mFloatDrawable.getBounds().bottom) {
            return EDGE_RB;
        } else if (mFloatDrawable.getBounds().contains(eventX, eventY)) {
            return EDGE_MOVE_IN;
        }
        return EDGE_MOVE_OUT;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDrawable == null) {
            return;
        }

        if (mDrawable.getIntrinsicWidth() == 0
                || mDrawable.getIntrinsicHeight() == 0) {
            return;
        }


        configureBounds();
        // 在画布上花图片
        mDrawable.draw(canvas);
        canvas.save();
        // 在画布上画浮层FloatDrawable,Region.Op.DIFFERENCE是表示Rect交集的补集
        canvas.clipRect(mDrawableFloat, Region.Op.DIFFERENCE);
        // 在交集的补集上画上灰色用来区分
        canvas.drawColor(Color.parseColor("#a0000000"));
        canvas.restore();
        // 画浮层
        mFloatDrawable.draw(canvas);
    }

    protected void configureBounds() {
        // configureBounds在onDraw方法中调用
        // isFirst的目的是下面对mDrawableSrc和mDrawableFloat只初始化一次，
        // 之后的变化是根据touch事件来变化的，而不是每次执行重新对mDrawableSrc和mDrawableFloat进行设置
        if (isFrist) {
            oriRationWH = ((float) mDrawable.getIntrinsicWidth())
                    / ((float) mDrawable.getIntrinsicHeight());

            final float scale = mContext.getResources().getDisplayMetrics().density;
            int w = Math.min(getWidth(), (int) (mDrawable.getIntrinsicWidth()
                    * scale + 0.5f));
            int h = (int) (w / oriRationWH);

            int left = (getWidth() - w) / 2;
            int top = (getHeight() - h) / 2;
            int right = left + w;
            int bottom = top + h;

            mDrawableSrc.set(left, top, right, bottom);
            mDrawableDst.set(mDrawableSrc);

            int floatWidth = dipTopx(mContext, cropWidth);
            int floatHeight = dipTopx(mContext, cropHeight);

            /**限制裁剪框初始化在图像区域*/
            if (floatWidth > w) {
                floatWidth = w;
                floatHeight = cropHeight * floatWidth / cropWidth;
            }

            if (floatHeight > h) {
                floatHeight = h;
                floatWidth = cropWidth * floatHeight / cropHeight;
            }

            int floatLeft = (getWidth() - floatWidth) / 2;
            int floatTop = (getHeight() - floatHeight) / 2;
            mDrawableFloat.set(floatLeft, floatTop, floatLeft + floatWidth,
                    floatTop + floatHeight);

            isFrist = false;
        }

        mDrawable.setBounds(mDrawableDst);
        mFloatDrawable.setBounds(mDrawableFloat);
    }

    // 在up事件中调用了该方法，目的是检查是否把浮层拖出了图像区域
    protected void checkBounds() {
        int newLeft = mDrawableFloat.left;
        int newTop = mDrawableFloat.top;

        int l = mDrawableDst.left;
        int t = mDrawableDst.top;
        int r = mDrawableDst.right;
        int b = mDrawableDst.bottom;

        boolean isChange = false;
        if (mDrawableFloat.left < l) {
            newLeft = l;
            isChange = true;
        }

        if (mDrawableFloat.top < t) {
            newTop = t;
            isChange = true;
        }

        if (mDrawableFloat.right > r) {
            newLeft = r - mDrawableFloat.width();
            isChange = true;
        }

        if (mDrawableFloat.bottom > b) {
            newTop = b - mDrawableFloat.height();
            isChange = true;
        }

        mDrawableFloat.offsetTo(newLeft, newTop);
        if (isChange) {
            invalidate();
        }
    }

    // 进行图片的裁剪
    public Bitmap getCropImage() {

        int width = mDrawableFloat.width();
        int height = mDrawableFloat.height();


        Rect cropRect = new Rect(0, 0, width, height);



        try {
            final Matrix outputMatrix = new Matrix();//用于最图图片的精确缩放
            InputStream inputStream = new FileInputStream(file);
            final BitmapFactory.Options ops = new BitmapFactory.Options();
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(inputStream, false);

            final float scale = ((float) decoder.getWidth()) / mDrawableDst.width();
            cropRect.left = (int) ((mDrawableFloat.left - mDrawableDst.left) * scale);
            cropRect.top = (int) ((mDrawableFloat.top - mDrawableDst.top) * scale);
            cropRect.right = (int) ((mDrawableFloat.right - mDrawableDst.left) * scale);
            cropRect.bottom = (int) ((mDrawableFloat.bottom - mDrawableDst.top) * scale);

            final Bitmap source = decoder.decodeRegion(cropRect, ops);
            return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), outputMatrix, false);
        } catch (IOException e) {
            e.printStackTrace();
        }



        return null;
    }

    public int dipTopx(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
