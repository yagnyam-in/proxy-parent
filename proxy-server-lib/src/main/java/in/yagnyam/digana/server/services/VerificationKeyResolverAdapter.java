package in.yagnyam.digana.server.services;

import in.yagnyam.digana.utils.StringUtils;
import in.yagnyam.proxy.services.CertificateService;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.lang.UnresolvableKeyException;

import java.security.Key;
import java.util.List;

import static org.jose4j.jwx.HeaderParameterNames.KEY_ID;

@Slf4j
public class VerificationKeyResolverAdapter implements VerificationKeyResolver {

    private final CertificateService certificateService;

    @Builder
    private VerificationKeyResolverAdapter(@NonNull CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @Override
    public Key resolveKey(JsonWebSignature jws, List<JsonWebStructure> nestingContext) throws UnresolvableKeyException {
        String serialNumber = jws.getKeyIdHeaderValue();
        if (StringUtils.isEmpty(serialNumber)) {
            log.info("Header " + KEY_ID + " not present in the JWS in " + jws);
            throw new IllegalArgumentException("Header " + KEY_ID + " not present in the JWS");
        }
        return certificateService.getCertificateBySerialNumber(serialNumber)
                .orElseThrow(() -> new UnresolvableKeyException("Unable fetch the certificate " + serialNumber))
                .getCertificate()
                .getPublicKey();
    }
}
