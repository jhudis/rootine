package com.github.jhudis.rootine;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

public class Home extends AppCompatActivity implements RunnableRoutineFragment.OnListFragmentInteractionListener {

    private ArrayList<Routine> routines = new ArrayList<>();
    private RoutinesDbAdapter mDbHelper;

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

        routines.clear();
        routines.addAll(mDbHelper.getRunnableRoutines());

        mDbHelper.close();

        if (routines.size() == 0)
            findViewById(R.id.routine_list).setVisibility(View.GONE);
        else
            findViewById(R.id.no_runnable_routines).setVisibility(View.GONE);
    }

    public ArrayList<Routine> getRoutines() {
        return routines;
    }

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

    @Override
    public void onListFragmentInteraction(Routine routine) {
        Intent runRoutine = new Intent(this, RoutineService.class);
        runRoutine.putExtra(getString(R.string.routine_id_extra), routine.getId());
        startService(runRoutine);
    }
}
