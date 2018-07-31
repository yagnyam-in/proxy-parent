package in.yagnyam.proxy.server.services;

import static org.jose4j.jwx.HeaderParameterNames.KEY_ID;

import in.yagnyam.proxy.services.CertificateService;
import in.yagnyam.proxy.utils.StringUtils;
import java.security.Key;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.lang.UnresolvableKeyException;

@Slf4j
public class VerificationKeyResolverAdapter implements VerificationKeyResolver {

  private final CertificateService certificateService;

  @Builder
  private VerificationKeyResolverAdapter(@NonNull CertificateService certificateService) {
    this.certificateService = certificateService;
  }

  @Override
  public Key resolveKey(JsonWebSignature jws, List<JsonWebStructure> nestingContext)
      throws UnresolvableKeyException {
    String serialNumber = jws.getKeyIdHeaderValue();
    if (StringUtils.isEmpty(serialNumber)) {
      log.info("Header " + KEY_ID + " not present in the JWS in " + jws);
      throw new IllegalArgumentException("Header " + KEY_ID + " not present in the JWS");
    }
    return certificateService.getCertificateBySerialNumber(serialNumber)
        .orElseThrow(
            () -> new UnresolvableKeyException("Unable fetch the certificate " + serialNumber))
        .getCertificate()
        .getPublicKey();
  }
}
