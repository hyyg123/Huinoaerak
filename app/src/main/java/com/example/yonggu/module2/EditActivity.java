package com.example.yonggu.module2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.plus.Plus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

/**
 * Created by Yonggu on 2015-11-28.
 */
public class EditActivity extends Activity {
    private Button saveBtn;
    private EditText contentsText;
    private long rowID;
    private Spinner spinner;    //, spinner2;
    private ArrayAdapter<String> adapter;   // adapter2;
    private String [] tabArray, tabArray2;
    private String selectedTableName, selectedUniversityName, rContentsText;
    private boolean isUpdate;
    private Button imageBtn;
    private ImageView contentImageView;
    private String contentImagePath;

    // 갤러리에서 호출한 이미지를 위한 변수들
    private String imagePath = "";
    private Bitmap picture = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        setContentView(R.layout.activity_edit);

        saveBtn = (Button)findViewById(R.id.saveBtn);
        saveBtn.setOnClickListener(clickListener);
        imageBtn = (Button)findViewById(R.id.imageBtn);
        imageBtn.setOnClickListener(clickListener);
        contentImageView = (ImageView)findViewById(R.id.editImageView);
        contentImageView.setOnClickListener(clickListener);
        contentImageView.setEnabled(false);
        contentImageView.setVisibility(View.GONE);

        contentsText = (EditText)findViewById(R.id.contentsEditText);

        tabArray = getResources().getStringArray(R.array.viewTabTextArray);
        tabArray2 = getResources().getStringArray(R.array.universityArray);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, tabArray);
        //adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,tabArray2);

        spinner = (Spinner)findViewById(R.id.spinner);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(onItemSelectedListener);
/*

        spinner2 = (Spinner)findViewById(R.id.spinner2);
        spinner2.setEnabled(false);
        spinner2.setVisibility(View.INVISIBLE);
        spinner2.setAdapter(adapter2);
        spinner2.setOnItemSelectedListener(onItemSelectedListener2);
*/

        Intent intent = getIntent();

        // 스피너 해당하는 탭과 대학교로 정해준다.
        int selectedTabPosition = intent.getExtras().getInt("selectedTabPosition");
        spinner.setSelection(selectedTabPosition);
        /*
        if(selectedTabPosition == 3){
            spinner2.setSelection(MyApp.selectedUnivPosition);
        }
*/
        // ViewFragment 에서 넘어온 경우
        isUpdate = intent.getBooleanExtra("isRevise", false);
        if(isUpdate){
            contentsText.setText(intent.getStringExtra("contents"));
            rowID = intent.getExtras().getLong("rowId");
            contentImagePath = intent.getStringExtra("contentImagePath");

            // 내용 이미지를 설정하지 않은 경우
            if(contentImagePath.contains("null") || contentImagePath.equals("")){
                contentImageView.setImageBitmap(null);
                contentImageView.setVisibility(View.GONE);
                // 내용 이미지를 설정한 경우
            }else{
                final String keyName_ConImage = "c" + contentImagePath.substring(
                        contentImagePath.lastIndexOf("/")+1, contentImagePath.lastIndexOf("."));
                new AsyncTask<Void, Void, Bitmap>(){
                    @Override
                    protected Bitmap doInBackground(Void... params) {
                        // 캐시에서 이미지를 가져옴
                        Bitmap result = MyApp.imageCache.getBimap(keyName_ConImage);
                        // 캐시에 이미지가 없을 경우
                        if(result == null){
                            try {
                                URL imageURL = new URL(contentImagePath);
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
                            contentImageView.setVisibility(View.VISIBLE);
                            contentImageView.setImageBitmap(bitmap);
                            contentImageView.setEnabled(true);
                        }else{
                            contentImageView.setVisibility(View.GONE);
                            contentImageView.setImageBitmap(null);
                            contentImageView.setEnabled(false);
                        }
                    }
                }.execute();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    // 버튼 클릭 리스너
    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.saveBtn :
                    AsyncTask<Object, Object, String> saveTask = new AsyncTask<Object, Object, String>() {
                        ProgressDialog progressDialog;
                        @Override
                        protected void onPreExecute() {
                            progressDialog = ProgressDialog.show(EditActivity.this, "글을 저장 중입니다.", " ");
                            saveBtn.setEnabled(false);
                        }

                        @Override
                        protected String doInBackground(Object... objects) {
                            if(isContents()) {
                                if (isUpdate) {
                                    update();
                                    return "글이 수정되었습니다.";
                                }else {
                                    save();
                                    return "글이 작성되었습니다.";
                                }
                            }
                            return "";
                        }
                        @Override
                        protected void onPostExecute(String result) {
                            progressDialog.cancel();
                            if(!result.equals(""))
                                Toast.makeText(EditActivity.this, result, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    };
                    saveTask.execute();
                    break;
                case R.id.imageBtn :
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "선택하기"), 1);
                    break;
                case R.id.editImageView :
                    deleteImageDialog();
                    break;
            }

        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if(picture != null){
            //picture.recycle();
        }
    }

    private boolean isContents(){
        return !contentsText.getText().toString().equals("");
    }
    private void save(){
        RequestHandler rh = new RequestHandler();

        // 현재 시간 구하기
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String dateStr = format.format(date);

        String content = contentsText.getText().toString();

        // 테이블이름, 아이디, 닉네임, 내용 등을 서버에 전송
        // 서버는 DB에 insert

        // 테이블명, ID, 닉네임(이름으로 임시 대체), 현재시각, 내용 순으로 DB에 입력
        HashMap<String, String> body = new HashMap<>();
        body.put("sel", MyApp.INSERT_INTO_TAB);
        body.put("id", MyApp.personId);
        body.put("nickname", MyApp.personName);
        body.put("tab_type", selectedTableName);
        body.put("userImage", MyApp.userImage);
        if(contentImageView.isEnabled()) body.put("conImage", "true");

        /*if(!spinner2.isEnabled()){
            body.put("university", "none");
        }else{
            body.put("university", selectedUniversityName);
        }*/
        body.put("content", content);
        body.put("date", dateStr);

        String responseMsg = rh.uploadFile(MyApp.urlStr, imagePath, body);
        //= rh.sendPostRequest(MyApp.urlStr, body);
    }

    private void update(){
        String content = contentsText.getText().toString();
        RequestHandler rh = new RequestHandler();

        HashMap<String, String> body = new HashMap<>();
        body.put("sel", MyApp.UPDATE_TAB);
        body.put("id", MyApp.personId);
        body.put("number", Long.toString(rowID));
        body.put("tab_type", selectedTableName);
        if(contentImageView.isEnabled()) body.put("conImage", "true");

        /*if(!spinner2.isEnabled()){
            body.put("university", "none");
        }else{
            body.put("university", selectedUniversityName);
        }*/
        body.put("content", content);

        String responseMsg = rh.uploadFile(MyApp.urlStr, imagePath, body);

        MyListFragment.isUpdate = true;
    }

    private void deleteImageDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("이미지를 삭제하시겠습니까?");
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new AsyncTask<Void, Void, String>(){
                    @Override
                    protected String doInBackground(Void... params) {
                        HashMap<String, String> data = new HashMap<String, String>();
                        data.put("sel", MyApp.DELETE_CONIMAGE);
                        data.put("number", Long.toString(rowID));

                        return new RequestHandler().sendPostRequest(MyApp.urlStr, data);
                    }

                    @Override
                    protected void onPostExecute(String str) {
                        contentImageView.setVisibility(View.GONE);
                        contentImageView.setImageBitmap(null);
                        contentImageView.setEnabled(false);
                    }
                }.execute();
            }
        });
        builder.create().show();
    }


    // 스피너 리스너 2
    AdapterView.OnItemSelectedListener onItemSelectedListener2 = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            selectedUniversityName = tabArray2[position];
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    };

    // 스피너 리스너
    AdapterView.OnItemSelectedListener onItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            selectedTableName = getResources().getStringArray(R.array.tableNameArray)[position];
            if(position > 2) {   // 대학교를 골라야할 탭이라면
                //spinner2.setEnabled(true);
                //spinner2.setVisibility(View.VISIBLE);
            }else if(position == 3){
                //spinner2.setEnabled(false);
                //spinner2.setVisibility(View.INVISIBLE);
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    };

    // 글 이미지 업로드
    // 관련 테이블 추가 (ContentImageTable)
    /*
        갤러리 호출 -> 이미지 저장
        EditActivity 에서 이미지를 추가할 때마다
            이미지 뷰에 뷰 설정 후 레이아웃에 addView
            이미지 저장 경로 스트링을 ArrayList에 저장
            올리기 버튼 -> 서버에 전송
        수정에서 EditActivity 로 넘어온 경우
            서버에서 쿼리 후 url을 json으로 받아서 파싱 후 이미지 로드
    */

    // 갤러리 -> 에디트(onActivityResult->onResume) -> ImageAcvitiy
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1 && resultCode==RESULT_OK && data != null) {
            try {
                Uri imgData = data.getData();
                picture = MediaStore.Images.Media.getBitmap(getContentResolver(), imgData);
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

                contentImageView.setImageBitmap(picture);
                contentImageView.setVisibility(View.VISIBLE);
                contentImageView.setEnabled(true);

               /* Intent intent = new Intent(this, ImageActivity.class);
                intent.putExtra("sel", 1);
                intent.putExtra("board_num", rowID);
                intent.putExtra("imagePath", imagePath);*/
                //startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }
}
