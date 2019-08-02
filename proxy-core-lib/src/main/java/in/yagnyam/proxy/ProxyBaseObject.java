package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface ProxyBaseObject {

    /// Is this object Valid
    ///
    /// Checks the business rules etc.
    @JsonIgnore
    boolean isValid();

}
