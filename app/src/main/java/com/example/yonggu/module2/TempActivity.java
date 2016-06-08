package com.example.yonggu.module2;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by Yonggu on 15. 12. 20..
 */
public class TempActivity extends Activity {
    private final static String TAG = "TempActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        finish();
    }
}
