package com.github.jhudis.rootine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

public class OnBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        RoutinesDbAdapter mDbHelper = new RoutinesDbAdapter(context);
        mDbHelper.open();
        List<Routine> routines = mDbHelper.getAllRoutines();
        for (Routine routine : routines)
            routine.setAlarms(context);
    }
}
