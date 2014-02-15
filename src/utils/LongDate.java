/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
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

    public long getDateLong() {
        return dateLong;
    }

    public Date getDateLongAsDate() {
        return longToCalendar(dateLong).getTime();
    }

    public void setDateLong(long dateLong) {
        this.dateLong = dateLong;
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

    private static long calendarToLong(Calendar date) throws NumberFormatException {
//        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH);
//        long baseDate = Long.valueOf(sdf.format(cal.getTime()));
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
        final String format = sdf.format(date.getTime());
        return Long.valueOf(format) * 10000 + 2300;
    }

    private Calendar longToCalendar(long dateLong) throws NumberFormatException {
        try {
            final Calendar ret = Calendar.getInstance();
            final Date date = new SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH).parse(dateLong + "");
            ret.setTime(date);
            return ret;
        } catch (ParseException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

    public int getDaysDiffToNow() {
        return getDaysDiff(getDateLongAsDate(), Calendar.getInstance().getTime());
    }

    public static long nowToNextWednesday(int weeks) {
        Calendar date = Calendar.getInstance();

        int diff = Calendar.WEDNESDAY - date.get(Calendar.DAY_OF_WEEK);
        if (!(diff > 0)) {
            diff += 7 * weeks;
        }
        date.add(Calendar.DAY_OF_MONTH, diff);
        return calendarToLong(date);
    }

    public long plusWeek(int weeks) {
        Calendar base = longToCalendar(getDateLong());
        base.add(Calendar.DATE, 7 * weeks);
        return calendarToLong(base);
    }

    private int getDaysDiff(Date newerDate, Date olderDate) {
        return (int) ((newerDate.getTime() - olderDate.getTime())
                / (1000 * 60 * 60 * 24));
    }
}