package com.example.yonggu.module2;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

/**
 * Created by Yonggu on 2015-12-02.
 */
public class MyCursorAdapter extends CursorAdapter {
    private static final String TAG = "MyCursorAdapter";

    BitmapLoarder bitmapLoarder1, bitmapLoarder2;
    private Animation animation;

    private Boolean isCreatedView;
    private Context context;

    private SparseArray<WeakReference<View>> viewArray;
    private static final int ARRAY_SIZE = 256;

    public MyCursorAdapter(Context context, Cursor c){
        super(context, c);
        this.context = context;

        bitmapLoarder1 = new BitmapLoarder();
        bitmapLoarder1.placeHolderBitmap =
                BitmapFactory.decodeResource(context.getResources(), R.drawable.celebration_128_w);
        bitmapLoarder2 = new BitmapLoarder();
        bitmapLoarder2.placeHolderBitmap =
                BitmapFactory.decodeResource(context.getResources(), R.drawable.celebration_128_w);
        animation = AnimationUtils.loadAnimation(context, R.anim.short_fade);

        viewArray = new SparseArray<WeakReference<View>>(ARRAY_SIZE);
    }

    // bindView에서의 view는 newView에서 반환한 view이다.
    //


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //Log.d(TAG, "getView call!");
        return super.getView(position, convertView, parent);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        //Log.d(TAG, "bindView call!");
        ViewHolder viewHolder = (ViewHolder)view.getTag();

        /*if(viewArray != null && viewArray.get(cursor.getPosition()) != null){
            view = viewArray.get(cursor.getPosition()).get();
            ViewHolder testHolder = (ViewHolder)view.getTag();
            Log.d(TAG, "t = " + testHolder.contents.getText().toString());
            Log.d(TAG, "viewArray get position :" + cursor.getPosition());
            if(view != null){
                return ;
            }
        }*/

        viewHolder.commentCntTextView.setText(cursor.getString(cursor.getColumnIndex("comment_cnt")));
        viewHolder.profileNickName.setText(cursor.getString(cursor.getColumnIndex("name")));
        viewHolder.currentTime.setText(cursor.getString(cursor.getColumnIndex("time")));
        viewHolder.contents.setText(cursor.getString(cursor.getColumnIndex("contents")));

        // uri를 통해 비트맵 이미지로 받아오기
        final String userImagePath = cursor.getString(cursor.getColumnIndex("userImage"));
        final String conImagePath = cursor.getString(cursor.getColumnIndex("conImage"));

        // // 프로필 사진과 글 사진을 캐시 또는 URL을 통해서 불러오기
        // 이미지 설정을 하지 않은 경우

        if(userImagePath.contains("null") || userImagePath.equals("")){
            viewHolder.profileImage.setImageResource(R.drawable.user);
        // 이미지 설정을 한 경우
        }else{
            final String keyName_UserImage = "c" + userImagePath.substring(
                    userImagePath.lastIndexOf("/")+1, userImagePath.lastIndexOf("."));
            bitmapLoarder1.loadBitmap(context.getResources(),
                    keyName_UserImage, userImagePath, viewHolder.profileImage);
        }

        // 내용 이미지를 설정하지 않은 경우
        if (conImagePath.contains("null") || conImagePath.equals("")) {
            viewHolder.contentImage.setImageBitmap(null);
            viewHolder.contentImage.setVisibility(View.GONE);
            // 내용 이미지를 설정한 경우
        } else {
            final String keyName_ConImage = "c" + conImagePath.substring(
                    conImagePath.lastIndexOf("/") + 1, conImagePath.lastIndexOf("."));
            bitmapLoarder2.loadBitmap(context.getResources(),
                    keyName_ConImage, conImagePath, viewHolder.contentImage);
        }

        Log.d(TAG, "viewArray put position :" + cursor.getPosition());
        //viewArray.put(cursor.getPosition(), new WeakReference<View>(view));

        // viewHolder.profileImage.startAnimation(animation);
        // viewHolder.contentImage.startAnimation(animation);
    }

    // item 등록
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        //Log.d(TAG, "newView call!");
        ViewHolder viewHolder = new ViewHolder();
        LayoutInflater inflater = LayoutInflater.from(context);

        View view;
        /*if(viewArray != null && viewArray.get(cursor.getPosition()) != null){
            view = viewArray.get(cursor.getPosition()).get();
            Log.d(TAG, "viewArray.get");
            if(view != null){
                return view;
            }
        }*/

        /*Log.d(TAG, "newView cursor position = " + cursor.getPosition());
        Log.d(TAG, "viewArray cursor position = " + viewArray.get(cursor.getPosition()));
        Log.d(TAG, "viewArray : " + viewArray.get(0));*/

        view = inflater.inflate(R.layout.listview_item, viewGroup, false);

        viewHolder.profileImage = (ImageView)view.findViewById(R.id.profileImage);
        viewHolder.contentImage = (ImageView)view.findViewById(R.id.uploadImage);
        viewHolder.profileNickName = (TextView)view.findViewById(R.id.nickName);
        viewHolder.currentTime = (TextView)view.findViewById(R.id.currentTime);
        viewHolder.contents = (TextView)view.findViewById(R.id.contents);
        viewHolder.commentCntTextView = (TextView)view.findViewById(R.id.commentCntTextView);

        //Log.d(TAG, "new view");
        view.setTag(viewHolder);
        return view;
    }

    private class ViewHolder{
        ImageView profileImage, contentImage;
        TextView profileNickName, currentTime, contents, commentCntTextView;
    }
}



/*new AsyncTask<Void, Void, Bitmap>(){
                @Override
                protected Bitmap doInBackground(Void... params) {
                    // 캐시에서 이미지를 가져옴
                    Bitmap result = MyApp.imageCache.getBimap(keyName_ConImage);
                    // 캐시에 이미지가 없을 경우
                    if(result == null){
                        try {
                            URL imageURL = new URL(conImagePath);
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
                            MyApp.imageCache.put(keyName_ConImage, result);

                            // 전에 캐시에 저장되어 있는 이미지 지우기
                            String temp_str = keyName_ConImage.substring(
                                    1, keyName_ConImage.lastIndexOf("_"));
                            int user_cnt = Integer.parseInt(temp_str) - 1;

                            temp_str = keyName_ConImage.replaceFirst(temp_str, Integer.toString(user_cnt));
                            boolean re = MyApp.imageCache.reomveKey(temp_str);
                        }
                    }
                    return result;
                }
                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    if(bitmap != null){
                        contentImage.setVisibility(View.VISIBLE);
                        contentImage.setImageBitmap(bitmap);
                    }else{
                        contentImage.setVisibility(View.GONE);
                        contentImage.setImageBitmap(null);
                    }
                }
            }.execute();*/