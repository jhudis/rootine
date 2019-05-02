package com.github.jhudis.rootine;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;

public class ManageRoutines extends AppCompatActivity implements RoutineFragment.OnListFragmentInteractionListener {

    private ArrayList<Routine> routines = new ArrayList<>();
    private RoutinesDbAdapter mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_routines);

        mDbHelper = new RoutinesDbAdapter(this);
        mDbHelper.open();

        routines.clear();
        routines.addAll(mDbHelper.getAllRoutines());

        mDbHelper.close();
    }

    public void addRoutine(View view) {
        Intent addRoutine = new Intent(this, AddRoutine.class);
        //-1: new routine, otherwise: editing existing routine
        addRoutine.putExtra(getString(R.string.routine_id_extra), -1);
        startActivity(addRoutine);
    }

    public ArrayList<Routine> getRoutines() {
        return routines;
    }

    @Override
    public void onListFragmentInteraction(Routine routine) {
        //This happens when you click on a routine
        //For updating routines
        Intent updateRoutine = new Intent(this, AddRoutine.class);
        updateRoutine.putExtra(getString(R.string.routine_id_extra), routine.getId());
        startActivity(updateRoutine);
    }
}
