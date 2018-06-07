package in.yagnyam.proxy.utils;

public class EncodingUtils {

    private static final String base56Alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";

    public static String asBase56(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Invalid input " + value);
        }
        int base = base56Alphabet.length();
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            int r = Math.toIntExact(value % base);
            value = (value - r) / base;
            sb.append(base56Alphabet.charAt(r));
        }
        return sb.toString();

    }
}
