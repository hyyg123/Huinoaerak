package com.example.yonggu.module2;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * Created by Yonggu on 2015-12-04.
 */
public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.overridePendingTransition(R.anim.fade, R.anim.fade_out);
        setContentView(R.layout.activity_splash);

        init();
    }

    private void init(){
        final Handler handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                finish();    // 액티비티 종료
            }
        };

        handler.sendEmptyMessageDelayed(0, 1500);
    }

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(R.anim.fade, R.anim.fade_out);
    }
}
