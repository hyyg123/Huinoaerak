/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.yonggu.module2;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmListenerService;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";
    SharedPreferences sharedPreferences;

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String board_num = data.getString("board_num");
        String tab_type = data.getString("tab_type");
        String comment = data.getString("comment");
        String nickname = data.getString("nickname");

        Log.d(TAG, "From: " + from);
        Log.d(TAG, "board_num: " + board_num);
        Log.d(TAG, "tab_type: " + tab_type);
        Log.d(TAG, "comment: " + comment);
        Log.d(TAG, "nickname: " + nickname);

        if (from.startsWith("/topics/")) {
            // message received from some topic.
        } else {
            // normal downstream message.
        }

        // [START_EXCLUDE]
        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean sentToken = sharedPreferences.getBoolean(MyApp.SENT_TOKEN_TO_SERVER, false);
        boolean isNotification =
                sharedPreferences.getBoolean(getString(R.string.isNotification), false);

        Log.d(TAG, "" + sentToken);
        Log.d(TAG, "" + isNotification);
        if(sentToken && isNotification){
            sendNotification(board_num, tab_type, comment, nickname);
        }

        // [END_EXCLUDE]
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param number, table, comment GCM message received.
     */
    private void sendNotification(String number, String table, String comment, String nickname) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // ViewActivity에 필요한 데이터 전달하기
        // isByNotification = true
        intent.putExtra("board_num", number);
        intent.putExtra("tab_type", table);
        intent.putExtra("isByNotification", true);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.celebration_128_b)
                .setContentTitle(nickname)
                .setContentText(comment)
                .setTicker("새로운 댓글이 달렸습니다.")
                .setNumber(MyApp.COUNT_NOTIFICATION++)
                .setAutoCancel(true)
                //.setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        if(sharedPreferences.getBoolean(getString(R.string.isSound), false)){
            notificationBuilder.setSound(defaultSoundUri);
        }

        if(sharedPreferences.getBoolean(getString(R.string.isVibration), false)){
            long[] pattern = {0, 1500, 0, 0};
            notificationBuilder.setVibrate(pattern);
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        try {
            notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
        }catch (SecurityException e){
            e.printStackTrace();
        }
    }
}
