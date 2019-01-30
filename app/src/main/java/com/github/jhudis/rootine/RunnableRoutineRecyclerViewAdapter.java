package com.github.jhudis.rootine;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.jhudis.rootine.RunnableRoutineFragment.OnListFragmentInteractionListener;
import com.github.jhudis.rootine.dummy.DummyContent.DummyItem;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Routine} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class RunnableRoutineRecyclerViewAdapter extends RecyclerView.Adapter<RunnableRoutineRecyclerViewAdapter.ViewHolder> {

    private final List<Routine> mValues;
    private final OnListFragmentInteractionListener mListener;

    public RunnableRoutineRecyclerViewAdapter(List<Routine> routines, OnListFragmentInteractionListener listener) {
        mValues = routines;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_runnableroutine, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Routine routine = mValues.get(position);
        holder.mRoutine = routine;
        holder.mNameView.setText(routine.getName());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mRoutine);
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
        public final TextView mNameView;
        public Routine mRoutine;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mNameView = view.findViewById(R.id.name);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mNameView.getText() + "'";
        }
    }
}
