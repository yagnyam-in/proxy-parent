package in.yagnyam.proxy.messages.banking;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * ISO Currency Codes
 * TODO: Convert to an ENUM
 *
 * @author Yagnyam
 * @see Amount
 */
public interface Currency {

    String INR = "INR";
    String EUR = "EUR";
    String USD = "USD";
    String GBP = "GBP";
    String AUD = "AUD";
    String JPY = "JPY";

    /**
     * Valid Currencies
     */
    Set<String> currenciesSupported = new HashSet<>(Arrays.asList(INR, EUR, USD, GBP, AUD, JPY));

    /**
     * Check if the given currency is valid
     *
     * @param currency Currency to check
     * @return true if a valid currency
     */
    static boolean isValidCurrency(String currency) {
        return currenciesSupported.contains(currency);
    }
}
