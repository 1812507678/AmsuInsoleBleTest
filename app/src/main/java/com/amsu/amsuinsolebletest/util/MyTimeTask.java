package com.amsu.amsuinsolebletest.util;

import android.app.Activity;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by HP on 2017/3/16.
 */

public class MyTimeTask {
    private Timer mTimer;
    private long mTimeSpan;
    private TimerTask mTimerTask;
    private boolean mIsTimeerRunning;

    public MyTimeTask(long mTimeSpan, TimerTask mTimerTask) {
        this.mTimeSpan = mTimeSpan;
        this.mTimerTask = mTimerTask;
    }

    public void startTime(){
        if (mTimer==null){
            mTimer = new Timer();
            mTimer.schedule(mTimerTask,mTimeSpan,mTimeSpan);
        }
        mIsTimeerRunning = true;
    }

    public void stopTime(){
        if (!mIsTimeerRunning && mTimer!=null && mTimerTask!=null){
            /*mTimerTask.cancel();
            mTimerTask=null;*/
            mTimer.cancel();
            mTimer = null;
        }
    }

    //时间计时器，timeSpan为时间间隔，会一直执行。返回Date类型的日期
    public static void startTimeRiseTimerTask(final Activity activity, final long timeSpan, final OnTimeChangeAtScendListener onTimeChangeAtScendListener){
        final Timer timer = new Timer();
        TimerTask tt=new TimerTask() {
            boolean mIsFirstStart = true;
            Date nowDate;
            @Override
            public void run() {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mIsFirstStart){
                            mIsFirstStart = false;
                            nowDate = new Date();
                            nowDate.setHours(0);
                            nowDate.setMinutes(0);
                            nowDate.setSeconds(0);
                        }
                        nowDate = new Date(nowDate.getTime()+timeSpan);
                        onTimeChangeAtScendListener.onTimeChange(nowDate);
                    }
                });
            }
        };
        timer.schedule(tt, timeSpan,timeSpan);
    }

    //时间计时器，timeSpan为时间间隔，会一直执行。返回Date类型的日期
    public static void startTimeRiseTimerTask(final long timeSpan, final OnTimeChangeAtScendListener onTimeChangeAtScendListener){
        final Timer timer = new Timer();
        TimerTask tt=new TimerTask() {
            boolean mIsFirstStart = true;
            Date nowDate;
            @Override
            public void run() {
                if (mIsFirstStart){
                    mIsFirstStart = false;
                    nowDate = new Date();
                    nowDate.setHours(0);
                    nowDate.setMinutes(0);
                    nowDate.setSeconds(0);
                }
                nowDate = new Date(nowDate.getTime()+timeSpan);
                onTimeChangeAtScendListener.onTimeChange(nowDate);
            }
        };
        timer.schedule(tt, timeSpan,timeSpan);
    }

    public interface OnTimeChangeAtScendListener{
        void onTimeChange(Date date);
    }

    //定时器，当总时间到时会触发
    public static void startCountDownTimerTask(long mAllTime, final OnTimeOutListener onTimeOutListener){
        final Timer timer = new Timer();
        TimerTask tt=new TimerTask() {
            @Override
            public void run() {
                System.out.println("到点啦！");
                onTimeOutListener.onTomeOut();
                timer.cancel();
            }
        };
        timer.schedule(tt, mAllTime);
    }

    public interface OnTimeOutListener{
        void onTomeOut();
    }

}
