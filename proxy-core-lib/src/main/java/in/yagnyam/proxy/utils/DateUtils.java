package in.yagnyam.proxy.utils;


import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateUtils {

  public static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

  public static Date now() {
    return Calendar.getInstance(UTC_TIME_ZONE).getTime();
  }

  public static Date today() {
    Calendar cal = Calendar.getInstance(UTC_TIME_ZONE);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
  }

  public static Date afterYears(int years) {
    Calendar c = Calendar.getInstance(UTC_TIME_ZONE);
    c.add(Calendar.YEAR, years);
    return c.getTime();
  }

  public static Date afterDays(int days) {
    Calendar c = Calendar.getInstance(UTC_TIME_ZONE);
    c.add(Calendar.DATE, days);
    return c.getTime();
  }

  public static int age(Date dateOfBirth) {
    Calendar today = Calendar.getInstance(UTC_TIME_ZONE);

    Calendar dob = Calendar.getInstance(UTC_TIME_ZONE);
    dob.setTime(dateOfBirth);

    return today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
  }

  public static Date forDate(int year, int month, int day) {
    Calendar cal = Calendar.getInstance(UTC_TIME_ZONE);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.MONTH, month);
    cal.set(Calendar.DAY_OF_MONTH, day);
    return cal.getTime();
  }

  public static boolean isValid(Date date) {
    return date != null;
  }

}
