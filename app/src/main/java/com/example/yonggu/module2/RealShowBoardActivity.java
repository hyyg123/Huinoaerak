package com.example.yonggu.module2;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * Created by Yonggu on 16. 3. 11..
 */
public class RealShowBoardActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
        setContentView(R.layout.real_activity_show_board);
    }

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }
}
