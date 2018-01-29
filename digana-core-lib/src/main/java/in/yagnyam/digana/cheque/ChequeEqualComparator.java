package in.yagnyam.digana.cheque;

import lombok.SneakyThrows;

import java.util.Arrays;

/**
 * Checks if two given cheques are same or not
 * Usage: <code>new ChequeEqualComparator().equals(c1, c2)</code>
 */
public class ChequeEqualComparator {

    @SneakyThrows
    public static final boolean equals(Cheque cl, Cheque cr) {
        if (cl == cr) {
            return true;
        } else if (cl == null || cr == null) {
            return false;
        } else {
            ChequeSerializerFactory serializerFactory = ChequeSerializerFactory.instance();
            return Arrays.equals(serializerFactory.serializeCheque(cl), serializerFactory.serializeCheque(cr));
        }
    }
 }
