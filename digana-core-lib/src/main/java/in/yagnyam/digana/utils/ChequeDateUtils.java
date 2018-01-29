package in.yagnyam.digana.utils;

import java.util.Calendar;
import java.util.Date;

/**
 * Date Utils used by Cheque Lib
 */
public class ChequeDateUtils {

    /**
     * <p>
     * Return today
     * </p>
     * <p>
     * This must be inline with daysAfterToday function
     * </p>
     *
     * @return Todays date
     * @see {@link #daysAfterToday(int) daysAfterToday}
     */
    public static Date today() {
        // We need not get current business day from institution
        // Its all over kill
        // Just return creation time
        return Calendar.getInstance().getTime();
    }

    /**
     * <p>
     * Return a future date from today after given days
     * </p>
     * <p>
     * This must be inline with today
     * </p>
     *
     * @param days No of days
     * @return Future days after given days
     * @see {@link #today() today}
     */
    public static Date daysAfterToday(int days) {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.DATE, days);
        return date.getTime();
    }


    /**
     * <p>
     * Return a future date from today after given Months
     * </p>
     * <p>
     * This must be inline with today
     * </p>
     *
     * @param months No of Months
     * @return Future days after given months
     * @see {@link #today() today}
     */
    public static Date monthsAfterToday(int months) {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.MONTH, months);
        return date.getTime();
    }

}
