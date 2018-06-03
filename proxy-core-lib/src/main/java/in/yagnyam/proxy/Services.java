package in.yagnyam.proxy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Services provided by Proxy
 * TODO: Convert to an ENUM
 */
public interface Services {

    /**
     * Banking Service
     */
    String BANKING = "banking";

    /**
     * Identity Service
     */
    String IDENTITY = "identity";

    /**
     * Valid services provided by Proxy
     */
    Set<String> servicesOffered = new HashSet<>(Arrays.asList(BANKING, IDENTITY));

    /**
     * Is this service offered by Proxy
     * @param serviceName Service to check
     * @return true if a valid service name
     */
    static boolean isValidService(String serviceName) {
        return servicesOffered.contains(serviceName);
    }
}
