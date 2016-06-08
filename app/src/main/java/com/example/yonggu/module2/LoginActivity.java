package com.example.yonggu.module2;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by Yonggu on 2015-11-30.
 */
public class LoginActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener{

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final int RC_SIGN_IN = 9001;
    private Button signInButton;
    final private DatabaseConnector dc = new DatabaseConnector(this);
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        setContentView(R.layout.activity_login);

        TextView backBtn = (TextView)findViewById(R.id.back);
        backBtn.setOnClickListener(this);

        // uri 가져오기
        signInButton = (Button)findViewById(R.id.sign_button);

        // 로그인
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog = ProgressDialog.show(LoginActivity.this, "LOADING", "로딩 중입니다", false);
                MyApp.mGoogleApiClient = new GoogleApiClient.Builder(LoginActivity.this)
                        .addConnectionCallbacks(LoginActivity.this)
                        .addOnConnectionFailedListener(LoginActivity.this)
                        .addApi(Plus.API)
                        .addScope(new Scope(Scopes.PROFILE))
                        .build();
                MyApp.mGoogleApiClient.connect();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.back :
                finish();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            MyApp.mGoogleApiClient = new GoogleApiClient.Builder(LoginActivity.this)
                    .addConnectionCallbacks(LoginActivity.this)
                    .addOnConnectionFailedListener(LoginActivity.this)
                    .addApi(Plus.API)
                    .addScope(new Scope(Scopes.PROFILE))
                    .build();
            MyApp.mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Person currentPerson = Plus.PeopleApi.getCurrentPerson(MyApp.mGoogleApiClient);
        //String accountName = Plus.AccountApi.getAccountName(mGoogleApiClient);
        //Account account = new Account(accountName, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);

        if(currentPerson != null){
            MyApp.loginBool = true;
            MyApp.personId = currentPerson.getId();
            MyApp.personName = currentPerson.getDisplayName();
            new AsyncTask<Object, Object, Object>(){
                RequestHandler rh = new RequestHandler();
                @Override
                protected Object doInBackground(Object... objects) {
                    // 동시에 서버 DB에 저장
                    // http 통신
                    try{
                        // http body str to send
                        HashMap<String, String> body = new HashMap<>();
                        body.put("sel", MyApp.CHECK_LOGIN_INFO);
                        body.put("id", MyApp.personId);
                        body.put("nickname", MyApp.personName);

                        String responseMsg = rh.sendPostRequest(MyApp.urlStr, body);

                        // 처음 가입하는 경우 받을 데이터가 없으므로 리턴
                        if(responseMsg.equals("first")){
                            return null;
                        }

                        // json parsing
                        JSONObject jsonResponse = new JSONObject(responseMsg);

                        // root of json
                        JSONArray jsonRootNode = jsonResponse.optJSONArray("login_info");

                        // get value
                        int length = jsonRootNode.length();
                        for(int i=0; i<length; i++){
                            JSONObject childNode = jsonRootNode.getJSONObject(i);
                            //MyApp.personId = childNode.optString("id").toString();
                            MyApp.personName = childNode.optString("nickname").toString();
                            MyApp.selectedUniv = childNode.optString("university").toString();
                            MyApp.userImage = childNode.optString("userImage").toString();
                        }
                    }catch (JSONException e){
                        e.printStackTrace();
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    // 로그인 정보를 클라이언트의 DB에 저장 ( 쿠키 역할을 한다 )
                    dc.insertLoginInfo(MyApp.personId, MyApp.personName, MyApp.selectedUniv, MyApp.userImage);

                    return null;
                }

                @Override
                protected void onPostExecute(Object o) {
                    showMessage();
                    dialog.cancel();

                    if (checkPlayServices() && MyApp.loginBool) {
                        // Start IntentService to register this application with GCM.
                        //Log.i(TAG, MyApp.personId);
                        Intent intent = new Intent(LoginActivity.this, RegistrationIntentService.class);
                        startService(intent);
                    }

                    finish();
                }
            }.execute();
        }
        else{
            MyApp.init();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {}

    // 연결에 실패하였을 때
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if(connectionResult.hasResolution()){
            try {
                connectionResult.startResolutionForResult(this, RC_SIGN_IN);
                MyApp.init();   //  초기화
            }
            catch(IntentSender.SendIntentException e){
                e.printStackTrace();
            }
        }else{}
    }

    @Override
    public void finish() {
        super.finish();
        this.overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    private void showMessage(){
        Toast.makeText(this, "로그인 되었습니다", Toast.LENGTH_SHORT).show();
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                //Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
}
