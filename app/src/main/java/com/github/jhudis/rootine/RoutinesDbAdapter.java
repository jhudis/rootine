package com.github.jhudis.rootine;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoutinesDbAdapter {

    public static final String KEY_NAME = "name";
    public static final String KEY_DAYS_ACTIVE = "days_active";
    public static final String KEY_START_TIME = "start_time";
    public static final String KEY_PRIORITY = "priority";
    public static final String KEY_TASKS = "tasks";
    public static final String KEY_LAST_RUN = "last_run";
    public static final String KEY_ROWID = "_id";

    private static final String TAG = "RoutinesDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
            "create table routines (_id integer primary key autoincrement, "
                    + "name text not null, days_active integer, start_time integer, priority integer, tasks text not null, last_run integer);";

    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "routines";
    private static final int DATABASE_VERSION = 4;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS routines");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     *
     * @param ctx the Context within which to work
     */
    public RoutinesDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the routines database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     *
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public RoutinesDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }


    /**
     * Create a new routine using the name, days active, start time, priority, tasks, and last run date provided.
     * If the routine is successfully created return the new rowId for that routine, otherwise return
     * a -1 to indicate failure.
     *
     * @param name the name of the routine
     * @param daysActive the days for which the routine is active, as an integer
     * @param startTime the start time of the routine, as an integer
     * @param priority the priority of the routine
     * @param tasks the tasks of the routine
     * @param lastRun the date when this routine was last run
     * @return rowId or -1 if failed
     */
    public long createRoutine(String name, int daysActive, int startTime, int priority, String tasks, int lastRun) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NAME, name);
        initialValues.put(KEY_DAYS_ACTIVE, daysActive);
        initialValues.put(KEY_START_TIME, startTime);
        initialValues.put(KEY_PRIORITY, priority);
        initialValues.put(KEY_TASKS, tasks);
        initialValues.put(KEY_LAST_RUN, lastRun);

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    /**
     * Create a new routine using the name, days active, start time, priority, tasks, and last run date from the
     * routine provided. If the routine is successfully created return the new rowId for that
     * routine, otherwise return a -1 to indicate failure.
     *
     * @param routine the Routine object to make a new table entry from
     * @return rowId or -1 if failed
     */
    public long createRoutine(Routine routine) {
        return createRoutine(
                routine.getName(),
                routine.getDaysActiveAsInt(),
                routine.getStartTimeAsInt(),
                routine.getPriority(),
                routine.getTasksAsString(),
                routine.getDateLastRunAsInt());
    }

    /**
     * Delete the routine with the given rowId
     *
     * @param rowId id of routine to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteRoutine(long rowId) {

        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all routines in the database
     *
     * @return Cursor over all routines
     */
    public Cursor fetchAllRoutines() {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_NAME, KEY_DAYS_ACTIVE, KEY_START_TIME, KEY_PRIORITY, KEY_TASKS, KEY_LAST_RUN},
                null, null, null, null, null);
    }

    /**
     * Construct each routine in the database and return them all as a list
     * @return list of all routines
     */
    public List<Routine> getAllRoutines() {
        List<Routine> ret = new ArrayList<>();
        Cursor c = fetchAllRoutines();
        c.moveToFirst();
        while(!c.isAfterLast()) {
            ret.add(getRoutine(c.getInt(c.getColumnIndex(KEY_ROWID))));
            c.moveToNext();
        }
        return ret;
    }

    /**
     * Get each untimed routine which can be run today, in order from greatest to least priority
     * @return a list of all runnable routines
     */
    public List<Routine> getRunnableRoutinesInOrder() {
        List<Routine> ret = new ArrayList<>();
        List<Routine> routines = getAllRoutines();
        for (Routine routine : routines)
            if (routine.getStartTime() == null && routine.isRunnableToday())
                ret.add(routine);
        Collections.sort(ret);
        return ret;
    }

    /**
     * @return the untimed routine next in line to be run
     */
    public Routine getNextRunnableRoutine() {
        List<Routine> routines = getRunnableRoutinesInOrder();
        if (routines.size() == 0) return null;
        return routines.get(0);
    }

    /**
     * Return a Cursor positioned at the routine that matches the given rowId
     *
     * @param rowId id of routine to retrieve
     * @return Cursor positioned to matching routine, if found
     * @throws SQLException if routine could not be found/retrieved
     */
    public Cursor fetchRoutine(long rowId) throws SQLException {

        Cursor mCursor =

                mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID, KEY_NAME, KEY_DAYS_ACTIVE, KEY_START_TIME, KEY_PRIORITY, KEY_TASKS, KEY_LAST_RUN},
                        KEY_ROWID + "=" + rowId, null,
                        null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Construct and return the routine with the given rowId
     * @param rowId id of the routine
     * @return the routine with id rowId
     */
    public Routine getRoutine(long rowId) {
        Cursor c = fetchRoutine(rowId);
        return new Routine(c.getString(c.getColumnIndex(KEY_NAME)),
                           c.getInt   (c.getColumnIndex(KEY_DAYS_ACTIVE)),
                           c.getInt   (c.getColumnIndex(KEY_START_TIME)),
                           c.getInt   (c.getColumnIndex(KEY_PRIORITY)),
                           c.getString(c.getColumnIndex(KEY_TASKS)),
                           c.getInt   (c.getColumnIndex(KEY_LAST_RUN)),
                           c.getInt   (c.getColumnIndex(KEY_ROWID)));
    }

    /**
     * Update the routine using the details provided. The routine to be updated is
     * specified using the rowId, and it is altered to use the name, days active, start time,
     * priority, tasks, and lastRun values passed in. Last run date will not update if given value is -1
     *
     * @param rowId id of routine to update
     * @param name value to set routine name to
     * @param daysActive value to set routine days active to
     * @param startTime value to set routine start time to
     * @param priority value to set routine priority to
     * @param tasks value to set routine tasks to
     * @param lastRun value to set routine last run date to
     * @return true if the routine was successfully updated, false otherwise
     */
    public boolean updateRoutine(long rowId, String name, int daysActive, int startTime, int priority, String tasks, int lastRun) {
        ContentValues args = new ContentValues();
        args.put(KEY_NAME, name);
        args.put(KEY_DAYS_ACTIVE, daysActive);
        args.put(KEY_START_TIME, startTime);
        args.put(KEY_PRIORITY, priority);
        args.put(KEY_TASKS, tasks);
        if (lastRun != -1)
            args.put(KEY_LAST_RUN, lastRun);

        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Update the routine using the a constructed routine. The routine to be updated is
     * specified using the rowId, and it is altered to use the name, days active, start time,
     * priority, tasks, and last run date values of the routine passed in
     *
     * @param rowId id of routine to update
     * @param routine the routine to take the new values from
     * @return true if the routine was successfully updated, false otherwise
     */
    public boolean updateRoutine(long rowId, Routine routine) {
        return updateRoutine(
                rowId,
                routine.getName(),
                routine.getDaysActiveAsInt(),
                routine.getStartTimeAsInt(),
                routine.getPriority(),
                routine.getTasksAsString(),
                routine.getDateLastRunAsInt());
    }

}
