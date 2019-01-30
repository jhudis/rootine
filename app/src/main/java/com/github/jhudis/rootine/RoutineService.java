package com.github.jhudis.rootine;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class RoutineService extends IntentService {

    private RoutinesDbAdapter mDbHelper;
    private TextToSpeech tts;

    public RoutineService() {
        super("RoutineService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDbHelper = new RoutinesDbAdapter(this);
    }

    private void speak(TextToSpeech tts, String string) {
        tts.speak(string, TextToSpeech.QUEUE_FLUSH, null, getString(R.string.text_to_speech_id));
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int routineId = intent.getIntExtra(getString(R.string.routine_id_extra), -1);
        mDbHelper.open();
        Routine routine = mDbHelper.getRoutine(routineId);
        routine.setLastRun(new Routine.Date());
        mDbHelper.updateRoutine(routineId, routine);
        mDbHelper.close();

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);
                }
            }
        });

        //Text-to-speech needs some time to start up
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String notificationText = "Running routine \"" + routine.getName() + "\"";
        Intent notificationIntent = new Intent(this, Home.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        updateNotification(getText(R.string.notification_title).toString(), notificationText, pendingIntent);

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preferences_file), Context.MODE_PRIVATE);
        String name = sharedPref.getString(getText(R.string.user_name).toString(), "");

        String command, toSpeak, notificationTitleSub, notificationTitle;
        int duration, minutesLeft, secondsLeft;
        for (Routine.Task task : routine.getTasks()) {
            command = task.getCommand();
            duration = task.getDuration();

            command = command.substring(0, 1).toLowerCase() + command.substring(1);
            notificationTitleSub = " left to " + command;
            toSpeak = name + ", you have " + duration + " minute" + (duration == 1 ? "" : "s") + " to " + command + ".";
            speak(tts, toSpeak);

            minutesLeft = duration;
            secondsLeft = 0;
            for (int i = 0; i < duration * 60; i++) {
                notificationTitle = minutesLeft + ":" + format2Digit(secondsLeft) + notificationTitleSub;
                updateNotification(notificationTitle, notificationText, pendingIntent);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                secondsLeft--;
                if (secondsLeft == -1) {
                    secondsLeft = 59;
                    minutesLeft--;
                }

                if (duration > 1 && minutesLeft == 1 && secondsLeft == 0) {
                    toSpeak = name + ", you have one minute left to " + command + ".";
                    speak(tts, toSpeak);
                }
            }
        }
        String doneMessage = "You're done, " + name + "!";
        updateNotification(doneMessage, notificationText, pendingIntent);
        speak(tts, doneMessage);
        try {
            Thread.sleep(1000); //Let the TTS finish talking
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        stopSelf();
    }

    private void updateNotification(String title, String text, PendingIntent pendingIntent) {
        Notification notification =
                new Notification.Builder(this, getString(R.string.notification_channel_id))
                        .setContentTitle(title)
                        .setContentText(text)
                        .setSmallIcon(R.drawable.ic_notification_logo)
                        .setColor(getColor(R.color.colorPrimary))
                        .setContentIntent(pendingIntent)
                        .setChannelId(getString(R.string.notification_channel_id))
                        .setOnlyAlertOnce(true)
                        .build();
        startForeground(1, notification);
    }

    private String format2Digit(int i) {
        return String.format("%02d", i);
    }

}
