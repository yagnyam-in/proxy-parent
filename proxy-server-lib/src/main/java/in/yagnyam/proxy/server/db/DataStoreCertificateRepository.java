package in.yagnyam.proxy.server.db;

import static com.googlecode.objectify.ObjectifyService.ofy;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.server.model.CertificateEntity;
import in.yagnyam.proxy.services.CertificateService;
import in.yagnyam.proxy.services.PemService;
import in.yagnyam.proxy.utils.CertificateUtils;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * Certificate Repository for storing and retrieving from Database
 */
@Builder
public class DataStoreCertificateRepository implements CertificateRepository {

  static {
    ObjectifyService.register(CertificateEntity.class);
  }

  @NonNull
  private PemService pemService;

  static CertificateEntity toEntity(Certificate certificate) {
    return CertificateEntity.builder()
        .serialNumber(certificate.getSerialNumber())
        .owner(certificate.getOwner())
        .sha256Thumbprint(certificate.getSha256Thumbprint())
        .alias(certificate.getAlias())
        .subject(certificate.getSubject())
        .validFrom(certificate.getValidFrom())
        .validTill(certificate.getValidTill())
        .certificateEncoded(certificate.getCertificateEncoded())
        .issuerSerialNumber(certificate.getIssuerSerialNumber())
        .issuerSha256Thumbprint(certificate.getIssuerSha256Thumbprint())
        .publicKeyEncoded(certificate.getPublicKeyEncoded())
        .publicKeySha256Thumbprint(certificate.getPublicKeySha256Thumbprint())
        .build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<Certificate> getCertificateBySerialNumber(@NonNull String serialNumber, String sha256Thumbprint) {
    return ObjectifyService.run(() -> {
          CertificateEntity entity = ofy().load().key(Key.create(CertificateEntity.class, serialNumber))
              .now();
          return Optional.ofNullable(entity)
              .map(this::fromEntity)
              .filter(CertificateService.sha256MatcherForCertificate(sha256Thumbprint));
        }
    );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Certificate> getCertificatesBySha256Thumbprint(@NonNull String sha256Thumbprint) {
    return ObjectifyService.run(() -> ofy().load()
        .type(CertificateEntity.class)
        .filter("sha256Thumbprint", sha256Thumbprint)
        .list().stream().map(this::fromEntity).collect(Collectors.toList())
    );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Certificate> getCertificatesById(@NonNull String certificateId, String sha256Thumbprint) {
    return ObjectifyService.run(() -> ofy().load()
        .type(CertificateEntity.class)
        .filter("owner", certificateId)
        .list().stream()
        .map(this::fromEntity)
        .filter(CertificateService.sha256MatcherForCertificate(sha256Thumbprint))
        .collect(Collectors.toList())
    );
  }

  /**
   * Save the Certificate along with Certificate Request
   *
   * @param certificate Certificate
   */
  @Override
  public void saveCertificate(@NonNull Certificate certificate) {
    CertificateEntity entity = toEntity(certificate);
    ObjectifyService.run(new VoidWork() {
      @Override
      public void vrun() {
        ofy().save().entity(entity).now();
      }
    });
  }

  @SneakyThrows
  Certificate fromEntity(CertificateEntity certificateEntity) {
    Certificate certificate = Certificate.builder()
        .serialNumber(certificateEntity.getSerialNumber())
        .owner(certificateEntity.getOwner())
        .sha256Thumbprint(certificateEntity.getSha256Thumbprint())
        .alias(certificateEntity.getAlias())
        .subject(certificateEntity.getSubject())
        .validFrom(certificateEntity.getValidFrom())
        .validTill(certificateEntity.getValidTill())
        .certificateEncoded(certificateEntity.getCertificateEncoded())
        .issuerSerialNumber(certificateEntity.getIssuerSerialNumber())
        .issuerSha256Thumbprint(certificateEntity.getIssuerSha256Thumbprint())
        .publicKeyEncoded(certificateEntity.getPublicKeyEncoded())
        .publicKeySha256Thumbprint(certificateEntity.getPublicKeySha256Thumbprint())
        .build();
    return CertificateUtils.enrichCertificate(certificate, pemService);
  }
}
