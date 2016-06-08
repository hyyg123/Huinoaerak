package com.example.yonggu.module2;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
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

import com.example.yonggu.module2.util.ImageCache;
import com.example.yonggu.module2.util.ImageFetcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * Created by Yonggu on 16. 1. 25..
 * 유저가 쓴 글을 보여주는 액티비티
 */

public class ShowBoardFragment extends ListFragment implements AdapterView.OnItemClickListener,
        AbsListView.OnScrollListener, View.OnClickListener{
    private static final String IMAGE_CACHE_DIR = "home_alone";

    private static final int profileImageSize = 100;
    private static final int conImageSize = 400;

    private ListView listView;
    private MyCursorAdapter cursorAdapter;
    private String id;
    private byte startPosition = 0; // 리스트 뷸 무한 스크롤을 위한 변수
    private boolean isScrollBottom = false;
    private HashMap<Integer, String> tableMap;
    private static int index = 0;
    private TextView title;
    private String nickname;
    public static boolean isUpdate = true;

    private ImageFetcher mImageFetcher;
    private int mImageThumbSize;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        return inflater.inflate(R.layout.activity_show_board, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView backBtn = (TextView)view.findViewById(R.id.back);
        backBtn.setOnClickListener(this);

        title = (TextView)view.findViewById(R.id.categoryName);

        cursorAdapter = new MyCursorAdapter(getActivity(), null);
        setListAdapter(cursorAdapter);

        listView = getListView();
        listView.setOnItemClickListener(this);
        listView.setOnScrollListener(this);

        id = getActivity().getIntent().getStringExtra("id");

        tableMap = new HashMap<>();

        isUpdate = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        new GetBoardByIdTask().execute(isUpdate);

        mImageFetcher.setExitTasksEarly(false);
        cursorAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        //cursorAdapter.bitmapLoarder1.recycleBitmap();
        //cursorAdapter.bitmapLoarder2.recycleBitmap();

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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.back :
                getActivity().finish();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), RealViewActivity.class);
        intent.putExtra("tableName", tableMap.get(position));
        intent.putExtra("id", id);
        intent.putExtra("isShowById", true);
        startActivity(intent);
    }

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
            new GetBoardByIdTask().execute(false);
        }

        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
            // Before Honeycomb pause image loading on scroll to help with performance
            if (!com.example.yonggu.module2.util.Utils.hasHoneycomb()) {
                mImageFetcher.setPauseWork(true);
            }
        } else {
            mImageFetcher.setPauseWork(false);
        }
    }

    private class GetBoardByIdTask extends AsyncTask<Boolean, Void, Cursor>{
        final DatabaseConnector databaseConnector =new DatabaseConnector(getActivity());
        Boolean isBoardNum;

        @Override
        protected Cursor doInBackground(Boolean... params) {
            HashMap<String, String> data = new HashMap<>();
            data.put("sel", MyApp.GET_CONTACT_BY);
            data.put("id", id);

            if(params[0]) startPosition = 0;
            data.put("startPosition", Integer.toString(startPosition));

            String response = new RequestHandler().sendPostRequest(MyApp.urlStr, data);

            if(response.contains("NO")){
                databaseConnector.doEmptyTable(DatabaseConnector.CONTACT_BY_TABLE);
                return null;
            }else if(response.equals(" ")){
                databaseConnector.open();
                return databaseConnector.getContactById();
            }
            try{
                JSONObject jsonResponse = new JSONObject(response);  // JSONException 오류 발생 - 이유 모르겠음.....

                // root of json
                JSONArray jsonRootNode = jsonResponse.optJSONArray("table_by_info");

                // 전에 있던 Table 비우기
                if(params[0]) {
                    index = 0;
                    databaseConnector.doEmptyTable(DatabaseConnector.CONTACT_BY_TABLE);
                }

                // get value
                int length = jsonRootNode.length();
                for(int i=0; i<length; i++){
                    JSONObject childNode = jsonRootNode.getJSONObject(i);

                    String id = childNode.optString("id");
                    int number = childNode.optInt("number");
                    nickname = childNode.optString("nickname").toString();
                    String content = childNode.optString("content").toString();
                    String date = childNode.optString("date").toString();
                    String conImage = childNode.optString("conImage").toString();
                    String userImage = childNode.optString("userImage").toString();
                    String univ = childNode.optString("univ");
                    int comment_cnt = childNode.optInt("comment_cnt");
                    String tableName = childNode.optString("tableName").toString();

                    tableMap.put(index++, tableName);

                    isBoardNum = databaseConnector.isBoardNum(DatabaseConnector.CONTACT_BY_TABLE, number);
                    if(isBoardNum == false){
                        databaseConnector.insertContactForShowById(number, univ, tableName, id, nickname,
                                date, content, conImage, userImage, comment_cnt);
                    }
                }
            }catch (JSONException e){
                e.printStackTrace();
            }

            databaseConnector.open();
            return databaseConnector.getContactById();
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            cursorAdapter.changeCursor(cursor);
            databaseConnector.close();
            title.setText(nickname + "님의 글");

            isUpdate = false;

            //cursorAdapter.bitmapLoarder1.recycleBitmap();
            //cursorAdapter.bitmapLoarder2.recycleBitmap();
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
                mImageFetcher.setImageSize(profileImageSize);
                mImageFetcher.loadImage(userImagePath, viewHolder.profileImage);
            }

            //mImageFetcher.loadImage(Images.imageThumbUrls[position - mNumColumns], imageView);
            // 내용 이미지를 설정하지 않은 경우
            if (conImagePath.contains("null") || conImagePath.equals("")) {
                viewHolder.contentImage.setImageBitmap(null);
                viewHolder.contentImage.setVisibility(View.GONE);
                // 내용 이미지를 설정한 경우
            } else {
                viewHolder.contentImage.setVisibility(View.VISIBLE);
                mImageFetcher.setImageSize(conImageSize);
                mImageFetcher.loadImage(conImagePath, viewHolder.contentImage);
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


}














