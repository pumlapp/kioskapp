package com.zippy.zippykiosk;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;

/**
 * Created by kimb on 13/05/2015.
 */
public class AppHelpActivity  extends BaseActivity {
    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_help);

        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private final Runnable mTimeout = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };
}
