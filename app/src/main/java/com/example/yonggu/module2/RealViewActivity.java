package com.example.yonggu.module2;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;

/**
 * Created by Yonggu on 16. 3. 11..
 */
public class RealViewActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.real_activity_view);
    }

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }
}
