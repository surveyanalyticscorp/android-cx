package com.questionpro.cxlib.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {
    public static String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Format: YYYY-MM-DD
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String getCurrentDayOfMonth() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd", Locale.getDefault()); // Format: YYYY-MM-DD
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String getCurrentDayOfWeek() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        String dayName = "";
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                dayName = "Sunday";
                break;
            case Calendar.MONDAY:
                dayName = "Monday";
                break;
            case Calendar.TUESDAY:
                dayName = "Tuesday";
                break;
            case Calendar.WEDNESDAY:
                dayName = "Wednesday";
                break;
            case Calendar.THURSDAY:
                dayName = "Thursday";
                break;
            case Calendar.FRIDAY:
                dayName = "Friday";
                break;
            case Calendar.SATURDAY:
                dayName = "Saturday";
                break;
        }
        return dayName;
    }

    public static String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()); //Format YYYY-MM-DD HH:MM:SS
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String getCurrentTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()); //Format HH:MM:SS
        Date time = new Date();
        return timeFormat.format(time);
    }
}
