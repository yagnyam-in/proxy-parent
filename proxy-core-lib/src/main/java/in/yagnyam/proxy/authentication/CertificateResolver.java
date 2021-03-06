package in.yagnyam.proxy.authentication;

import java.security.cert.X509Certificate;
import java.util.Optional;

public interface CertificateResolver {

  Optional<X509Certificate> getCertificate(String serialNumber);
}
