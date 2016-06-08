package com.example.yonggu.module2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.FileOutputStream;

/*
    Fragment 관리

    각 프래그먼트에서 이벤트가 발생하면
    각 프래그먼트의 인터페이스에서 구현한 함수가 실행(그 함수들은 메인에서 구현됨)
    ?? 맞나
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String tableKey = "table";  // 테이블 이름을 지정해주기 위한 키

    public static MyListFragment fragment;

    private SlidingTabLayout mSlidingTabLayout;
    private ViewPager mViewPager;
    private MyListFragment list[] = new MyListFragment[4];
    //private MyListFragment2 list2[] = new MyListFragment2[1];
    private MyTabFragment mytab = new MyTabFragment();
    public static FloatingActionButton fab, new_fab;
    private int selectedPagePosition;
    private TextView tv;

    public static ActionBar actionBar;
    private BroadcastReceiver mRegistrationBroadcastReceiver;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // push notification에 의한 실행인가
        Log.d(TAG, "" + getIntent().getBooleanExtra("isByNotification", false));
        MyApp.COUNT_NOTIFICATION = 1;
        if(getIntent().getBooleanExtra("isByNotification", false)){
            Intent intent = new Intent(this, RealViewActivity.class);
            String board_num = getIntent().getStringExtra("board_num");
            intent.putExtra("id", Long.parseLong(board_num));
            intent.putExtra("tableName", getIntent().getStringExtra("tab_type"));
            startActivity(intent);
        }

        // 스플래쉬
        startActivity(new Intent(this, SplashActivity.class));

        super.onCreate(savedInstanceState);
        this.overridePendingTransition(R.anim.fade, R.anim.fade_out);
        setContentView(R.layout.activity_main);

        sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        // 로그인 불러오기
        final DatabaseConnector dc = new DatabaseConnector(this);
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                dc.open();
                Cursor cursor = dc.getLoginInfo();
                if(cursor != null && cursor.getCount() != 0){
                    cursor.moveToFirst();
                    MyApp.personId = cursor.getString(cursor.getColumnIndex("userId"));
                    MyApp.personName = cursor.getString(cursor.getColumnIndex("nickName"));
                    MyApp.selectedUniv = cursor.getString(cursor.getColumnIndex("selectedUni"));
                    String tempStr = cursor.getString(cursor.getColumnIndex("userImage"));

                    if(tempStr.contains("uploads"))
                        MyApp.userImage = tempStr.substring(tempStr.lastIndexOf("uploads"));
                    else
                        MyApp.userImage = "null";

                    MyApp.loginBool = true;
                    cursor.close();
                    dc.close();
                }else{
                    MyApp.loginBool = false;
                }
                return null;
            }
        }.execute();

        mViewPager = (ViewPager)findViewById(R.id.viewpager);
        mViewPager.setAdapter(new MyPagerAdapter(this, mViewPager));
        mViewPager.addOnPageChangeListener(onPageChangeListener);

        mSlidingTabLayout = (SlidingTabLayout)findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setOnPageChangeListener();

        fab = (FloatingActionButton)findViewById(R.id.fab);
        fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.barColor)));
        fab.setOnClickListener(clickFab);
        fab.show();


        new_fab = (FloatingActionButton)findViewById(R.id.new_fab);
        new_fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.boardColor)));
        //new_fab.setY(-200);
        new_fab.setEnabled(false);
        new_fab.hide();

        Bundle arguments[] = new Bundle[4];
        for(int i=0;i<arguments.length;i++) {
            arguments[i] = new Bundle();
            arguments[i].putString(tableKey, getResources().getStringArray(R.array.tableNameArray)[i]);
        }
        for(int i=0;i<list.length;i++){
            list[i] = new MyListFragment();
            list[i].setArguments(arguments[i]);
        }

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(MyApp.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    Log.d(TAG, "sentToken is true");
                    // 토큰이 등록되어 있을때
                } else {
                    // 토큰이 등록되어 있지 않을때
                    Log.d(TAG, "sentToken is false");
                }
            }
        };
    }

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(R.anim.fade, R.anim.fade_out);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 사요자 메뉴을을 띄어준다 다이얼로그로
        if(sharedPreferences.getBoolean("isFirstHNAR", false)){

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main, menu);
        actionBar = getSupportActionBar();

        //Custom ActionBar를 사용하기 위해 CustomEnable를 true 시키고 필요 없는 것은 false 한다
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        // Set cumtomView layout
        View mCustomView = LayoutInflater.from(this).inflate(R.layout.actionbar_, null);
        //actionBar.setCustomView(mCustomView);

        //Set actionbar background image
        //actionBar.setBackgroundDrawable();

        //Set actionbar layout layoutparams
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(mCustomView, params);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO);
        actionBar.setLogo(R.drawable.celebration_128_w);

        actionBar.show();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        startActivity(new Intent(this, SettingActivity.class));
        return true;
    }

    class MyPagerAdapter extends FragmentPagerAdapter {
        public MyPagerAdapter(FragmentActivity activity, ViewPager pager) {
            super(activity.getSupportFragmentManager());
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public Fragment getItem(int position) {
            // 각 탭의 Fragment
            // 다음 탭이 미리 생성되어진다.
            if(position < 4){
                fragment = position == 0 ? list[0] : null;
                return list[position];
            }/*else if(position < 4){
                return list2[position-3];
            }*/else{
                return mytab;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            // 각 탭의 Title
            switch (position){
                case 0 :
                    return getResources().getText(R.string.tab1);
                case 1:
                    return getResources().getText(R.string.tab2);
                case 2:
                    return getResources().getText(R.string.tab3);
                case 3:
                    return getResources().getText(R.string.tab4);
                case 4:
                    return getResources().getText(R.string.tab5);
            }
            return null;
        }

        // 화면을 옆으로 넘길 때 화면 밖으로 나가지는 아이템 처리
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
        }
        @Override
        public boolean isViewFromObject(View view, Object object) {
            return super.isViewFromObject(view, object);
        }
    }

    // 리스너들
    View.OnClickListener clickFab = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(!MyApp.loginBool){
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }else {
                Intent intent = new Intent(MainActivity.this, EditActivity.class);
                intent.putExtra("selectedTabPosition", selectedPagePosition);
                startActivity(intent);
            }
        }
    };

    // 메인 엑티비티 뷰 페이저 리스너
    ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
        @Override
        public void onPageSelected(int position) {
            selectedPagePosition = position;
            //getSupportActionBar().setSubtitle(getResources().getStringArray(R.array.appbar_nameArray)[position]);
            changeDesText(position);
        }
        @Override
        public void onPageScrollStateChanged(int state) {}
    };

    private void changeDesText(int position){
        if(tv == null) tv = (TextView)findViewById(R.id.des_text);
        tv.setText(getResources().getStringArray(R.array.appbar_nameArray)[position]);
        if(position == 4) {
            fab.setEnabled(false);
            fab.hide();
        }else{
            fab.setEnabled(true);
            fab.show();
        }
    }

    // MyTabFragment에서 호출됨
    // 프로필 사진
    // 갤러리에서 넘어온 후 처리?인가
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String imagePath = "";
        if(requestCode == 1 && resultCode==RESULT_OK && data != null){
            try {
                Uri imgData = data.getData();
                Bitmap picture = MediaStore.Images.Media.getBitmap(getContentResolver(), imgData);
                // 이미지 크기 줄이기
                final int WIDTH = 512;
                float scale = WIDTH / (float) picture.getWidth();
                final int HEIGHT = (int) (picture.getHeight() * scale);
                picture = Bitmap.createScaledBitmap(picture, WIDTH, HEIGHT, true);
                ImageActivity.bitmap = picture;
                // 비트맵을 jpg로 저장하기
                imagePath = getFilesDir().toString() + "/image.jpg";
                FileOutputStream fos = new FileOutputStream(imagePath);
                fos.flush();
                picture.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();

                Intent intent = new Intent(this, ImageActivity.class);
                intent.putExtra("sel", 0);
                intent.putExtra("imagePath", imagePath);
                startActivity(intent);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    // 갤러리에서 얻어온 이미지 경로 구하기
    private String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
}
