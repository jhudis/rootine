package com.github.jhudis.rootine;

import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class AddTask extends AppCompatActivity {

    private int taskIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        Intent intent = getIntent();
        taskIndex = intent.getIntExtra(getString(R.string.task_id_extra), -1);
        Routine.Task task = intent.getParcelableExtra(getString(R.string.task_extra));

        if (task == null) {
            //Don't allow user to delete list_item (they can just press back)
            Button deleteButton = findViewById(R.id.delete_task);
            deleteButton.setVisibility(View.GONE);

            //The rest of this method is loading properties of a pre-existing list_item
            return;
        }

        EditText commandEditText = findViewById(R.id.command),
                durationEditText = findViewById(R.id.duration);

        commandEditText.setText(task.getCommand());
        durationEditText.setText(task.getDuration()+"");
    }

    public void done(View view) {
        EditText commandEditText = findViewById(R.id.command);
        EditText durationEditText = findViewById(R.id.duration);

        String command = commandEditText.getText().toString();
        if (command.trim().equals("")) command = getString(R.string.untitled_task);

        String durationString = durationEditText.getText().toString();
        int duration = durationString.equals("") ? 0 : Integer.parseInt(durationString);
        if (duration == 0) duration = 1;

        Routine.Task task = new Routine.Task(command, duration);

        Intent parentAddRoutine = NavUtils.getParentActivityIntent(this);
        parentAddRoutine.putExtra(getString(R.string.task_id_extra), taskIndex);
        parentAddRoutine.putExtra(getString(R.string.task_extra), task);
        //Go back up to the AddRoutine instance the user came from, without starting a new activity
        NavUtils.navigateUpTo(this, parentAddRoutine);
    }

    public void deleteTask(View view) {
        Intent parentAddRoutine = NavUtils.getParentActivityIntent(this);
        parentAddRoutine.putExtra(getString(R.string.task_id_extra), taskIndex);
        //Go back up to the AddRoutine instance the user came from, without starting a new activity
        NavUtils.navigateUpTo(this, parentAddRoutine);
    }

}
