package com.example.yonggu.module2;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Yonggu on 16. 1. 19..
 */
public class CommentCursorAdapter extends CursorAdapter{
    final static private String TAG = "CommentCursorAdapter";

    private Animation animation;
    public BitmapLoarder bitmapLoarder;

    public CommentCursorAdapter(Context context, Cursor c){
        super(context, c);
        animation = AnimationUtils.loadAnimation(context, R.anim.short_fade);
        bitmapLoarder = new BitmapLoarder();
        bitmapLoarder.placeHolderBitmap =
                BitmapFactory.decodeResource(context.getResources(), R.drawable.celebration_128_w);
        bitmapLoarder.isScaled(true);
        bitmapLoarder.setWidth(100);
        bitmapLoarder.setHeight(100);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ViewHolder viewHolder = new ViewHolder();

        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.comment_item, parent, false);

        viewHolder.commentImageView = (ImageView)v.findViewById(R.id.commentImageView);
        viewHolder.nickNameTextView = (TextView)v.findViewById(R.id.nickNameTextView);
        viewHolder.dateTextView = (TextView)v.findViewById(R.id.dateTextView);
        viewHolder.commentTextView = (TextView)v.findViewById(R.id.commentTextView);
        viewHolder.commentTextView = (TextView)v.findViewById(R.id.commentTextView);

        v.setTag(viewHolder);
        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        /*final ImageView commentImageView = (ImageView)view.findViewById(R.id.commentImageView);
        final TextView nickNameTextView = (TextView)view.findViewById(R.id.nickNameTextView);
        final TextView dateTextView = (TextView)view.findViewById(R.id.dateTextView);
        final TextView commentTextView = (TextView)view.findViewById(R.id.commentTextView);*/

        if (cursor==null) return;

        ViewHolder viewHolder = (ViewHolder)view.getTag();

        viewHolder.nickNameTextView.setText(cursor.getString(cursor.getColumnIndex("nickName")));
        viewHolder.dateTextView.setText(cursor.getString(cursor.getColumnIndex("date")));
        viewHolder.commentTextView.setText(cursor.getString(cursor.getColumnIndex("content")));

        final String imagePath = cursor.getString(cursor.getColumnIndex("userImage"));

        //Log.i("TEST", "CommentCursrAdapter nickName : " + nickNameTextView.getText().toString() );
        //Log.i("TEST", "CommentCursrAdapter content : " + commentTextView.getText().toString() );

        // 내용 이미지를 설정하지 않은 경우
        if(imagePath.equals("") || imagePath.contains("null")){
            viewHolder.commentImageView.setImageResource(R.drawable.user);
            // 내용 이미지를 설정한 경우
        }else{
            final String keyName_UserImage = "c" + imagePath.substring(
                    imagePath.lastIndexOf("/")+1, imagePath.lastIndexOf("."));
            //Log.i(TAG, "key name : " + keyName_UserImage);

            bitmapLoarder.loadBitmap(context.getResources(),
                    keyName_UserImage, imagePath, viewHolder.commentImageView);
        }


        //view.startAnimation(animation);
    }
    private class ViewHolder{
        ImageView commentImageView;
        TextView nickNameTextView;
        TextView dateTextView;
        TextView commentTextView;
    }
}

/*new AsyncTask<String, Void, Bitmap>(){
                @Override
                protected Bitmap doInBackground(String... params) {
                    // 캐시에서 이미지를 가져옴
                    Bitmap result = MyApp.imageCache.getBimap(keyName_UserImage);
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

                            //Log.i("MyC" , "URL Downloaded!");
                            bis.close();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        if(result != null){
                            MyApp.imageCache.put(keyName_UserImage, result);

                            // 전에 캐시에 저장되어 있는 이미지 지우기
                            String temp_str = keyName_UserImage.substring(
                                    1, keyName_UserImage.lastIndexOf("_"));
                            Log.i(TAG, "전에 저장되어 있던 캐시 키 네임 : " + temp_str);
                            int user_cnt = Integer.parseInt(temp_str) - 1;

                            temp_str = keyName_UserImage.replaceFirst(temp_str, Integer.toString(user_cnt));
                            boolean re = MyApp.imageCache.reomveKey(temp_str);
                            //Log.i("T", "IS REMOVED ? " + re);
                        }
                    }
                    return result;
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    if(bitmap != null){
                        commentImageView.setImageBitmap(bitmap);
                    }else {
                        commentImageView.setImageResource(R.drawable.user);
                    }
                }
            }.execute();*/
