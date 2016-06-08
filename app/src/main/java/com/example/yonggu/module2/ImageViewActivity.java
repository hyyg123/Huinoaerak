package com.example.yonggu.module2;

import android.app.Activity;
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

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by Yonggu on 16. 1. 26..
 */
public class ImageViewActivity extends Activity implements View.OnClickListener{
    private ImageView imageView;
    private PhotoViewAttacher mAttacher;
    private String imagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imageview);

        TextView backBtn = (TextView)findViewById(R.id.back);
        backBtn.setOnClickListener(this);

        imageView = (ImageView)findViewById(R.id.imageView);

        imagePath = getIntent().getStringExtra("imagePath");

        // 이미지뷰에 등록
        if(imagePath.contains("null") || imagePath.equals("")){
            imageView.setImageBitmap(null);
            imageView.setVisibility(View.GONE);
        }else{
            final String keyName_ConImage = "c" + imagePath.substring(
                    imagePath.lastIndexOf("/")+1, imagePath.lastIndexOf("."));

            new ImageViewTask().execute(keyName_ConImage);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.back :
                finish();
                break;
        }
    }

    private class ImageViewTask extends AsyncTask<String, Void, Bitmap>{
        @Override
        protected Bitmap doInBackground(String... params) {
            // 캐시에서 이미지를 가져옴
            Bitmap result = MyApp.imageCache.getBimap(params[0]);
            // 캐시에 이미지가 없을 경우
            if(result == null){
                try {
                    URL imageURL = new URL(imagePath);
                    HttpURLConnection urlConnection = (HttpURLConnection)imageURL.openConnection();
                    urlConnection.setDoInput(true);
                    urlConnection.connect();

                    BufferedInputStream bis = new BufferedInputStream
                            (urlConnection.getInputStream(), urlConnection.getContentLength());

                    result = BitmapFactory.decodeStream(bis);
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
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageBitmap(bitmap);
                imageView.setEnabled(true);
                mAttacher = new PhotoViewAttacher(imageView);
            }else{
                imageView.setVisibility(View.GONE);
                imageView.setImageBitmap(null);
                imageView.setEnabled(false);
            }
        }
    }
}
