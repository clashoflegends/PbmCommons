/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.util.Calendar;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author jmoura
 */
public class LongDate {

    private static final Log log = LogFactory.getLog(LongDate.class);
    private long dateLong;

    public LongDate() {
    }

    public LongDate(long date) {
        this.dateLong = date;
    }

    public long toLong() {
        return getDateLong();
    }

    @Override
    public String toString() {
        return getDateLong() + "";
    }

    public String toDateString() {
        final String date = toString();
        final String year = date.substring(0, 4);
        final String month = date.substring(4, 6);
        final String day = date.substring(6, 8);

        return String.format("%s-%s-%s", year, month, day);
    }

    public String toDateTimeString() {
        final String date = toString();
        final String year = date.substring(0, 4);
        final String month = date.substring(4, 6);
        final String day = date.substring(6, 8);
        final String hour = date.substring(8, 10);
        final String minute = date.substring(10, 12);

        return String.format("%s-%s-%s %s:%s", year, month, day, hour, minute);
    }

    public String toDateMysql() {
        final String date = toString();
        final String year = date.substring(0, 4);
        final String month = date.substring(4, 6);
        final String day = date.substring(6, 8);
        final String hour = date.substring(8, 10);
        final String minute = date.substring(10, 12);

        return String.format("%s-%s-%s %s:%s:00", year, month, day, hour, minute);
    }

    public long plusWeek(int weeks) {
        return getDateLong() + (70000 * weeks);
    }

    public long getDaysDiff(long baseDate) {
        return (getDateLong() - baseDate) / 10000;
    }

    public long getDaysDiffToNow() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmm");
        long baseDate = Long.valueOf(sdf.format(cal.getTime()));
        return getDaysDiff(baseDate);
    }

    public static long nowToNextWednesday(int weeks) {
        long ret = 0;
        Calendar date = Calendar.getInstance();
        int diff = Calendar.WEDNESDAY - date.get(Calendar.DAY_OF_WEEK);
        if (!(diff > 0)) {
            diff += 7 * weeks;
        }
        date.add(Calendar.DAY_OF_MONTH, diff);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd");
        String format = sdf.format(date.getTime());
        ret = Long.valueOf(format) * 10000;
        return ret;
    }

    public long getDateLong() {
        return dateLong;
    }

    public void setDateLong(long dateLong) {
        this.dateLong = dateLong;
    }
}
