package com.example.yonggu.module2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by Yonggu on 16. 1. 25..
 */
public class ProfileViewActivity extends Activity implements View.OnClickListener {
    private ImageView userImageView;
    private TextView nameTextView;
    private Button cntBtn;
    private TextView titleTextView;
    private String id;
    private String nickName;
    private String userImage;
    private Bitmap pBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TextView backBtn = (TextView)findViewById(R.id.back);
        backBtn.setOnClickListener(this);

        Intent intent = getIntent();
        id = intent.getStringExtra("id");
        nickName = intent.getStringExtra("nickName");
        userImage = intent.getStringExtra("userImage");
        cntBtn = (Button)findViewById(R.id.boardBtn);
        userImageView = (ImageView)findViewById(R.id.userImageView);
        nameTextView = (TextView)findViewById(R.id.nickNameTextView);
        titleTextView = (TextView)findViewById(R.id.categoryName);

        cntBtn.setOnClickListener(this);
        userImageView.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        nameTextView.setText(nickName);
        titleTextView.setText(nickName);
        cntBtn.setText(nickName + "님이 쓴 글 보기");

        // 이미지 설정을 하지 않은 경우
        if(userImage.contains("null") || userImage.equals("")){
            userImageView.setImageResource(R.drawable.user);
            // 이미지 설정을 한 경우
        }else{
            final String keyName_UserImage = "c" + userImage.substring(
                    userImage.lastIndexOf("/")+1, userImage.lastIndexOf("."));
            new GetImageTask().execute(keyName_UserImage);
        }

        new GetMyBoardCountTask().execute();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(pBitmap != null) {
            //pBitmap.recycle();
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()){
            case R.id.boardBtn :
                intent = new Intent(this, RealShowBoardActivity.class);
                intent.putExtra("id", id);
                startActivity(intent);
                break;
            case R.id.userImageView :
                intent = new Intent(this, ImageViewActivity.class);
                intent.putExtra("imagePath", userImage);
                startActivity(intent);
                break;
            case R.id.back :
                finish();
                break;
        }

    }

    private class GetMyBoardCountTask extends AsyncTask<String ,Void, String>{
        @Override
        protected String doInBackground(String... params) {
            HashMap<String, String> data = new HashMap<>();
            data.put("sel", MyApp.GET_CON_CNT);
            data.put("id", id);

            String response = new RequestHandler().sendPostRequest(MyApp.urlStr, data);

            if(response.contains("{")){
                return response.substring(response.lastIndexOf("{")+1, response.lastIndexOf("}"));
            }else{
                return "";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if(!s.equals("")){
                cntBtn.setText(cntBtn.getText() + "(" + s + ")");
            }
        }
    }

    private class GetImageTask extends AsyncTask<String, Void, Bitmap>{
        @Override
        protected Bitmap doInBackground(String... params) {
            // 캐시에서 이미지를 가져옴
            Bitmap result = MyApp.imageCache.getBimap(params[0]);
            // 캐시에 이미지가 없을 경우
            if(result == null){
                try {
                    URL imageURL = new URL(userImage);
                    HttpURLConnection urlConnection = (HttpURLConnection)imageURL.openConnection();
                    urlConnection.setDoInput(true);
                    urlConnection.connect();

                    BufferedInputStream bis = new BufferedInputStream
                            (urlConnection.getInputStream(), urlConnection.getContentLength());

                    result = BitmapFactory.decodeStream(bis);
                    Log.i("MyC", "URL Downloaded!");
                    bis.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
                if(result != null){
                    MyApp.imageCache.put(params[0], result);

                    // 전에 캐시에 저장되어 있는 이미지 지우기
                    String temp_str = params[0].substring(
                            1, params[0].lastIndexOf("_"));
                    int user_cnt = Integer.parseInt(temp_str) - 1;

                    temp_str = params[0].replaceFirst(temp_str, Integer.toString(user_cnt));
                    boolean re = MyApp.imageCache.reomveKey(temp_str);
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(bitmap != null){
                userImageView.setImageBitmap(bitmap);
                pBitmap = bitmap;
            }else {
                userImageView.setImageResource(R.drawable.user);
            }
        }
    };

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }
}
