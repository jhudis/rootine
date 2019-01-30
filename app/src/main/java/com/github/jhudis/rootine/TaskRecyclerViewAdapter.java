package com.github.jhudis.rootine;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.jhudis.rootine.TaskFragment.OnListFragmentInteractionListener;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link com.github.jhudis.rootine.Routine.Task} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class TaskRecyclerViewAdapter extends RecyclerView.Adapter<TaskRecyclerViewAdapter.ViewHolder> {

    private final List<Routine.Task> mValues;
    private final OnListFragmentInteractionListener mListener;

    public TaskRecyclerViewAdapter(List<Routine.Task> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Routine.Task task = mValues.get(position);
        holder.mTask = task;
        int duration = task.getDuration();
        holder.mDurationView.setText(duration + " min" + (duration == 1 ? "" : "s"));
        holder.mCommandView.setText(task.getCommand());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mTask);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mDurationView;
        public final TextView mCommandView;
        public Routine.Task mTask;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mDurationView = view.findViewById(R.id.duration);
            mCommandView = view.findViewById(R.id.command);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mCommandView.getText() + "'";
        }
    }
}
