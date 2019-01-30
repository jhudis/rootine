package com.github.jhudis.rootine;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Routine implements Comparable<Routine> {

    private String name;
    private boolean[] daysActive; //Index 0 is Sunday, 6 is Saturday
    private Time startTime; //When referring to "untimed" routines, this indicates the routine has no set start time
                            //i.e. it's run when the user clicks the run button on Home screen on an active day
    private int priority; //For routines which appear on the same day, 0 goes first, 9 last
    private List<Task> tasks;
    private Date lastRun; //For keeping track of which untimed routines can be run at the moment
    private int id;

    //Delimiters for converting list of tasks into a single string (for database storage)
    private static final String DEL1 = "0k4d6";
    private static final String DEL2 = "qtJo7";

    private static final String[] DAYS = {"Sun", "Mon", "Tues", "Wed", "Thurs", "Fri", "Sat"};

    public Routine(String name, boolean[] daysActive, Time startTime, int priority, List<Task> tasks) {
        this.name = name;
        this.daysActive = daysActive;
        this.startTime = startTime;
        this.priority = priority;
        this.tasks = tasks;
    }

    public Routine(String name, int daysActive, int startTime, int priority, String tasks) {
        this(name,
             convertIntToDaysActive(daysActive),
             startTime == -1 ? null : new Time(startTime),
             priority,
             convertStringToTasks(tasks));
    }

    public Routine(String name, int daysActive, int startTime, int priority, String tasks, int lastRun, int id) {
        this(name, daysActive, startTime, priority, tasks);
        this.lastRun = lastRun == -1 ? null : new Date(lastRun);
        this.id = id;
    }

    public boolean isRunnableToday() {
        //For running from Home screen
        if (alreadyRunToday()) return false;
        int dayOfWeekToday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-1;
        return daysActive[dayOfWeekToday];
    }

    private boolean alreadyRunToday() {
        if (lastRun == null) return false;
        return lastRun.equals(new Date());
    }

    public String getTasksAsString() {
        //For database storage
        String ret = "";
        if (tasks.size() == 0) return ret;
        for (Task t : tasks)
            ret += t + DEL2;
        return ret.substring(0, ret.length() - DEL2.length());
    }

    private static List<Task> convertStringToTasks(String tasks) {
        //For database retrieval
        if (tasks.length() == 0) return new ArrayList<>();
        List<Task> ret = new ArrayList<>();
        String[] taskStringArray = tasks.split(DEL2);
        for (String t : taskStringArray)
            ret.add(new Task(t));
        return ret;
    }

    public int getDaysActiveAsInt() {
        //For database storage
        //Treat the array of booleans as a string of binary digits, then convert to decimal
        //e.g. [Friday, Saturday] -> 3
        int ret = 0;
        int pow = 1;
        for (int i = 0; i < 7; i++) {
            if (daysActive[6 - i])
                ret += pow;
            pow *= 2;
        }
        return ret;
    }

    private static boolean[] convertIntToDaysActive(int num) {
        //For database retrieval
        //Convert to binary, then treat the string of binary digits as an array of booleans
        //e.g. 3 -> [Friday, Saturday]
        boolean[] ret = new boolean[7];
        String bin = String.format("%07d", Integer.parseInt(Integer.toBinaryString(num)));
        for (int i = 0; i < 7; i++)
            ret[i] = bin.charAt(i) == '1';
        return ret;
    }

    public String getDaysActiveAsString() {
        //For displaying in ManageRoutines
        String ret = "";
        for (int i = 0; i < 7; i++)
            if (daysActive[i])
                ret += DAYS[i] + ", ";
        return ret.length() == 0 ? "No Active Days" : ret.substring(0, ret.length() - 2);
    }

    public String getStartTimeOrPriority() {
        //For displaying in ManageRoutines
        return startTime == null ? "Priority: " + priority : startTime.toString12Hr();
    }

    public int getStartTimeAsInt() {
        //For database storage
        //12:12 PM  ->  1212
        //12:00 AM  ->  0
        if (startTime == null)
            return -1;
        else
            return startTime.getTimeAsInt();
    }

    public int getDateLastRunAsInt() {
        //For database storage
        return lastRun == null ? -1 : lastRun.getDateAsInt();
    }

    public void setAlarms(Context context) {
        if (getStartTime() == null) return; //Only do this if this is a timed routine

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, getStartTime().getHour());
        calendar.set(Calendar.MINUTE, getStartTime().getMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        Intent runRoutine = new Intent(context, RoutineService.class);
        runRoutine.putExtra(context.getString(R.string.routine_id_extra), getId());

        long triggerAtMillis;
        int uniqueAlarmCode; //For updating/cancelling alarms
        for (int day = 0; day < 7; day++) { //Set a weekly alarm for each active day
            uniqueAlarmCode = 2 * getId() + 3 * day;
            PendingIntent alarmIntent = PendingIntent.getForegroundService(context, uniqueAlarmCode, runRoutine, 0);

            if (getDaysActive()[day]) {
                calendar.set(Calendar.DAY_OF_WEEK, day + 1); //Calendar days are 1-indexed
                triggerAtMillis = calendar.getTimeInMillis();
                if (calendar.getTimeInMillis() < System.currentTimeMillis()) //If we've already passed the alarm this week, have it start next week
                    triggerAtMillis += AlarmManager.INTERVAL_DAY * 7;
                alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, AlarmManager.INTERVAL_DAY * 7, alarmIntent);
            } else {
                alarmManager.cancel(alarmIntent); //Cancel alarms on inactive days to account for the user updating active days
            }
        }
    }

    public void cancelAlarms(Context context) {
        if (getStartTime() == null) return; //Only do this if this is a timed routine

        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        Intent runRoutine = new Intent(context, RoutineService.class);
        int uniqueAlarmCode;
        for (int day = 0; day < 7; day++) {
            uniqueAlarmCode = 2 * getId() + 3 * day;
            //Make a dummy intent so we can cancel the alarm
            PendingIntent alarmIntent = PendingIntent.getForegroundService(context, uniqueAlarmCode, runRoutine, 0);
            alarmManager.cancel(alarmIntent);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean[] getDaysActive() {
        return daysActive;
    }

    public void setDaysActive(boolean[] daysActive) {
        this.daysActive = daysActive;
    }

    public Time getStartTime() {
        return startTime;
    }

    public void setStartTime(Time startTime) {
        this.startTime = startTime;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getLastRun() {
        return lastRun;
    }

    public void setLastRun(Date lastRun) {
        this.lastRun = lastRun;
    }

    @Override
    public int compareTo(@NonNull Routine o) {
        return priority - o.getPriority();
    }

    public static class Task implements Parcelable {
        private String command;
        private int duration; //in minutes

        public Task(String command, int duration) {
            this.command = command;
            this.duration = duration;
        }

        public Task(String task) {
            //For database retrieval
            String[] splitTask = task.split(DEL1);
            command = splitTask[0];
            duration = Integer.parseInt(splitTask[1]);
        }

        protected Task(Parcel in) {
            command = in.readString();
            duration = in.readInt();
        }

        public static final Creator<Task> CREATOR = new Creator<Task>() {
            @Override
            public Task createFromParcel(Parcel in) {
                return new Task(in);
            }

            @Override
            public Task[] newArray(int size) {
                return new Task[size];
            }
        };

        @Override
        public String toString() {
            //For database storage
            return command + DEL1 + duration;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public int getDuration() {
            return duration;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(command);
            dest.writeInt(duration);
        }
    }

    public static class Time {
        private int hour; //military time
        private int minute;

        public Time(int hour, int minute) {
            this.hour = hour;
            this.minute = minute;
        }

        public Time(String timeString) {
            //timeString will look like "12:00 AM"

            //Split into "12:00" and "AM"
            int spaceIndex = timeString.indexOf(" ");
            String timeNumber = timeString.substring(0, spaceIndex);
            String am_pm = timeString.substring(spaceIndex + 1);
            boolean isPM = am_pm.equals("PM");

            //Split into 12 and 0
            int colonIndex = timeNumber.indexOf(":");
            hour = Integer.parseInt(timeNumber.substring(0, colonIndex));
            minute = Integer.parseInt(timeNumber.substring(colonIndex + 1));

            //Convert to military time
            // 12AM --> 0
            // 1AM --> 1
            // 11AM --> 11
            // 12PM --> 12
            // 1PM --> 13
            // 11PM --> 23
            if (isPM && hour != 12)
                hour += 12;
            if (!isPM && hour == 12)
                hour = 0;
        }

        public Time(int time) {
            //For database retrieval
            String timeString = Integer.toString(time);
            int len = timeString.length();
            if (len <= 2) { //e.g. 0 = 00:00 = 12:00 AM, 30 = 00:30 = 12:30 AM
                hour = 0;
                minute = Integer.parseInt(timeString);
            } else {
                hour = Integer.parseInt(timeString.substring(0, len-2));
                minute = Integer.parseInt(timeString.substring(len-2));
            }
        }

        private String formatMinute() {
            return String.format("%02d", minute);
        }

        public int getTimeAsInt() {
            //For database storage
            return Integer.parseInt(hour + formatMinute());
        }

        public String toString12Hr() {
            //For displaying in ManageRoutines and loading into input for AddRoutine
            String am_pm = hour < 12 ? "AM" : "PM";
            int hour12 = hour;
            if (hour > 12) hour12 -= 12;
            if (hour == 0) hour12 = 12;
            return hour12 + ":" + formatMinute() + " " + am_pm;
        }

        @Override
        public String toString() {
            return hour + ":" + formatMinute();
        }

        public boolean isPM() {
            return hour >= 12;
        }

        public int getHour() {
            return hour;
        }

        public void setHour(int hour) {
            this.hour = hour;
        }

        public int getMinute() {
            return minute;
        }

        public void setMinute(int minute) {
            this.minute = minute;
        }
    }

    public static class Date {
        private int month, day, year; //e.g. 12/27/2018 = 12, 27, 18

        public Date(int month, int day, int year) {
            this.month = month;
            this.day = day;
            this.year = year;
        }

        public Date() {
            this(Calendar.getInstance().get(Calendar.MONTH)+1,
                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                    Calendar.getInstance().get(Calendar.YEAR)-2000);
        }

        public Date(int date) {
            //For database retrieval
            this(Integer.parseInt(String.format("%06d", date).substring(0, 2)),
                    Integer.parseInt(String.format("%06d", date).substring(2, 4)),
                    Integer.parseInt(String.format("%06d", date).substring(4)));
        }

        public int getDateAsInt() {
            //For database storage
            return Integer.parseInt(format2Digits(month) + format2Digits(day) + format2Digits(year));
        }

        private String format2Digits(int num) {
            return String.format("%02d", num);
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public int getDay() {
            return day;
        }

        public void setDay(int day) {
            this.day = day;
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Date date = (Date) o;
            return month == date.month &&
                    day == date.day &&
                    year == date.year;
        }
    }

}
