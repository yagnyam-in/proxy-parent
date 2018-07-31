package in.yagnyam.proxy.utils;


import java.util.Calendar;
import java.util.Date;

public class DateUtils {

  public static Date now() {
    return Calendar.getInstance().getTime();
  }

  public static Date afterYears(int years) {
    Calendar c = Calendar.getInstance();
    c.add(Calendar.YEAR, years);
    return c.getTime();
  }

  public static int age(Date dateOfBirth) {
    Calendar today = Calendar.getInstance();

    Calendar dob = Calendar.getInstance();
    dob.setTime(dateOfBirth);

    return today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
  }

  public static boolean isValid(Date date) {
    return date != null;
  }

}
