package in.yagnyam.digana.utils;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface ServletUtils {

    static Optional<HttpServletRequest> asHttpServletRequest(ServletRequest servletRequest) {
        if (servletRequest instanceof HttpServletRequest) {
            return Optional.of((HttpServletRequest) servletRequest);
        } else {
            return Optional.empty();
        }
    }

    static Optional<String> getHeader(ServletRequest servletRequest, String header) {
        return asHttpServletRequest(servletRequest).flatMap(h -> Optional.ofNullable(h.getHeader(header)));
    }

    static Optional<String> getRequestURI(ServletRequest servletRequest) {
        return asHttpServletRequest(servletRequest).map(HttpServletRequest::getRequestURI);
    }
}
