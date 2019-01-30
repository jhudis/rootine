package com.github.jhudis.rootine;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Home extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        createNotificationChannel();

        //We want to use the user's name for running routines
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preferences_file), Context.MODE_PRIVATE);
        String name = sharedPref.getString(getString(R.string.user_name), "");
        if (name.equals("")) {
            Intent userInfo = new Intent(this, UserInfo.class);
            startActivity(userInfo);
        }

        mDbHelper = new RoutinesDbAdapter(this);
        mDbHelper.open();
        Routine nextRunnableRoutine = mDbHelper.getNextRunnableRoutine();
        mDbHelper.close();
        Button runRoutineButton = findViewById(R.id.run_routine_button);
        if (nextRunnableRoutine == null) {
            runRoutineButton.setEnabled(false);
            return;
        }
        nextRunnableRoutineId = nextRunnableRoutine.getId();
        String buttonText = "Run \"" + nextRunnableRoutine.getName() + "\"";
        runRoutineButton.setText(buttonText);
    }

    private RoutinesDbAdapter mDbHelper;

    private int nextRunnableRoutineId;

    private void createNotificationChannel() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager.getNotificationChannels().size() > 0)
            return;

        CharSequence name = getString(R.string.notification_channel_name);
        String description = getString(R.string.notification_channel_description);
        String id = getString(R.string.notification_channel_id);
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(id, name, importance);
        channel.setDescription(description);
        notificationManager.createNotificationChannel(channel);
    }

    public void manageRoutines(View view) {
        Intent manageRoutines = new Intent(this, ManageRoutines.class);
        startActivity(manageRoutines);
    }

    public void runNextRoutine(View view) {
        Intent runRoutine = new Intent(this, RoutineService.class);
        runRoutine.putExtra(getString(R.string.routine_id_extra), nextRunnableRoutineId);
        startService(runRoutine);

        Intent refresh = new Intent(this, Home.class);
        startActivity(refresh);
    }
}
