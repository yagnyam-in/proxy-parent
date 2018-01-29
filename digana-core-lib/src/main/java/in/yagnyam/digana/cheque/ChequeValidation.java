package in.yagnyam.digana.cheque;

/**
 * Cheque Validation Utility functions
 */
class ChequeValidation {

    static void assertNotNull(Object value, String name) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException(name + " not set");
        }
    }


    static void assertPositive(long value, String name) throws IllegalArgumentException {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " can't be " + value);
        }
    }

    static void assertNullOrPositive(Long value, String name) throws IllegalArgumentException {
        if (value != null) {
            assertPositive(value, name);
        }
    }


}
