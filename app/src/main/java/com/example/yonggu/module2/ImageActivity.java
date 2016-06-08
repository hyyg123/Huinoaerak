package com.example.yonggu.module2;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by Yonggu on 15. 12. 18..
 */

// 갤러리에서 이미지를 선택 후 호출되는 엑티티비
    //
public class ImageActivity extends Activity implements View.OnClickListener{
    private ImageView imageView;
    private Button upBtn;
    private String imagePath;
    public static Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        TextView backBtn = (TextView)findViewById(R.id.back);
        backBtn.setOnClickListener(this);

        imageView = (ImageView)findViewById(R.id.imageView);
        upBtn = (Button)findViewById(R.id.upBtn);
        upBtn.setOnClickListener(this);

        imagePath = getIntent().getStringExtra("imagePath");

        imageView.setImageBitmap(bitmap);
        bitmap =  null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(bitmap != null){
            //bitmap.recycle();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.upBtn :
                new ImageTask().execute();
                break;
            case R.id.back :
                finish();
                break;
        }
    }

    private class ImageTask extends AsyncTask<Bitmap, Void, String> {
        ProgressDialog loading ;
        RequestHandler rh = new RequestHandler();
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loading = ProgressDialog.show(ImageActivity.this, "Uploading Image", "Please wait...", true , true);
        }
        @Override
        protected void onPostExecute(String aVoid) {
            super.onPostExecute(aVoid);
            loading.dismiss();
            Toast.makeText(ImageActivity.this, "프로필 사진이 변경되었습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
         @Override
         protected String doInBackground(Bitmap... params) {
             HashMap<String, String> data = new HashMap<>();
             data.put("sel", MyApp.INSERT_IMAGE_USERLIST);
             data.put("id", MyApp.personId);

             String result = rh.uploadFile(MyApp.urlStr, imagePath, data);

             // 파일 업로드 성공 시
             // null 은 또 왜 출력되는 거임.ㅡ,,ㅡ;
             if(result.contains("{")) {
                 MyApp.userImage = result.substring(result.indexOf("{")+1, result.indexOf("}"));
                  // 내부 디비(쿠키) 저장
                 new DatabaseConnector(ImageActivity.this).updateLoginInfo(MyApp.personName, MyApp.selectedUniv, MyApp.userImage);
                }
             return "";
         }
    }
}
