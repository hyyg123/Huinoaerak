package com.example.yonggu.module2;

import android.app.ActivityManager;
import android.app.Application;
import android.content.ContentProvider;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.LruCache;

import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Yonggu on 2015-12-01.
 */
public class MyApp extends Application {
    static public GoogleApiClient mGoogleApiClient;
    static public String personName;
    static public String personId;
    static public String selectedUniv = "연세대학교";
    static public String userImage = "null";
    static public int selectedBaseUnivPosition = 11,selectedUnivPosition, selectedPlayUnivPosition;
    static public boolean loginBool = false;

    final static public String urlStr = "http://hyyg123.cafe24.com/db_query.php";
    final static public String urlForImage = "http://hyyg123.cafe24.com";
    final static public String urlForPushNotification = urlForImage + "/GcmSender.php";

    // 서버에게 어떤 쿼리를 한 것인지를 정하는 상수
    public final static String CHECK_LOGIN_INFO     = "0";
    public final static String CHECK_UPDATE_NICK    = "1";
    public final static String SELECT_TABLE         = "2";
    public final static String INSERT_INTO_TAB      = "3";
    public final static String DELETE_TAB           = "4";
    public final static String UPDATE_TAB           = "5";
    public final static String INSERT_IMAGE_USERLIST= "6";
    public final static String DELETE_IMAGE_USERLIST= "7";
    public final static String INSERT_COMMENT       = "8";
    public final static String GET_COMMENTS         = "9";
    public final static String DELETE_CONIMAGE      = "10";
    public final static String GET_CONTACT_BY       = "11";
    public final static String GET_CON_CNT          = "12";

    public static DiskLruImageCache imageCache;
    private static final String subDir = "home_alone";
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10;
    private static final int IMAGE_QULAITY= 70;

    public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";
    public static final String REGISTRATION_COMPLETE = "registrationComplete";

    public static int COUNT_NOTIFICATION = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        imageCache = new DiskLruImageCache(getBaseContext(), subDir, DISK_CACHE_SIZE, Bitmap.CompressFormat.JPEG, IMAGE_QULAITY);
    }

    static public void init(){
        mGoogleApiClient = null;
        personId = null;
        personName = null;
        selectedUniv = null;
        userImage = "null";
        loginBool = false;
    }
}

/*
class UrlToBitmapTask extends AsyncTask<String, Void, Bitmap> {
    Bitmap result = null;
    @Override
    protected Bitmap doInBackground(String... params) {
        try {
            URL imageURL = new URL(params[0]);
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
        return result;
    }
}
*/
