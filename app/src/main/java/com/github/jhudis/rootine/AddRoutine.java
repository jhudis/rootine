package com.github.jhudis.rootine;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

public class AddRoutine extends AppCompatActivity implements TaskFragment.OnListFragmentInteractionListener {

    private ArrayList<Routine.Task> tasks = new ArrayList<>();
    private int routineId;

    private RoutinesDbAdapter mDbHelper;

    private TimePickerDialog timePickerDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_routine);

        //Set up time selection widget
        final EditText startTimeEditText = findViewById(R.id.start_time);
        startTimeEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] am_pm = new String[1];
                timePickerDialog = new TimePickerDialog(AddRoutine.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        am_pm[0] = hourOfDay >= 12 ? "AM" : "PM";
                        if (hourOfDay > 12) hourOfDay -= 12;
                        startTimeEditText.setText(hourOfDay + ":" + String.format("%02d", minute) + " " + am_pm[0]);
                    }
                }, 0, 0, false);

                //Start the widget at the previously selected time
                Routine.Time previousTime = new Routine.Time(startTimeEditText.getText().toString());
                timePickerDialog.updateTime(previousTime.getHour(), previousTime.getMinute());

                timePickerDialog.show();
            }
        });

        mDbHelper = new RoutinesDbAdapter(this);
        mDbHelper.open();

        Intent intent = getIntent();
        routineId = intent.getIntExtra(getString(R.string.routine_id_extra), -2);

        //This means we're making a new routine
        if (routineId == -1) {
            //Don't allow user to delete routine (they can just press back)
            Button deleteButton = findViewById(R.id.delete_routine);
            deleteButton.setVisibility(View.GONE);

            //The rest of this method is loading properties of a pre-existing routine
            return;
        }

        EditText nameEditText = findViewById(R.id.name);
        ToggleButton[] dayButtons = {findViewById(R.id.sunday), findViewById(R.id.monday),
                findViewById(R.id.tuesday), findViewById(R.id.wednesday), findViewById(R.id.thursday),
                findViewById(R.id.friday), findViewById(R.id.saturday)};
        CheckBox timedCheckBox = findViewById(R.id.timed);

        //Get routine from database and load its properties into the inputs
        Routine routine = mDbHelper.getRoutine(routineId);
        nameEditText.setText(routine.getName());
        if (routine.getStartTime() != null)
            startTimeEditText.setText(routine.getStartTime().toString12Hr());
        for (int i = 0; i < 7; i++)
            dayButtons[i].setChecked(routine.getDaysActive()[i]);
        if (routine.getStartTime() != null) {
            timedCheckBox.setChecked(true);
            enableDisableTimeEditText(true);
        }
        List<Routine.Task> tasksTemp = routine.getTasks();
        if (tasksTemp != null) {
            tasks.clear();
            tasks.addAll(tasksTemp);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        //This method accounts for when a list_item is added, removed, or modified
        //(since doing the above does not create a new activity; the user goes back to the one they were using)

        int taskIndex = intent.getIntExtra(getString(R.string.task_id_extra), -2);
        Routine.Task task = intent.getParcelableExtra(getString(R.string.task_extra));

        if (taskIndex == -2) {
            return;
        } else if (taskIndex == -1) {
            tasks.add(task);
        } else if (task == null) {
            tasks.remove(taskIndex);
        } else {
            tasks.set(taskIndex, task);
        }
    }

    private void enableDisableTimeEditText(boolean checked) {
        EditText startTimeEditText = findViewById(R.id.start_time);
        if (checked) {
            startTimeEditText.setEnabled(true);
            startTimeEditText.setInputType(InputType.TYPE_DATETIME_VARIATION_TIME);
        } else {
            startTimeEditText.setEnabled(false);
            startTimeEditText.setInputType(InputType.TYPE_NULL);
        }
    }

    public void onTimedCheckboxClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        enableDisableTimeEditText(checked);
    }

    public void addTask(View view) {
        Intent addTask = new Intent(this, AddTask.class);
        startActivity(addTask);
    }

    public void done(View view) {
        EditText nameEditText = findViewById(R.id.name),
                startTimeEditText = findViewById(R.id.start_time);
        ToggleButton[] dayButtons = {findViewById(R.id.sunday), findViewById(R.id.monday),
                findViewById(R.id.tuesday), findViewById(R.id.wednesday), findViewById(R.id.thursday),
                findViewById(R.id.friday), findViewById(R.id.saturday)};
        CheckBox timedCheckBox = findViewById(R.id.timed);

        String name = nameEditText.getText().toString();
        if (name.equals("")) name = getString(R.string.untitled_routine);

        boolean[] daysActive = new boolean[7];
        for (int i = 0; i < 7; i++)
            daysActive[i] = dayButtons[i].isChecked();

        boolean isTimed = timedCheckBox.isChecked();
        String timeString = startTimeEditText.getText().toString();
        Routine.Time startTime = isTimed ? new Routine.Time(timeString) : null;

        Routine routine = new Routine(name, daysActive, startTime, tasks);

        if (routineId == -1) //We're making a new routine
            routineId = (int) mDbHelper.createRoutine(routine);
        else //We're updating a routine
            mDbHelper.updateRoutine(routineId, routine);
        routine.setId(routineId);
        mDbHelper.close();

        routine.setAlarms(this);

        Intent manageRoutines = new Intent(this, ManageRoutines.class);
        startActivity(manageRoutines);
    }

    public void deleteRoutine(View view) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_routine_dialogue_title))
                .setMessage(getString(R.string.delete_routine_dialogue_message))
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Routine routine = mDbHelper.getRoutine(routineId);
                        routine.cancelAlarms(AddRoutine.this);

                        mDbHelper.deleteRoutine(routineId);

                        mDbHelper.close();

                        Intent manageRoutines = new Intent(AddRoutine.this, ManageRoutines.class);
                        startActivity(manageRoutines);
                    }
                }).create().show();
    }

    public List<Routine.Task> getTasks() {
        return tasks;
    }

    @Override
    public void onListFragmentInteraction(Routine.Task task) {
        //This happens when you click on a list_item

        int taskIndex = -1;
        for (int i = 0; i < tasks.size(); i++)
            if (tasks.get(i) == task)
                taskIndex = i;

        Intent addTask = new Intent(this, AddTask.class);
        addTask.putExtra(getString(R.string.task_id_extra), taskIndex);
        addTask.putExtra(getString(R.string.task_extra), task);
        startActivity(addTask);
    }
}
