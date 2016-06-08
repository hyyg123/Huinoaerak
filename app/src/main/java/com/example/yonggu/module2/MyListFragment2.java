package com.example.yonggu.module2;

import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by Yonggu on 2015-11-26.
 */
public class MyListFragment2 extends ListFragment implements AbsListView.OnScrollListener{

    private ListView listView;
    private String [] universityArray;
    private ArrayAdapter<String> adapter;
    private Spinner spinner;
    private CursorAdapter cursorAdapter;
    private String tableName, universityName;

    private byte startPosition = 0; // 리스트 뷸 무한 스크롤을 위한 변수
    private boolean isScrollBottom = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle argument = getArguments();
        tableName = argument.getString(MainActivity.tableKey);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_fragment2, container, false);

        universityArray = getResources().getStringArray(R.array.universityArray);
        //universityName = universityArray[11];
        adapter = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, universityArray);
        spinner = (Spinner)view.findViewById(R.id.spinner);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(onItemSelectedListener);
        spinner.setSelection(MyApp.selectedBaseUnivPosition);

        return view;
    }

    // onCreateView 반환된 후 호출됨
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
    }

    @Override
    public void onResume() {
        super.onResume();
        new GetContactsTask().execute(true);
    }

    @Override
    public void onStop() {
        Cursor cursor = cursorAdapter.getCursor(); // 현재의 커서를 얻는다.
        cursorAdapter.changeCursor(null);  // 지금 adapter에 커서가 없다.

        if (cursor != null)
            cursor.close(); // 커서의 리소스를 해제한다.
        super.onStop();
    }

    // 스피너 리스너
    AdapterView.OnItemSelectedListener onItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            //Toast.makeText(view.getContext(), universityArray[position] + " <- 선택되어진거", Toast.LENGTH_SHORT).show();
            universityName = universityArray[position];
            if(tableName.equals(getResources().getStringArray(R.array.tableNameArray)[3])){
                MyApp.selectedUnivPosition = position;
            }else{
                MyApp.selectedPlayUnivPosition = position;
            }

            new GetContactsTask().execute(true);
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };

    // 리스트뷰 리스너
    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            Intent intent = new Intent(getActivity(), ViewFragment.class);
            intent.putExtra("tableName", tableName);
            intent.putExtra("id", id);
            startActivity(intent);
        }
    };

    // GUI 쓰레드 외부에서 데이터베이스 쿼리를 수행한다.
    private class GetContactsTask extends AsyncTask<Boolean, Object, Cursor> {
        private DatabaseConnector databaseConnector =  new DatabaseConnector(getActivity());
        RequestHandler rh = new RequestHandler();

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

                if(responseMsg.equals("NO")){
                    databaseConnector.doEmptyTable(tableName);
                    return null;
                }

                // json parsing
                JSONObject jsonResponse = new JSONObject(responseMsg);  // JSONException 오류 발생 - 이유 모르겠음.....

                // root of json
                JSONArray jsonRootNode = jsonResponse.optJSONArray("table_info");

                // 전에 있던 Table 비우기
                if(params[0]) databaseConnector.doEmptyTable(tableName);

                // get value
                int length = jsonRootNode.length();
                for (int i=0; i<length; i++){
                    JSONObject childNode = jsonRootNode.getJSONObject(i);

                    int number = childNode.optInt("number");
                    String id = childNode.optString("id");
                    String nickname = childNode.optString("nickname").toString();
                    String content = childNode.optString("content").toString();
                    String univ = childNode.optString("univ").toString();
                    String date = childNode.optString("date").toString();
                    String conImage = childNode.optString("conImage").toString();
                    String userImage = childNode.optString("userImage").toString();
                    int comment_cnt = childNode.optInt("comment_cnt");

                    Log.i("USERIMAGE", "userImage = " + userImage);

                    if(univ == null || univ.length() == 0) {
                    }
                    databaseConnector.insertUniversityContact(tableName, number, univ, id, nickname,
                            date, content, conImage, userImage, comment_cnt);
                }
            }catch (JSONException e){
                e.printStackTrace();
            }
            databaseConnector.open();
            return databaseConnector.getUniversityTableContacts(tableName, universityName);
        }

        // doInBackground 메소드에서 반환된 Cursor를 사용한다.
        @Override
        protected void onPostExecute(Cursor cursor) {
            if(cursor != null) {
                cursorAdapter.changeCursor(cursor);    // 어댑터의 커서를 설정한다.
            }
            databaseConnector.close();
        }
    }

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
                            MainActivity.actionBar.hide();
                            MainActivity.fab.hide();
                            MainActivity.fab.setEnabled(false);
                        }else if((startYPosition < endYPosition) && (endYPosition - startYPosition) > 1){
                            MainActivity.fab.show();
                            MainActivity.fab.setEnabled(true);
                            if(listView.getFirstVisiblePosition() == 0 || listView.getFirstVisiblePosition() == 1)
                                MainActivity.actionBar.show();
                        }
                    }
                    break;
            }
            return false;
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
            new GetContactsTask().execute(false);
        }
    }
}
