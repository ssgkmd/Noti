package com.example.simhyobin.noti;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.format.DateFormat;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Locale;

import static com.example.simhyobin.noti.MainActivity.mCredential;

public class NotiFireMessagingService extends FirebaseMessagingService {
    HttpTransport transport;
    JsonFactory jsonFactory;
    Calendar service;



    private static final String TAG = "NotiFirebaseMsgService";
    DBHelper dbhelper;
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "onMessageRecLog: " + remoteMessage.getData());


        Log.d(TAG, "From: " + remoteMessage.getFrom());
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            saveMessage(remoteMessage);
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            sendNotification(String.valueOf(remoteMessage.getData()));
        }
    }



    private void handleNow() {
        Log.d(TAG, "Short lived task is done.");
    }



    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send token to your app server.
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void saveMessage(RemoteMessage remoteMessage){
        dbhelper = new DBHelper(this, "data", null, 1);
        /*Log.d("saveMessage",messageBody);

        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new StringReader(messageBody));
        reader.setLenient(true);
        MessageResource messageResource = gson.fromJson(reader, MessageResource.class);*/

        String title = remoteMessage.getData().get("title");
        String content = remoteMessage.getData().get("content");
        String noti_time = remoteMessage.getData().get("noti_time");
        String author_nickname = remoteMessage.getData().get("author_nickname");
        dbhelper.save_message(remoteMessage.getData().get("hash_key"),title,content,Integer.parseInt(remoteMessage.getData().get("date")),Integer.parseInt(noti_time),remoteMessage.getData().get("author_id"),author_nickname);
        // title, content, date, noti_time, author_nickname
        GoogleAccountCredential cred = mCredential;


        transport= AndroidHttp.newCompatibleTransport();
        jsonFactory = JacksonFactory.getDefaultInstance();


        try{
            service = new com.google.api.services.calendar.Calendar.Builder(transport, jsonFactory, cred).setApplicationName("Noti").build();
            Event event = new Event()
                    .setSummary(title)
                    .setLocation("Noti")
                    .setDescription(content);

            java.util.Calendar cal = java.util.Calendar.getInstance(Locale.KOREA);
            cal.setTimeInMillis(Integer.parseInt(noti_time));
            String date = DateFormat.format("yyyy-MM-DDThh:mm:ss", cal).toString();

            DateTime startDateTime = new DateTime(date);
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone("Asia/Seoul");
            event.setStart(start);

            DateTime endDateTime = new DateTime(date);
            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime)
                    .setTimeZone("Asia/Seoul");
            event.setEnd(end);


            EventAttendee[] attendees = new EventAttendee[] {};
            event.setAttendees(Arrays.asList(attendees));

            EventReminder[] reminderOverrides = new EventReminder[] {};
            Event.Reminders reminders = new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Arrays.asList(reminderOverrides));
            event.setReminders(reminders);

            String calendarId = "primary";
            event = service.events().insert(calendarId, event).execute();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    private void sendNotification(String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);
        Log.d("message_test",messageBody);
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        String channelId = "Default";
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this,"default")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("메시지 도착")
                        .setContentText("ehdhdhdhdhckr")
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Default channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        Log.d("builder",String.valueOf(notificationBuilder));
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());

    }

}