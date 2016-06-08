package com.example.yonggu.module2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.plus.Plus;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by Yonggu on 15. 12. 15..
 */

// 마지막 탭의 마이페이지가 있는 액티비티
public class MyTabFragment extends ListFragment implements View.OnClickListener{
    private ArrayAdapter<String> adapter;
    private ListView listView;
    private TextView nickNameTextView;
    private ImageView imageView;
    private String imageForSend;
    private Bitmap pBitmap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.mytab_fragment, container, false);
        nickNameTextView = (TextView)layout.findViewById(R.id.nickNmaeTextView);
        nickNameTextView.setOnClickListener(this);
        imageView = (ImageView)layout.findViewById(R.id.profileImage);
        imageView.setOnClickListener(this);

        return layout;
    }

    // after view created, call
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = getListView();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0 :    // 내가 쓴 글 보기
                        if(MyApp.loginBool){
                            Intent intent = new Intent(getActivity(), RealShowBoardActivity.class);
                            intent.putExtra("id", MyApp.personId);
                            startActivity(intent);
                        }else{
                            startActivity(new Intent(getActivity(), LoginActivity.class));
                        }

                        break;
                    case 1 :    // 닉네임 변경하기
                        if(MyApp.loginBool){
                            setNickNameDialog();
                        }else{
                            startActivity(new Intent(getActivity(), LoginActivity.class));
                        }
                        break;
                    case 2 :    // 프로필 사진 변경하기
                        if(MyApp.loginBool){
                            setProfileImage();
                        }else{
                            startActivity(new Intent(getActivity(), LoginActivity.class));
                        }
                        break;
                    case 3 :    // 로그인 & 로그아웃
                        if(MyApp.loginBool){    // 로그아웃일떄
                            logoutDialog();
                        }else{  // 로그인일떄
                            startActivity(new Intent(getActivity(), LoginActivity.class));
                        }
                        break;
                }
            }
        });

        adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);

        adapter.add("내가 쓴 글 보기");
        adapter.add("닉네임 변경하기");
        adapter.add("프로필 사진 변경하기");
        adapter.add("로그아웃");

        setListAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(MyApp.loginBool) {
            adapter.remove("로그아웃");
            adapter.remove("로그인");
            adapter.add("로그아웃");
        }
        else {
            adapter.remove("로그인");
            adapter.remove("로그아웃");
            adapter.add("로그인");
        }
        nickNameTextView.setText(MyApp.personName);

        //bitmap = RequestHandler.getImage(MyApp.urlForImage + "/" + MyApp.userImage);

        // 로그인 유저의 이미지 결정
        final String userImagePath = MyApp.urlForImage + "/" + MyApp.userImage;
        imageForSend = userImagePath;

        if(userImagePath.equals("") || userImagePath.contains("null")){
            imageView.setImageResource(R.drawable.user);
            // 이미지 설정을 한 경우
        }else{
            final String keyName_UserImage = "c" + userImagePath.substring(
                    userImagePath.lastIndexOf("/")+1, userImagePath.lastIndexOf("."));
            Log.i("keyName" , keyName_UserImage);
            new AsyncTask<String, Void, Bitmap>(){
                @Override
                protected Bitmap doInBackground(String... params) {
                    // 캐시에서 이미지를 가져옴
                    Bitmap result = MyApp.imageCache.getBimap(keyName_UserImage);
                    // 캐시에 이미지가 없을 경우
                    if(result == null){
                        try {
                            URL imageURL = new URL(userImagePath);
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
                            MyApp.imageCache.put(keyName_UserImage, result);

                            // 전에 캐시에 저장되어 있는 이미지 지우기
                            String temp_str = keyName_UserImage.substring(
                                    1, keyName_UserImage.lastIndexOf("_"));
                            int user_cnt = Integer.parseInt(temp_str) - 1;

                            temp_str = keyName_UserImage.replaceFirst(temp_str, Integer.toString(user_cnt));
                            boolean re = MyApp.imageCache.reomveKey(temp_str);
                        }
                    }
                    return result;
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    if(bitmap != null){
                        imageView.setImageBitmap(bitmap);
                        pBitmap = bitmap;
                    }else {
                        imageView.setImageResource(R.drawable.user);
                    }
                }
            }.execute();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if(pBitmap != null) {
            //pBitmap.recycle();
        }
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        switch (viewId){
            // 프로필 사진 클릭시 변경
            case R.id.profileImage :
                if(MyApp.loginBool){
                    Intent intent = new Intent(getActivity(), ProfileViewActivity.class);
                    intent.putExtra("id", MyApp.personId);
                    intent.putExtra("nickName", MyApp.personName);
                    intent.putExtra("userImage", imageForSend);
                    startActivity(intent);
                }else{
                    startActivity(new Intent(getActivity(), LoginActivity.class));
                }
                break;
            // 닉네임 변경
            case R.id.nickNmaeTextView :
                if(MyApp.loginBool){
                    setNickNameDialog();
                }else{
                    startActivity(new Intent(getActivity(), LoginActivity.class));
                }
                break;
        }
    }

    // 갤러리를 불러온다
    private void setProfileImage(){
        final CharSequence items[] = {"기본 이미지", "사진 선택하기"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("프로필 사진");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    // 기본 이미지로 변경
                    case 0:
                        new AsyncTask<Void, Void, Void>(){
                            @Override
                            protected Void doInBackground(Void... params) {
                                HashMap<String, String> data = new HashMap<String, String>();
                                data.put("sel", MyApp.DELETE_IMAGE_USERLIST);
                                data.put("id", MyApp.personId);
                                new RequestHandler().sendPostRequest(MyApp.urlStr, data);
                                MyApp.userImage = "null";
                                final DatabaseConnector dc = new DatabaseConnector(getActivity());
                                dc.updateLoginInfo(MyApp.personName, MyApp.selectedUniv, MyApp.userImage);
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                startActivity(new Intent(getActivity(), TempActivity.class));
                            }
                        }.execute();
                        break;
                    // 이미지 선택하기 위해 MainActivity에서 갤러리 호출
                    case 1:
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        getActivity().startActivityForResult(Intent.createChooser(intent, "선택하기"), 1);
                        break;
                }
            }
        });
        builder.show();

    }

    // 닉네임을 정하기 위한 다이얼로그
    private void setNickNameDialog(){
        final EditText input = new EditText(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("변경할 닉네임을 입력하세요.");
        builder.setView(input);
        builder.setPositiveButton("변경", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 밑의 onClick에서 오버라이드함 ( 다이얼로그가 종료되지않게 하기 위해서
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //dialog.cancel();
            }
        });
        final AlertDialog d = builder.create();
        d.setOnShowListener(new DialogInterface.OnShowListener(){
            @Override
            public void onShow(DialogInterface dialog) {
                Button btn = d.getButton(AlertDialog.BUTTON_POSITIVE);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String updatedName = input.getText().toString();
                        if(updatedName == "" || updatedName.length() < 2){
                            Toast.makeText(getActivity(), "두 글자 이상 입력하세요", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if(updatedName.length() > 10){
                            Toast.makeText(getActivity(), "10글자 이하로 입력하세요", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        new AsyncTask<Void, Void, String>() {
                            RequestHandler rh = new RequestHandler();
                            // 기존의 닉네임과 유니크한지 검사한다
                            @Override
                            protected String doInBackground(Void... params) {
                                HashMap<String, String> body = new HashMap<>();
                                body.put("sel",MyApp.CHECK_UPDATE_NICK);
                                body.put("id", MyApp.personId);
                                body.put("nickname", updatedName);

                                return rh.sendPostRequest(MyApp.urlStr, body);
                            }
                            @Override
                            protected void onPostExecute(String str) {
                                if(str.equals("") || str.contains(" ")) {
                                    MyApp.personName = updatedName;
                                    nickNameTextView.setText(updatedName);
                                    final DatabaseConnector dc = new DatabaseConnector(getActivity());
                                    dc.updateLoginInfo(updatedName, MyApp.selectedUniv, MyApp.userImage);
                                    Toast.makeText(getActivity(), "닉네임이 변경되었습니다", Toast.LENGTH_SHORT).show();
                                    d.cancel();
                                }else {
                                    Toast.makeText(getActivity(), str, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }.execute();
                    }
                });
            }
        });
        d.show();
    }

    private void logoutDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("정말 로그아웃 하시겠습니까?");
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // 로그아웃 처리
                if (MyApp.mGoogleApiClient != null) {
                    Plus.AccountApi.clearDefaultAccount(MyApp.mGoogleApiClient);
                    Plus.AccountApi.revokeAccessAndDisconnect(MyApp.mGoogleApiClient);
                    MyApp.mGoogleApiClient.disconnect();
                    MyApp.mGoogleApiClient = null;
                }

                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(getActivity());
                sharedPreferences.edit().putBoolean(MyApp.SENT_TOKEN_TO_SERVER, false).apply();

                // Activity가 생성된 이후에 객체를 생성해야 null이 안된다
                final DatabaseConnector dc = new DatabaseConnector(getActivity());
                dc.deleteLoginInfo();
                MyApp.init();
                showMessage();
                adapter.remove("로그아웃");
                adapter.remove("로그인");
                adapter.add("로그인");
            }
        });
        builder.create().show();
    }
    private void showMessage(){
        Toast.makeText(getActivity(), "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();
        nickNameTextView.setText(" ");
        imageView.setImageResource(R.drawable.user);
    }
}
