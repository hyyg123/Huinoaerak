package com.example.yonggu.module2;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.yonggu.module2.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Yonggu on 2015-11-26.
 */
public class  MyListFragment extends ListFragment implements AbsListView.OnScrollListener{
    private final static String TAG = "MyListFragment";
    private static final String IMAGE_CACHE_DIR = "home_alone";

    private static final int profileImageSize = 200;
    private static final int conImageSize = 400;

    private ListView listView;
    private MyCursorAdapter cursorAdapter;
    private String tableName;
    private byte startPosition = 0; // 리스트 뷸 무한 스크롤을 위한 변수
    private boolean isScrollBottom = false;
    private boolean isScrollTop = false;
    public static boolean isUpdate = true;

    private boolean isActionBarAlwaysShow;
    private boolean isButtonAlwaysShow;

    private ImageFetcher mImageFetcher1, mImageFetcher2;
    private int mImageThumbSize;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle argument = getArguments();
        tableName = argument.getString(MainActivity.tableKey);

        ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(getActivity(), IMAGE_CACHE_DIR);

        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher1 = new ImageFetcher(getActivity(), mImageThumbSize);
        mImageFetcher1.setLoadingImage(R.drawable.celebration_512_w);
        mImageFetcher1.addImageCache(getActivity().getSupportFragmentManager(), cacheParams);
        mImageFetcher1.setImageFadeIn(true);

        mImageFetcher2 = new ImageFetcher(getActivity(), mImageThumbSize);
        mImageFetcher2.setLoadingImage(R.drawable.celebration_512_w);
        mImageFetcher2.addImageCache(getActivity().getSupportFragmentManager(), cacheParams);
        mImageFetcher2.setImageFadeIn(true);

        /*JSONObject rootNode = new JSONObject();

        Map<String, Object> selectList = new HashMap<>();

        selectList.put("key1", "data1");
        selectList.put("key2", "data2");
        selectList.put("key3", "data3");

        JSONArray jsonArray = new JSONArray();
        JSONObject tempObject = null;

        Set<String> keySet = selectList.keySet();
        for(String key : keySet){
            try {
                tempObject = new JSONObject();
                tempObject.put(key, (String)selectList.get(key));
                jsonArray.put(tempObject);
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        try{
            rootNode.put("jsonTest", jsonArray);
        }catch (JSONException e){

        }

        Log.d(TAG, rootNode.toString());*/

    }

    // View 만들기
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_fragment, container, false);
    }

    // View 생성된 후 호출됨
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        //super.onViewCreated(view, savedInstanceState);
        setRetainInstance(true);

        cursorAdapter = new MyCursorAdapter(getActivity(), null);
        setListAdapter(cursorAdapter);

        listView = getListView();
        listView.setOnItemClickListener(itemClickListener);
        listView.setOnTouchListener(scollCheck);
        listView.setOnScrollListener(this);

        isUpdate = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        isActionBarAlwaysShow = PreferenceManager.getDefaultSharedPreferences(getContext()).
                getBoolean(getResources().getString(R.string.isActionBarAlwaysShow), false);
        isButtonAlwaysShow = PreferenceManager.getDefaultSharedPreferences(getContext()).
                getBoolean(getResources().getString(R.string.isButtonAlwaysShow), false);
        new GetContactsTask().execute(isUpdate);
        mImageFetcher1.setExitTasksEarly(false);
        mImageFetcher2.setExitTasksEarly(false);
        cursorAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();

        //cursorAdapter.bitmapLoarder1.recycleBitmap();
        ///cursorAdapter.bitmapLoarder2.recycleBitmap();

        Cursor cursor = cursorAdapter.getCursor(); // 현재의 커서를 얻는다.
        cursorAdapter.changeCursor(null);  // 지금 adapter에 커서가 없다.

        if (cursor != null)
            cursor.close(); // 커서의 리소스를 해제한다.
    }

    @Override
    public void onPause() {
        super.onPause();

        mImageFetcher1.setPauseWork(false);
        mImageFetcher1.setExitTasksEarly(true);
        mImageFetcher1.flushCache();

        mImageFetcher2.setPauseWork(false);
        mImageFetcher2.setExitTasksEarly(true);
        mImageFetcher2.flushCache();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mImageFetcher1.closeCache();
        mImageFetcher2.closeCache();
    }

    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            Intent intent = new Intent(getActivity(), RealViewActivity.class);
            intent.putExtra("tableName", tableName);
            intent.putExtra("id", id);
            startActivity(intent);
        }
    };

    private Boolean isFirst = true;

    // GUI 쓰레드 외부에서 데이터베이스 쿼리를 수행한다.
    private class GetContactsTask extends AsyncTask<Boolean, Object, Cursor> {
        final DatabaseConnector databaseConnector =new DatabaseConnector(getActivity());
        RequestHandler rh = new RequestHandler();
        Boolean isBoardNum;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        // 데이터베이스를 열고 모든 연락처 정보가 담긴 Cursor를 반환한다.
        @Override
        protected Cursor doInBackground(Boolean... params) {
            try {
                HashMap<String, String> body = new HashMap<>();
                body.put("sel", MyApp.SELECT_TABLE);
                body.put("table_name", tableName);

                if(params[0]) startPosition = 0;
                body.put("startPosition", Byte.toString(startPosition));

                String responseMsg = rh.sendPostRequest(MyApp.urlStr, body);

                if(responseMsg.contains("NO")){
                    databaseConnector.doEmptyTable(tableName);
                    return null;
                }else if(responseMsg.equals(" ")){
                    databaseConnector.open();
                    return databaseConnector.getNormalTableContacts(tableName);
                }

                Log.d(TAG, responseMsg);
                // json parsing
                JSONObject jsonResponse = new JSONObject(responseMsg);  // JSONException 오류 발생 - 이유 모르겠음.....

                // root of json
                JSONArray jsonRootNode = jsonResponse.optJSONArray("table_info");

                // 전에 있던 Table 비우기
                if(params[0]) databaseConnector.doEmptyTable(tableName);

                // get value
                int length = jsonRootNode.length();
                for(int i=0; i<length; i++){
                    JSONObject childNode = jsonRootNode.getJSONObject(i);

                    String id = childNode.optString("id");
                    int number = childNode.optInt("number");
                    String nickname = childNode.optString("nickname").toString();
                    String content = childNode.optString("content").toString();
                    String date = childNode.optString("date").toString();
                    String conImage = childNode.optString("conImage").toString();
                    String userImage = childNode.optString("userImage").toString();
                    int comment_cnt = childNode.optInt("comment_cnt");

                    isBoardNum = databaseConnector.isBoardNum(tableName, number);
                    if(isBoardNum == false){
                        databaseConnector.insertNormalContact(tableName, number, id, nickname,
                                date, content, conImage, userImage, comment_cnt);
                    }

                }
            }catch (JSONException e){
                e.printStackTrace();
            }catch (SQLiteConstraintException e){
                e.printStackTrace();
            }

            databaseConnector.open();
            return databaseConnector.getNormalTableContacts(tableName);
        }

        // doInBackground 메소드에서 반환된 Cursor를 사용한다.
        @Override
        protected void onPostExecute(Cursor cursor) {
            cursorAdapter.changeCursor(cursor);    // 어댑터의 커서를 설정한다.

            databaseConnector.close();
            isUpdate = false;
            MainActivity.new_fab.hide();

            //cursorAdapter.bitmapLoarder1.recycleBitmap();
            //cursorAdapter.bitmapLoarder2.recycleBitmap();
        }
    }

    // 스크롤 터치 이벤트
    // 액션바와 플로팅버튼을 컨트롤한다
    View.OnTouchListener scollCheck = new View.OnTouchListener() {
        boolean firstDragFlag = true;
        boolean dragFlag = false;
        double startYPosition = 0.0, endYPosition = 0.0;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()){
                case MotionEvent.ACTION_MOVE :
                    dragFlag = true;
                    if(firstDragFlag){
                        startYPosition = event.getY();
                        firstDragFlag = false;
                    }
                    break;
                case MotionEvent.ACTION_UP :
                    endYPosition = event.getY();
                    firstDragFlag = true;
                    if(dragFlag){
                        if((startYPosition > endYPosition) && (startYPosition - endYPosition) > 1 ){
                            if(!isActionBarAlwaysShow){
                                MainActivity.actionBar.hide();
                            }
                            if(!isButtonAlwaysShow){
                                MainActivity.fab.hide();
                                MainActivity.fab.setEnabled(false);
                            }
                        }else if((startYPosition < endYPosition) && (endYPosition - startYPosition) > 1){
                            MainActivity.fab.show();
                            MainActivity.fab.setEnabled(true);

                            if(listView.getFirstVisiblePosition() == 0 || listView.getFirstVisiblePosition() == 1) {
                                MainActivity.actionBar.show();
                            }

                            if(listView.getFirstVisiblePosition() == 0){
                                MainActivity.new_fab.show();
                                onResume();
                            }
                        }
                    }
                    break;
            }
            return false;
        }
    };

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if(totalItemCount <= firstVisibleItem + 5){
            isScrollBottom = true;
            startPosition = (byte)totalItemCount;
        }else{
            isScrollBottom = false;
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if((scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE ||
                scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) && isScrollBottom){
            new GetContactsTask().execute(false);
        }

        // Pause fetcher to ensure smoother scrolling when flinging
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
            // Before Honeycomb pause image loading on scroll to help with performance
            if (!com.example.yonggu.module2.util.Utils.hasHoneycomb()) {
                mImageFetcher1.setPauseWork(true);
                mImageFetcher2.setPauseWork(true);
            }
        } else {
            mImageFetcher1.setPauseWork(false);
            mImageFetcher2.setPauseWork(false);
        }
    }

    private class MyCursorAdapter extends CursorAdapter {
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

            /*bitmapLoarder1 = new BitmapLoarder();
            bitmapLoarder1.placeHolderBitmap =
                    BitmapFactory.decodeResource(context.getResources(), R.drawable.celebration_128_w);
            bitmapLoarder2 = new BitmapLoarder();
            bitmapLoarder2.placeHolderBitmap =
                    BitmapFactory.decodeResource(context.getResources(), R.drawable.celebration_128_w);*/
            animation = AnimationUtils.loadAnimation(context, R.anim.short_fade);
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
                mImageFetcher1.setImageSize(profileImageSize);
                mImageFetcher1.loadImage(userImagePath, viewHolder.profileImage);
            }

            //mImageFetcher.loadImage(Images.imageThumbUrls[position - mNumColumns], imageView);
            // 내용 이미지를 설정하지 않은 경우
            if (conImagePath.contains("null") || conImagePath.equals("")) {
                viewHolder.contentImage.setImageBitmap(null);
                viewHolder.contentImage.setVisibility(View.GONE);
                // 내용 이미지를 설정한 경우
            } else {
                viewHolder.contentImage.setVisibility(View.VISIBLE);
                mImageFetcher2.setImageSize(conImageSize);
                mImageFetcher2.loadImage(conImagePath, viewHolder.contentImage);
            }

            //Log.d(TAG, "viewArray put position :" + cursor.getPosition());
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

            View view = inflater.inflate(R.layout.listview_item, viewGroup, false);

            viewHolder.profileImage = (RecyclingImageView) view.findViewById(R.id.profileImage);
            viewHolder.contentImage = (RecyclingImageView) view.findViewById(R.id.uploadImage);
            viewHolder.profileNickName = (TextView)view.findViewById(R.id.nickName);
            viewHolder.currentTime = (TextView)view.findViewById(R.id.currentTime);
            viewHolder.contents = (TextView)view.findViewById(R.id.contents);
            viewHolder.commentCntTextView = (TextView)view.findViewById(R.id.commentCntTextView);

            //Log.d(TAG, "new view");
            view.setTag(viewHolder);
            return view;
        }

        private class ViewHolder{
            RecyclingImageView profileImage, contentImage;
            TextView profileNickName, currentTime, contents, commentCntTextView;
        }
    }
}
