package com.amsu.amsuinsolebletest.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;

import com.amsu.amsuinsolebletest.R;
import com.amsu.amsuinsolebletest.util.Constant;


/**
 * Created by HP on 2017/1/4.
 */

public class DashboardView extends View {

    private static final String TAG = "DashboardView";
    private Paint mPaint;
    private Paint mTextPaint;
    private Paint mPointPaint;
    private float mBigRadius;
    private float mSmallRadius;
    private float mPointRadius;
    private float mLargeLength ;
    private float mSmallLength ;
    private float centerX;
    private float centerY;
    private float offectDegree;
    private Canvas mCanvas;
    private float currentAge;

    public DashboardView(Context context) {
        super(context);
        init(context,null);
    }

    public DashboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs);
    }

    public DashboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
    }

    private void init(Context context,AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DashboardView);
        float big_circle_radius = typedArray.getDimension(R.styleable.DashboardView_big_circle_radius, 0);
        float small_circle_radius = typedArray.getDimension(R.styleable.DashboardView_small_circle_radius, 0);
        float small_point_radius = typedArray.getDimension(R.styleable.DashboardView_small_point_radius, 0);
        float long_dials = typedArray.getDimension(R.styleable.DashboardView_long_dials, 0);
        float short_dials = typedArray.getDimension(R.styleable.DashboardView_short_dials, 0);
        float width_dials = typedArray.getDimension(R.styleable.DashboardView_width_dials,0);
        int text_color = typedArray.getColor(R.styleable.DashboardView_text_color,Color.WHITE);
        float text_size = typedArray.getDimension(R.styleable.DashboardView_text_size,0);

        mPaint = new Paint(); //刻度盘
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(width_dials);

        mTextPaint = new Paint();  //文字
        mTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mTextPaint.setColor(text_color);
        mTextPaint.setAntiAlias(true);
        //mTextPaint.setStrokeWidth(2);
        mTextPaint.setTextSize(text_size);

        mPointPaint = new Paint();  //小红点
        mPaint.setStyle(Paint.Style.STROKE);
        mPointPaint.setColor(Color.RED);
        //mPointPaint.setMaskFilter(new BlurMaskFilter(20, BlurMaskFilter.Blur.SOLID));

        mBigRadius = big_circle_radius;
        mLargeLength = long_dials;
        mSmallLength = short_dials;
        mSmallRadius = small_circle_radius;
        mPointRadius = small_point_radius;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w/2;
        centerY = w/2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mCanvas = canvas;


        float start_x,start_y;
        float end_x,end_y;
        //偏移的角度，为 Math.pi/3+2*Math.pi/3
        offectDegree = (float) (15*Math.PI/18);

        for(int i=0;i<=80;++i){
            start_x= mBigRadius *(float)Math.cos(Math.PI/180 * i * 3 + offectDegree);
            start_y= mBigRadius *(float)Math.sin(Math.PI/180 * i * 3 + offectDegree);

            start_x+=centerX;
            start_y+=centerY;

            if(i%10==0){
                end_x = start_x+mLargeLength*(float)Math.cos(Math.PI / 180 * i * 3 + offectDegree);
                end_y = start_y+mLargeLength*(float)Math.sin(Math.PI / 180 * i * 3 + offectDegree);

                //画数字
                String text = String.valueOf(i+10);
                float text_x = start_x - 20*(float)Math.cos(Math.PI / 180 * i * 3 + offectDegree) - mTextPaint.measureText(text)/2;
                float text_y = start_y - 20*(float)Math.sin(Math.PI / 180 * i * 3 + offectDegree) + mTextPaint.measureText(text)/2;
                mCanvas.drawText(text,text_x,text_y,mTextPaint);
            }else{
                end_x = start_x+mSmallLength*(float)Math.cos(Math.PI/180 * i * 3 + offectDegree);
                end_y = start_y+mSmallLength*(float)Math.sin(Math.PI/180 * i * 3 + offectDegree);
            }

            mCanvas.drawLine(start_x, start_y, end_x, end_y, mPaint);


        }
        RectF rectF = new RectF(centerX-mSmallRadius,centerY-mSmallRadius,centerX+mSmallRadius,centerY+mSmallRadius);
        mCanvas.drawArc(rectF,150,240,false,mPaint);  //画内部小圆

        float point_x = mSmallRadius *(float)Math.cos(Math.PI/180 * currentAge * 3 + offectDegree) + centerX;
        float point_y = mSmallRadius *(float)Math.sin(Math.PI/180 * currentAge * 3 + offectDegree) + centerY;

       //canvas.drawPoint(point_x,point_y,mPointPaint);
        canvas.drawCircle(point_x,point_y,mPointRadius,mPointPaint);  //画红点



    }

    public void setAgeData(int age){
        currentAge = 0;
        //加速器
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(currentAge, age);
        valueAnimator.setDuration(Constant.AnimatorDuration);
        valueAnimator.setInterpolator(new Interpolator() {
            @Override
            public float getInterpolation(float v) {
                return 1-(1-v)*(1-v)*(1-v);
            }
        });
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                currentAge = (float) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });
        valueAnimator.start();

    }

    interface MyOnDataChangeListener{
        void onDataChange();
    }

    MyOnDataChangeListener myOnDataChangeListener;
}
