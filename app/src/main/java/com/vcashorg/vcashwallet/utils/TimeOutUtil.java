package com.vcashorg.vcashwallet.utils;

import com.vcashorg.vcashwallet.R;

public class TimeOutUtil {

    public static final int TIME_OUT_NEVER = 0;
    public static final int TIME_OUT_30SEC = 1;
    public static final int TIME_OUT_1MIN = 2;
    public static final int TIME_OUT_3MIN = 3;

    private static long TIMEOUT_DELAY;

    private static long lastTime = 0L;

    private int type;

    private static TimeOutUtil instance = null;

    private TimeOutUtil(){
        initTimOut();
    }

    public static TimeOutUtil getInstance() {

        if(instance == null) {
            instance = new TimeOutUtil();
        }

        return instance;
    }

    public void updateLastTime(){
        lastTime = System.currentTimeMillis();
    }

    public boolean isTimeOut(){
        if (type != TIME_OUT_NEVER){
            return (System.currentTimeMillis() - lastTime) > TIMEOUT_DELAY;
        }else {
            return false;
        }

    }

    public void updateTimeOutType(int type){
        this.type = type;
        SPUtil.getInstance(UIUtils.getContext()).setValue(SPUtil.TIME_OUT,type);
        updateLastTime();
        if(type == TIME_OUT_30SEC){
            TIMEOUT_DELAY = 1000 * 30;
        }else if(type == TIME_OUT_1MIN){
            TIMEOUT_DELAY = 1000 * 60;
        }else if(type == TIME_OUT_3MIN){
            TIMEOUT_DELAY = 1000 * 60 * 3;
        }
    }

    public int getTimeOutType(){
        return type;
    }

    public String getTimeOutString(){
        switch (type){
            case TIME_OUT_NEVER:
                return UIUtils.getString(R.string.never);
            case TIME_OUT_30SEC:
                return UIUtils.getString(R.string.after_30_seconds);
            case TIME_OUT_1MIN:
                return UIUtils.getString(R.string.after_1_minute);
            case TIME_OUT_3MIN:
                return UIUtils.getString(R.string.after_3_minute);
        }
        return "";
    }

    private void initTimOut(){
        type = SPUtil.getInstance(UIUtils.getContext()).getValue(SPUtil.TIME_OUT,1);
        if(type == TIME_OUT_30SEC){
            TIMEOUT_DELAY = 1000 * 30;
        }else if(type == TIME_OUT_1MIN){
            TIMEOUT_DELAY = 1000 * 60;
        }else if(type == TIME_OUT_3MIN){
            TIMEOUT_DELAY = 1000 * 60 * 3;
        }
        updateLastTime();
    }
}
