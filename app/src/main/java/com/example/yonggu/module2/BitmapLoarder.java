package com.example.yonggu.module2;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Yonggu on 16. 2. 20..
 */
public class BitmapLoarder {
    private final static String TAG = "BitmapLoarder";

    public Bitmap placeHolderBitmap;
    private int width, height;
    private boolean isScaled = false;

    private Bitmap bitmap;

    public void loadBitmap(Resources res, String key, String path, ImageView imageView){
        //Log.d(TAG, "------------------------------------------------------------------");
        //Log.d(TAG, key + " : start , image view = " + imageView.getId());

        if(cancelPotentialWork(key, imageView)){
            //Log.d(TAG, key + " : here is in if(cancelPotential)");
            final BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask(imageView);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(
                    res, placeHolderBitmap, bitmapWorkerTask);
            imageView.setImageDrawable(asyncDrawable);
            bitmapWorkerTask.execute(key, path);
        }
    }

    public static boolean cancelPotentialWork(String key, ImageView imageView){
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if(bitmapWorkerTask != null){
            final String bitmapKey = bitmapWorkerTask.key;
            if(bitmapKey != key){
                //Cancel previous task
                bitmapWorkerTask.cancel(true);
                //Log.d(TAG, key + " : Cancel previous task" );
            }else{
                //Log.d(TAG, key + " : No cancel previous task" );
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView){
        if(imageView != null){
            final Drawable drawable = imageView.getDrawable();
            if(drawable instanceof AsyncDrawable){
                final AsyncDrawable asyncDrawable = (AsyncDrawable)drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTasReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask){
            super(res, bitmap);
            bitmapWorkerTasReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask(){
            return bitmapWorkerTasReference.get();
        }
    }

    // params[0] = keyName, [1] = imagePath
    private class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private String key;

        public BitmapWorkerTask(ImageView imageView){
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            key = params[0];

            bitmap = MyApp.imageCache.getBimap(params[0]);

            if(bitmap == null){
                try {
                    bitmap = downloadImage(params[1]);
                    //Log.d(TAG, key + " : download image");
                    if(isScaled){
                        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
                    }
                } catch (IOException e){
                    e.printStackTrace();
                }
                if(bitmap != null){
                    key = updateCacheKey(bitmap, params[0]);
                }
            }else{
                //Log.d(TAG, key + " : image in cache");
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(isCancelled()){
                //Log.d(TAG, key + " : isCancelled = true");
                bitmap = null;
            }

            if(imageViewReference == null){
                //Log.d(TAG, key + " : imageViewReference= null" );
            }
            if(bitmap == null){
                //Log.d(TAG, key + " : bitmap = null" );
            }

            if(imageViewReference != null && bitmap != null){
                final ImageView imageView = imageViewReference.get();
                if(imageView != null){
                    //Log.d(TAG, key + " : set image success" );
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    private Bitmap downloadImage (String imagePath) throws IOException{
        Bitmap result = null;

        URL imageURL = new URL(imagePath);
        HttpURLConnection urlConnection = (HttpURLConnection)imageURL.openConnection();
        urlConnection.setDoInput(true);
        urlConnection.connect();

        BufferedInputStream bis = new BufferedInputStream
                (urlConnection.getInputStream(), urlConnection.getContentLength());
        result = BitmapFactory.decodeStream(bis);

        bis.close();

        return result;
    }

    private String updateCacheKey(Bitmap bitmap, String keyName){
        MyApp.imageCache.put(keyName, bitmap);
        String new_keyName = keyName.substring(
                1, keyName.lastIndexOf("_"));
        int user_cnt = Integer.parseInt(new_keyName) - 1;

        new_keyName = keyName.replaceFirst(new_keyName, Integer.toString(user_cnt));

        MyApp.imageCache.reomveKey(new_keyName);

        return new_keyName;
    }

    public void recycleBitmap(){
        if(bitmap != null)
            bitmap.recycle();
        else
            Log.d(TAG, "bitmap not null");
    }

    public void isScaled(boolean isScaled){
        this.isScaled = isScaled;
    }

    public void setWidth(int width){
        this.width= width;
    }
    public void setHeight(int height){
        this.height = height;
    }
}
