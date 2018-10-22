package com.zippy.zippykiosk;

/**
 * Created by KB on 8/03/15.
 * Copyright 2015 Zippy.com.au.
 */
class PasscodeChecker {
    // unlock coded. Pattern of taps at bottom of screen
    private static final int[] PASSCODE = new int[]{1,1,1,1,0,1,1,1,0,1,1,0,1};
    private int mIndex=0;
    private long mPrevTime= System.currentTimeMillis()-2000;

    boolean addHit() {
        long time = System.currentTimeMillis();
        //Log.d("Tap " + (time-mPrevTime));
        if(time-mPrevTime>1500 || time-mPrevTime<0) {
            // Reset if pause of 1.5secs or more
            mIndex=1;
            mPrevTime=time;
            return(false);
        }
        if(time-mPrevTime>500) {
            // Gap
            if(PASSCODE[mIndex]==0) {
                mIndex++;
            }else {
                // Reset
                mIndex=1;
                mPrevTime=time;
                return(false);
            }

        }
        mPrevTime=time;

        // Short tap
        if(mIndex<PASSCODE.length && PASSCODE[mIndex]==1) {
            mIndex++;
        }else {
            mIndex=1;
        }
        if(mIndex==PASSCODE.length) {
            mIndex=0;
            return(true);
        }
        return false;
    }


}
