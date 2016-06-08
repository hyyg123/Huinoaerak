package com.example.yonggu.module2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.sql.Blob;

/**
 * Created by Yonggu on 2015-11-27.
 */

// UserContacts 데이터베이스의 생성과 연결을 쉽게 해준다.
public class DatabaseConnector {
    // 데이터베이스명
    private static final String DATABASE_NAME = "UserContacts";

    private static String TABLE_NAME1[] = {"Tip", "Cook", "Living"};
    private static String TABLE_NAME2[] = {"Collage", "My"};
    public static final String LOGIN_TABLE = "loginTable";
    public static final String COMMENT_TABLE = "commentTable";
    public static final String CONTACT_BY_TABLE   = "showBy";
    private SQLiteDatabase database;    // 데이터베이스오 연결을 위해
    private DatabaseOpenHelper databaseOpenHelper;  // 데이터베이스 생성

    //DatabaseConnector를 위한 public 생성자
    public DatabaseConnector(Context context){
        // 새로운 DatabaseOpenHelper 생성
        databaseOpenHelper = new DatabaseOpenHelper(context, DATABASE_NAME, null, 1);
    }

    // 데이터베이스 연결을 연다.
    public void open(){
        // 읽기 쓰기를 위해 데이터베이스를 생성하고 있다.
        database = databaseOpenHelper.getWritableDatabase();
    }

    // 데이터베이스 연결을 닫는다.
    public void close(){
        if(database != null)
            database.close();   // 데이터베이스 연결을 닫는다.
    }

    // 로그인 테이블에 튜플이 있으면 로그인되어있는 아이디와 닉네임을 반환한다.
    // 로그인 테이블에 튜플이 하나도 없을경우 로그인 된 상태가 아니므로 null을 반환한다.
    public Cursor getLoginInfo(){
        Cursor cursor;
        try{
            cursor = database.rawQuery("SELECT * FROM " + LOGIN_TABLE + " WHERE _id=1", null);
        }catch (SQLiteException e){
            return null;
        }

        return cursor;
    }

    // 로그인 시 로그인 테이블에 로그인한 아이디와 네임을 저장한다.
    public long insertLoginInfo(String userId, String nickName, String selectedUniv, String userImage){
        ContentValues newContact = new ContentValues();

        newContact.put("_id", 1);
        newContact.put("userId", userId);
        newContact.put("nickName", nickName);
        newContact.put("selectedUni", selectedUniv);
        newContact.put("userImage", MyApp.urlForImage + "/" + userImage);

        open();
        long rowID = database.insert(LOGIN_TABLE, null, newContact);
        close();
        return rowID;
    }

    public void updateLoginInfo(String nickName, String selectedUniv, String userImage){
        ContentValues updateContact = new ContentValues();

        updateContact.put("nickName", nickName);
        updateContact.put("selectedUni", selectedUniv);
        updateContact.put("userImage", MyApp.urlForImage + "/" + userImage);

        open();
        database.update(LOGIN_TABLE, updateContact, "_id=1", null);
        close();
    }

    // 로그아웃 시 로그인 테이블의 튜플을 모두 없앤다.
    public void deleteLoginInfo(){
        open();
        database.delete(LOGIN_TABLE, null, null);
        close();
    }

    // 데이터베이스에 새로운 연락처를 삽입한다.
    public long insertNormalContact(String tableName, int num, String userId, String name, String time,
                                    String contents, String conImage, String userImage, int comment_cnt){
        ContentValues newContact = new ContentValues();

        newContact.put("_id", num);
        newContact.put("userId", userId);
        newContact.put("name", name);
        newContact.put("time", time);
        newContact.put("contents", contents);
        newContact.put("comment_cnt", comment_cnt);
        if(!userImage.equals("null")) newContact.put("userImage", MyApp.urlForImage+ "/" + userImage);
        else newContact.put("userImage", "");
        if(!conImage.equals("null")) newContact.put("conImage", MyApp.urlForImage+ "/" + conImage);
        else newContact.put("conImage", "");

        open();     /// 데이터베이스를 연다.
        long rowID = database.insert(tableName, null, newContact);
        close();    // 데이터베이스를 닫는다.
        return rowID;
    }
    public long insertUniversityContact(String tableName, int num, String universityName, String userId, String name,
                                        String time, String contents, String conImage, String userImage, int comment_cnt){
        ContentValues newContact = new ContentValues();

        newContact.put("_id", num);
        newContact.put("university", universityName);
        newContact.put("userId", userId);
        newContact.put("name", name);
        newContact.put("time", time);
        newContact.put("contents", contents);
        newContact.put("comment_cnt", comment_cnt);
        if(!userImage.equals("null")) newContact.put("userImage", MyApp.urlForImage+ "/" + userImage);
        else newContact.put("userImage", "");
        if(!conImage.equals("null") || !conImage.equals("")) newContact.put("conImage", MyApp.urlForImage+ "/" + conImage);
        else newContact.put("conImage", "");

        open();     /// 데이터베이스를 연다.
        long rowID = database.insert(tableName, null, newContact);
        close();    // 데이터베이스를 닫는다.
        return rowID;
    }
    public long insertContactForShowById(int board_num, String univ, String tableName, String id, String nick, String date, String contents,
                                         String conImage, String userImage, int comment_cnt){
        ContentValues newContact = new ContentValues();
        /*
        sqLiteDatabase.execSQL("CREATE TABLE " + CONTACT_BY_TABLE + "(_id integer primary key, " +
                    "userId TEXT, name TEXT, time TEXT, contents TEXT, userImage TEXT, " +
                    "conImage TEXT, comment_cnt INTEGER, table_type TEXT);");
         */
        newContact.put("_id", board_num);
        newContact.put("userId", id);
        newContact.put("name", nick);
        newContact.put("table_type", tableName);    // 해당 아이디가 쓴 글이 어던 테이블인지 저장
        newContact.put("comment_cnt", comment_cnt);
        newContact.put("time", date);
        newContact.put("contents", contents);

        if(!userImage.equals("null")) newContact.put("userImage", MyApp.urlForImage+ "/" + userImage);
        else newContact.put("userImage", "");
        if(!conImage.equals("null") || !conImage.equals("")) newContact.put("conImage", MyApp.urlForImage+ "/" + conImage);
        else newContact.put("conImage", "");

        open();
        long rowID = database.insert(CONTACT_BY_TABLE, null, newContact);
        close();
        return rowID;
    }


    // 데이터베이스 내의 요리, 청소, 빨래 테이블 모두 출력
    public Cursor getNormalTableContacts(String tableName){
        return database.query(tableName, new String[] {"_id", "name", "time", "contents", "conImage", "userImage", "comment_cnt"}
                , null, null, null, null, "time DESC");
    }

    public Cursor getUniversityTableContacts(String tableName, String university){
        try {
            //return database.query(tableName, null, "university=" + university, null, null, null, null);
            return database.rawQuery("SELECT * FROM " + tableName + " WHERE university='" + university + "'" + " ORDER BY time DESC", null);
        }catch(SQLiteException e){
            return null;
        }
    }

    // 특정 연락처 정보가 포함된 Cursor를 반환한다.
    public Cursor getOneContact(long id, String tableName){
        return database.query(tableName, null, "_id=" + id, null, null, null, null);
    }

    // ㅇ이이거 뭐지.. ㅅㅂ 주석 왜 안적었니 옛날 용구야
    public boolean isBoardNum(String tableName, int board_num){
        boolean result = false;
        open();
        Cursor cursor = database.query(tableName, null, "_id=" + board_num, null, null, null, null);
        if(cursor.getCount() > 0){
            result = true;
        }else{
            result = false;
        }

        close();
        return result;
    }

    // 모든 테이블 지우기
    public void doEmptyTable(String tableName){
        open();
        database.delete(tableName, null, null);
        close();
    }

    public void doEmptyComments(int board_num){
        open();
        database.execSQL("DELETE FROM " + COMMENT_TABLE + " WHERE board_num='" + board_num + "';");
        close();
    }
    // 코멘트 삽입
    public void insertComment(String id, int board_num, String content, String date, String userImage, String nickName){
        ContentValues newContact = new ContentValues();

        newContact.put("userId", id);
        newContact.put("board_num", board_num);
        newContact.put("content", content);
        newContact.put("date", date);
        newContact.put("nickName", nickName);
        if(!userImage.equals("null")) newContact.put("userImage", MyApp.urlForImage+ "/" + userImage);
        else newContact.put("userImage", "");

        open();     /// 데이터베이스를 연다.
        long rowID = database.insert(COMMENT_TABLE, null, newContact);
        close();    // 데이터베이스를 닫는다.
    }

    // 코멘트 불러오기
    public Cursor getComments(int board_num){
        return database.query(COMMENT_TABLE, null, "board_num=" + board_num, null, null, null, "date DESC");
        //return database.rawQuery("SELECT * FROM " + COMMENT_TABLE + " WHERE board_num=" + board_num + " " + "ORDER BY TIME DESC", null);
    }

    public Cursor getContactById(){
        return database.rawQuery("SELECT * FROM " + CONTACT_BY_TABLE  + " ORDER BY time DESC", null);
    }

    /*

    //데이터베이스 내의 기존 연락처를 수정한다.
    public void updateContact(long id, String tableName, String colName, String contents){
        ContentValues editContact = new ContentValues();
        editContact.put(colName, contents);

        open();     /// 데이터베이스를 연다.
        database.update(tableName, editContact, "_id=" + id, null);
        close();    // 데이터베이스를 닫는다.
    }

*/


    private class DatabaseOpenHelper extends SQLiteOpenHelper{
        // 생성자
        public DatabaseOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
            super(context, name, factory, version);
        }

        //데이터베이스가 처음 생성되었을 때 호출된다.
        //(앱을 처음 설치하였을 떄 한번만 호출됨)
        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            // 테이블 5개 생성
            // 탭별로 테이블 생성
            for(int i=0; i<TABLE_NAME1.length; i++){
                sqLiteDatabase.execSQL(
                        "CREATE TABLE " + TABLE_NAME1[i] +
                        "(_id integer primary key, " +
                        "userId TEXT, name TEXT, time TEXT, contents TEXT, userImage TEXT, " +
                                "conImage TEXT, comment_cnt INTEGER);");
            }
            for( int i=0; i<TABLE_NAME2.length;i++){
                sqLiteDatabase.execSQL(
                        "CREATE TABLE " + TABLE_NAME2[i] +
                                "(_id integer primary key, " +
                                "university TEXT, userId TEXT, name TEXT, time TEXT, " +
                                "contents TEXT, userImage TEXT, conImage TEXT, comment_cnt INTEGER);"
                );
            }
            sqLiteDatabase.execSQL(
                    "CREATE TABLE " + LOGIN_TABLE +
                            "(_id integer primary key, userId TEXT, nickName TEXT, selectedUni TEXT, userImage TEXT);"
            );
            sqLiteDatabase.execSQL("CREATE TABLE " + COMMENT_TABLE + "(_id integer primary key, userId TEXT," +
                    " nickName TEXT, userImage TEXT, board_num INT, content TEXT, date TEXT);");
            // 내가 쓴글 보기 디비
            sqLiteDatabase.execSQL("CREATE TABLE " + CONTACT_BY_TABLE + "(_id integer primary key, " +
                    "userId TEXT, name TEXT, time TEXT, contents TEXT, userImage TEXT, " +
                    "conImage TEXT, comment_cnt INTEGER, table_type TEXT);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        }
    }
}


