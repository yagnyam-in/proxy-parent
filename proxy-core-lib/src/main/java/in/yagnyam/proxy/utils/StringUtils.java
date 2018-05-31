package in.yagnyam.proxy.utils;


/**
 * Json Utility methods
 */
public class StringUtils {

    public static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String nonNull(String... strings) {
        if (strings == null) {
            return "";
        }
        for (String s : strings) {
            if (!isEmpty(s)) {
                return s;
            }
        }
        return "";
    }

    public static boolean equals(String a, String b) {
        return a == b || (a != null && a.equals(b));
    }

    public static final int hashCode(String s) {
        return s == null ? 0 : s.hashCode();
    }

    public static String trim(String s) {
        return s == null ? null : s.trim();
    }

    public static String toString(Object o) {
        return o == null ? null : o.toString();
    }

}

