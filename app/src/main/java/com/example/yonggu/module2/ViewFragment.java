package com.example.yonggu.module2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.yonggu.module2.util.ImageCache;
import com.example.yonggu.module2.util.ImageFetcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Yonggu on 2015-11-28.
 */

public class ViewFragment extends ListFragment implements AbsListView.OnScrollListener,
        AdapterView.OnItemClickListener{
    private final static String TAG = "ViewFragment";
    private static final String IMAGE_CACHE_DIR = "home_alone";

    private TextView nameTextView;
    private TextView timeTextView;
    private RecyclingImageView profileImageView;
    private TextView contentsTextView;
    private TextView categoryTextView;
    private Button smallTalkBtn;
    private RecyclingImageView contentImageView;
    private Button deleteBtn;
    private Button reviseBtn;
    private Button talkBtn;
    private Bitmap pBitmap;

    private long rowID = -1;    // 선택된 행의 ID
    private String selectedTableName, selectedViewName;
    private String userId;
    private String nickName;
    private String userImageForPut;
    private String tableNames[];
    private int selectedTabPosition;
    private String contentImagePathForSend;

    private ListView listView;
    private CommentCursorAdapter cursorAdapter;
    private EditText talkEditTextView;

    // 댓글 창 스크롤 드래그를 위한 변수
    private ScrollView scrollView;
    private LinearLayout commentView;
    private LinearLayout.LayoutParams params;

    private byte startPosition = 0; // 리스트 뷸 무한 스크롤을 위한 변수
    private boolean isScrollBottom = false;

    private ArrayList<String> arrayId, arrayName, arrayImage;

    private boolean isShowById = false;

    private int display_width;

    private ImageFetcher mImageFetcher;
    private int mImageThumbSize;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        arrayId = new ArrayList<>();
        arrayName= new ArrayList<>();
        arrayImage = new ArrayList<>();

        ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(getActivity(), IMAGE_CACHE_DIR);

        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageFetcher(getActivity(), mImageThumbSize);
        mImageFetcher.setLoadingImage(R.drawable.celebration_512_w);
        mImageFetcher.addImageCache(getActivity().getSupportFragmentManager(), cacheParams);
        mImageFetcher.setImageFadeIn(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_view, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView backBtn = (TextView)view.findViewById(R.id.back);
        backBtn.setOnClickListener(onClickListener);

        Intent intent = getActivity().getIntent();
        tableNames = getResources().getStringArray(R.array.tableNameArray);
        selectedTableName = intent.getExtras().getString("tableName");
        rowID = intent.getExtras().getLong("id");
        isShowById = intent.getBooleanExtra("isShowById", false);

        //Log.i("VIEW", "Selected table : " + selectedTableName + ", rowID = " + rowID);

        nameTextView = (TextView)view.findViewById(R.id.nickName);
        timeTextView = (TextView)view.findViewById(R.id.currentTime);
        contentsTextView = (TextView)view.findViewById(R.id.contents);
        categoryTextView = (TextView)view.findViewById(R.id.category);

        contentImageView = (RecyclingImageView) view.findViewById(R.id.contentImageView);
        contentImageView.setOnClickListener(onClickListener);

        profileImageView = (RecyclingImageView) view.findViewById(R.id.profileImage);
        profileImageView.setOnClickListener(onClickListener);

        // 보여질 탭의 이름 정하기
        for(int i=0;i<tableNames.length; i++){
            if(selectedTableName.equals(tableNames[i])){
                selectedTabPosition = i;
                selectedViewName = getResources().getStringArray(R.array.viewTabTextArray)[i];
                break;
            }
        }

        cursorAdapter = new CommentCursorAdapter(getActivity(), null);
        setListAdapter(cursorAdapter);

        listView = getListView();
        listView.setOnScrollListener(this);
        listView.setOnItemClickListener(this);
        //listView.setOnTouchListener(touchListener);

        talkEditTextView =  (EditText)view.findViewById(R.id.talkEditText);
        talkEditTextView.setOnTouchListener(touchListener);

        talkBtn = (Button)view.findViewById(R.id.talkBtn);
        talkBtn.setOnClickListener(onClickListener);

        smallTalkBtn = (Button)view.findViewById(R.id.small_talk);
        smallTalkBtn.setOnTouchListener(touchListener);

        scrollView = (ScrollView)view.findViewById(R.id.scrollView);
        params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100);

        commentView = (LinearLayout)view.findViewById(R.id.commentLayout);

        reviseBtn = (Button)view.findViewById(R.id.reviseBtn);
        deleteBtn = (Button)view.findViewById(R.id.deleteBtn);

        Display display = getActivity().getWindowManager().getDefaultDisplay();

        display_width = display.getWidth();
    }

    @Override
    public void onResume() {
        super.onResume();
        new LoadContactTask().execute(rowID);   // rowID 에 해당되는 연락처를 적재한다.
        // 댓글들을 불러온다.
        // 1. 불러올 때 전의 댓글수와 받아올 댓글의 수를 비교 -> 같으면 안불러온다. 다르면 불러온다.
        // 2. 아니면 전부 삭제 후 다시 다 불러온다. 3번이랑 같은거네
        // 3. 리스트 뷰로 할까...? 그러면 커서 어댑터도 만들어야되는데 그래 리스트뷰로 하는게 더 빠를 듯
        //  -> 내부 디비에 테이블 하나 더 추가, 커서 어답터 하나 더 커스텀, layout에 리스트뷰 추가
        //      리스트뷰 복습할 겸 해보자
        new LoadCommentsTask().execute(true);

        mImageFetcher.setExitTasksEarly(false);
        cursorAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        if(pBitmap != null){
            //pBitmap.recycle();
        }

        cursorAdapter.bitmapLoarder.recycleBitmap();

        Cursor cursor = cursorAdapter.getCursor(); // 현재의 커서를 얻는다.
        cursorAdapter.changeCursor(null);  // 지금 adapter에 커서가 없다.

        if (cursor != null)
            cursor.close(); // 커서의 리소스를 해제한다.
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageFetcher.setPauseWork(false);
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent;
            switch (view.getId()){
                // 글 수정 버튼
                case R.id.reviseBtn :
                    intent = new Intent(getActivity(), EditActivity.class);
                    intent.putExtra("isRevise", true);
                    intent.putExtra("rowId", rowID);
                    intent.putExtra("contents", contentsTextView.getText());
                    intent.putExtra("selectedTabPosition", selectedTabPosition);
                    intent.putExtra("contentImagePath", contentImagePathForSend);
                    startActivity(intent);
                    getActivity().finish();
                    break;
                // 글 삭제 버튼
                case R.id.deleteBtn :
                    delete();
                    break;
                // 서버에 댓글을 전송
                case R.id.talkBtn :
                    if(MyApp.loginBool) {
                        if (!talkEditTextView.getText().toString().equals("")){
                            new InsertCommentTask().execute();

                            InputMethodManager methodManager =
                                    (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            methodManager.hideSoftInputFromWindow(talkEditTextView.getWindowToken(), 0);

                            MyListFragment.isUpdate = true;
                        }
                    }else {
                        startActivity(new Intent(getActivity(), LoginActivity.class));
                    }
                    break;
                // 프로필을 보여주는 액티비티로 이동
                case R.id.profileImage :
                    intent = new Intent(getActivity(), ProfileViewActivity.class);
                    intent.putExtra("id", userId);
                    intent.putExtra("nickName", nickName);
                    intent.putExtra("userImage", userImageForPut);
                    startActivity(intent);
                    break;
                case R.id.contentImageView :
                    intent = new Intent(getActivity(), ImageViewActivity.class);
                    intent.putExtra("imagePath", contentImagePathForSend);
                    startActivity(intent);
                    break;
                case R.id.back :
                    getActivity().finish();
                    break;
            }
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), ProfileViewActivity.class);
        intent.putExtra("id", arrayId.get(position));
        intent.putExtra("nickName", arrayName.get(position));
        intent.putExtra("userImage", arrayImage.get(position));
        startActivity(intent);
    }

    private class LoadCommentsTask extends AsyncTask<Boolean, Object, Cursor>{
        final private DatabaseConnector dc = new DatabaseConnector(getActivity());
        final private RequestHandler rh = new RequestHandler();
        private int board_num = 0;

        @Override
        protected Cursor doInBackground(Boolean... params) {
            HashMap<String, String> data = new HashMap<>();
            data.put("board_num", Long.toString(rowID));
            data.put("sel", MyApp.GET_COMMENTS);

            if(params[0]) startPosition = 0;
            data.put("startPosition", Byte.toString(startPosition));

            String response = rh.sendPostRequest(MyApp.urlStr, data);

            if (response.contains("NO")){
                dc.doEmptyTable(DatabaseConnector.COMMENT_TABLE);
                return null;
            }

            // 전에 있던 Table 비우기
            if(params[0]) dc.doEmptyComments((int) rowID);    // 갱신하기 위하여..?

            try {
                JSONObject jsonResponse = new JSONObject(response);
                JSONArray jsonRootNode = jsonResponse.optJSONArray("comment_info");

                int length = jsonRootNode.length();
                for(int i=0; i<length; i++){
                    JSONObject childNode = jsonRootNode.getJSONObject(i);
                    String id = childNode.optString("id");
                    String nickname = childNode.optString("nickname");
                    String userImage = childNode.optString("userImage");
                    board_num = childNode.optInt("board_num");
                    String content = childNode.optString("content");
                    String date = childNode.optString("date");

                    dc.insertComment(id, board_num, content, date, userImage, nickname);

                    // 해당 댓글의 프로필 정보를 저장한다
                    arrayId.add(id);
                    arrayName.add(nickname);
                    if(!userImage.equals("null")) userImage = MyApp.urlForImage+ "/" + userImage;
                    else userImage = "";
                    arrayImage.add(userImage);
                }
            }catch (JSONException e){
                e.printStackTrace();
            }

            dc.open();
            return dc.getComments((int) rowID);
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            if (cursor!= null) {
                cursorAdapter.changeCursor(cursor);
                cursorAdapter.bitmapLoarder.recycleBitmap();
            }
            dc.close();
        }

    }
    private class LoadContactTask extends AsyncTask<Long, Object, Cursor>{
        final DatabaseConnector databaseConnector = new DatabaseConnector(getActivity());

        @Override
        protected Cursor doInBackground(Long... longs) {
            databaseConnector.open();
            if(isShowById){
                return databaseConnector.getOneContact(longs[0],DatabaseConnector.CONTACT_BY_TABLE);
            }else{
                return databaseConnector.getOneContact(longs[0], selectedTableName);
            }
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            super.onPostExecute(cursor);
            cursor.moveToFirst();   // 첫번째 행으로 이동한다.

            userId = cursor.getString(cursor.getColumnIndex("userId"));
            categoryTextView.setText(selectedViewName);
            nickName = cursor.getString(cursor.getColumnIndex("name"));
            nameTextView.setText(nickName);
            timeTextView.setText(cursor.getString(cursor.getColumnIndex("time")));
            contentsTextView.setText(cursor.getString(cursor.getColumnIndex("contents")));

            final String userImagePath = cursor.getString(cursor.getColumnIndex("userImage"));
            userImageForPut = userImagePath;
            final String contentImagePath = cursor.getString(cursor.getColumnIndex("conImage"));
            contentImagePathForSend = contentImagePath;

            cursor.close();
            databaseConnector.close();

            // 이미지 설정을 하지 않은 경우
            if(userImagePath.equals("") || userImagePath.contains("null")){
                profileImageView.setImageResource(R.drawable.user);
                // 이미지 설정을 한 경우
            }else{
                mImageFetcher.loadImage(userImagePath, profileImageView);
                /*final String keyName_UserImage = "c" + userImagePath.substring(
                        userImagePath.lastIndexOf("/")+1, userImagePath.lastIndexOf("."));
                new AsyncTask<String, Void, Bitmap>(){
                    @Override
                    protected Bitmap doInBackground(String... params) {
                        // 캐시에서 이미지를 가져옴


//                        Bitmap result = MyApp.imageCache.getBimap(keyName_UserImage);
//                        // 캐시에 이미지가 없을 경우
//                        if(result == null){
//                            try {
//                                URL imageURL = new URL(userImagePath);
//                                HttpURLConnection urlConnection = (HttpURLConnection)imageURL.openConnection();
//                                urlConnection.setDoInput(true);
//                                urlConnection.connect();
//
//                                BufferedInputStream bis = new BufferedInputStream
//                                        (urlConnection.getInputStream(), urlConnection.getContentLength());
//
//                                result = BitmapFactory.decodeStream(bis);
//                                bis.close();
//                            }catch (Exception e){
//                                e.printStackTrace();
//                            }
//                            if(result != null){
//                                MyApp.imageCache.put(keyName_UserImage, result);
//
//                                // 전에 캐시에 저장되어 있는 이미지 지우기
//                                String temp_str = keyName_UserImage.substring(
//                                        1, keyName_UserImage.lastIndexOf("_"));
//                                int user_cnt = Integer.parseInt(temp_str) - 1;
//
//                                temp_str = keyName_UserImage.replaceFirst(temp_str, Integer.toString(user_cnt));
//                                boolean re = MyApp.imageCache.reomveKey(temp_str);
//                            }
//                        }

                        return null;
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
//                        if(bitmap != null){
//                            profileImageView.setImageBitmap(bitmap);
//                            pBitmap = bitmap;
//                        }else {
//                            profileImageView.setImageResource(R.drawable.user);
//                        }
                    }
                }.execute();*/
            }

            // 내용 이미지를 설정하지 않은 경우
            if(contentImagePath.contains("null") || contentImagePath.equals("")){
                contentImageView.setImageBitmap(null);
                contentImageView.setVisibility(View.GONE);
                // 내용 이미지를 설정한 경우
            }else{
                mImageFetcher.loadImage(contentImagePath, contentImageView);
                /*final String keyName_ConImage = "c" + contentImagePath.substring(
                        contentImagePath.lastIndexOf("/")+1, contentImagePath.lastIndexOf("."));
                new AsyncTask<Void, Void, Bitmap>(){
                    @Override
                    protected Bitmap doInBackground(Void... params) {
                        mImageFetcher.loadImage(contentImagePath, contentImageView);
                        // 캐시에서 이미지를 가져옴
//                        Bitmap result = MyApp.imageCache.getBimap(keyName_ConImage);
//                        // 캐시에 이미지가 없을 경우
//                        if(result == null){
//                            try {
//                                URL imageURL = new URL(contentImagePath);
//                                HttpURLConnection urlConnection = (HttpURLConnection)imageURL.openConnection();
//                                urlConnection.setDoInput(true);
//                                urlConnection.connect();
//
//                                BufferedInputStream bis = new BufferedInputStream
//                                        (urlConnection.getInputStream(), urlConnection.getContentLength());
//
//                                result = BitmapFactory.decodeStream(bis);
//                                bis.close();
//                            }catch (Exception e){
//                                e.printStackTrace();
//                            }
//                            if(result != null){
//                                MyApp.imageCache.put(keyName_ConImage, result);
//
//                                // 전에 캐시에 저장되어 있는 이미지 지우기
//                                String temp_str = keyName_ConImage.substring(
//                                        1, keyName_ConImage.lastIndexOf("_"));
//                                int user_cnt = Integer.parseInt(temp_str) - 1;
//
//                                temp_str = keyName_ConImage.replaceFirst(temp_str, Integer.toString(user_cnt));
//                                boolean re = MyApp.imageCache.reomveKey(temp_str);
//                            }
//                        }
//
//                        float scale = (float)display_width / result.getWidth() ;
//                        int height = (int)(result.getHeight() * scale);
//
//                        result = Bitmap.createScaledBitmap(result, display_width, height, false);
//
//                        return result;
                        return null;
                    }
                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        *//*if(bitmap != null){
                            contentImageView.setVisibility(View.VISIBLE);
                            contentImageView.setImageBitmap(bitmap);
                        }else{
                            contentImageView.setVisibility(View.GONE);
                            contentImageView.setImageBitmap(null);
                        }*//*
                    }
                }.execute();*/
            }

            // 해당하는 글이 자신의 글일 경우
            // 수정, 삭제 버튼을 보여준다.

            if(MyApp.personId != null && userId.equals(MyApp.personId)){
                reviseBtn.setEnabled(true);
                reviseBtn.setVisibility(View.VISIBLE);
                deleteBtn.setVisibility(View.VISIBLE);
                reviseBtn.setOnClickListener(onClickListener);
                deleteBtn.setOnClickListener(onClickListener);
            }else{
                reviseBtn.setEnabled(false);
                deleteBtn.setEnabled(false);
                reviseBtn.setVisibility(View.INVISIBLE);
                deleteBtn.setVisibility(View.INVISIBLE);
                reviseBtn.setOnClickListener(null);
                deleteBtn.setOnClickListener(null);
            }
        }
    }

    private void delete(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("정말 삭제 하시겠습니까?");
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                new AsyncTask<Object, Object, Object>() {
                    RequestHandler rh = new RequestHandler();
                    DatabaseConnector dc = new DatabaseConnector(getActivity());
                    @Override
                    protected Object doInBackground(Object... objects) {
                        try {
                            HashMap<String,String> body = new HashMap<String, String>();
                            body.put("sel", MyApp.DELETE_TAB);
                            body.put("number", Long.toString(rowID));

                            String responseMsg = rh.sendPostRequest(MyApp.urlStr, body);

                            dc.deleteContact(rowID, selectedTableName);

                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        //final DatabaseConnector dc = new DatabaseConnector(ViewFragment.this);
                        //dc.deleteContact(rowID, selectedTableName);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Object o) {
                        getActivity().finish();
                    }
                }.execute();
            }
        });

        builder.create().show();
    }


    private class InsertCommentTask extends AsyncTask<Void, Void, String>{
        final private RequestHandler rh = new RequestHandler();
        private String content;

        @Override
        protected void onPreExecute() {
            content = talkEditTextView.getText().toString();
            talkEditTextView.setText("");
        }

        @Override
        protected String  doInBackground(Void... params) {
            Date date = new Date(System.currentTimeMillis());
            SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String dateStr = format.format(date);

            HashMap<String, String> data = new HashMap<>();
            data.put("sel", MyApp.INSERT_COMMENT);
            data.put("id", MyApp.personId);
            data.put("board_num", Long.toString(rowID));
            data.put("content", content);
            data.put("date", dateStr);

            String responseMsg = rh.sendPostRequest(MyApp.urlStr, data);
            Log.d(TAG, responseMsg);

            return responseMsg;
        }

        @Override
        protected void onPostExecute(String response) {
            //startActivity(new Intent(ViewFragment.this, TempActivity.class));
            onResume();
            new PushNotificationTask(response).execute();
        }
    }

    private class PushNotificationTask extends AsyncTask<Void, Void, Void>{
        final private RequestHandler rh = new RequestHandler();
        String responseMsg;

        PushNotificationTask(String responseMsg){
            this.responseMsg = responseMsg;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                JSONObject jsonResponse = new JSONObject(responseMsg);
                //JSONArray jsonRootNode = jsonResponse.optJSONArray("push_notification_info");

                //JSONObject childNode = jsonRootNode.getJSONObject(0);
                JSONObject childNode = jsonResponse.optJSONObject("push_notification_info");
                String id = childNode.optString("id");
                String nickname = childNode.optString("nickname");
                String tab_type= childNode.optString("tab_type");
                String board_num = childNode.optString("board_num");
                String comment = childNode.optString("comment");

                if(id.contains(MyApp.personId)){
                    return null;
                }

                HashMap<String, String> push_data = new HashMap<>();
                push_data.put("id", id);
                push_data.put("nickname", nickname);
                push_data.put("board_num", board_num);
                push_data.put("tab_type", tab_type);
                push_data.put("comment", comment);

                String response = rh.sendPostRequest(MyApp.urlForPushNotification, push_data);
                Log.d(TAG, response);

            }catch (JSONException e){
                e.printStackTrace();
            }
            return null;
        }
    }

    View.OnTouchListener touchListener = new View.OnTouchListener() {
        boolean firstDragFlag = true;
        boolean dragFlag = false;
        double startYPosition = 0.0, endYPosition = 0.0;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int id = v.getId();

            if (id == R.id.small_talk) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        if(scrollView.getHeight() > 400){
                            params.weight = 0;
                            scrollView.setLayoutParams(params);
                        }else{
                            params.weight = 7;
                            scrollView.setLayoutParams(params);
                        }
                        smallTalkBtn.setBackground(getResources().getDrawable(R.drawable.image_back));
                        break;
                    case MotionEvent.ACTION_MOVE:
                        //params.height = scrollView.getHeight() + (int)event.getY();
                        //scrollView.setLayoutParams(params);
                        break;
                    case MotionEvent.ACTION_DOWN:
                        smallTalkBtn.setBackground(getResources().getDrawable(R.drawable.image_back_pushed));
                        break;
                }
            }else if(id == R.id.talkEditText){
                v.requestFocus();
                InputMethodManager imm = (InputMethodManager)
                        getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
                params.weight = 0;
                scrollView.setLayoutParams(params);
            }
            return true;
        }
    };

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if(totalItemCount <= firstVisibleItem + visibleItemCount){
            isScrollBottom = true;
            startPosition = (byte)totalItemCount;
        }else{
            isScrollBottom = false;
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE && isScrollBottom){
            new LoadCommentsTask().execute(false);
        }
    }

    /*@Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }*/

    private class CommentCursorAdapter extends CursorAdapter{
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

            viewHolder.commentImageView = (RecyclingImageView)v.findViewById(R.id.commentImageView);
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
                /*final String keyName_UserImage = "c" + imagePath.substring(
                        imagePath.lastIndexOf("/")+1, imagePath.lastIndexOf("."));
                //Log.i(TAG, "key name : " + keyName_UserImage);

                bitmapLoarder.loadBitmap(context.getResources(),
                        keyName_UserImage, imagePath, viewHolder.commentImageView);*/

                mImageFetcher.loadImage(imagePath, viewHolder.commentImageView);
            }

            //view.startAnimation(animation);
        }
        private class ViewHolder{
            RecyclingImageView commentImageView;
            TextView nickNameTextView;
            TextView dateTextView;
            TextView commentTextView;
        }
    }
}