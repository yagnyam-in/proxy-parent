package in.yagnyam.proxy.server.db;

import static com.googlecode.objectify.ObjectifyService.ofy;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.server.model.CertificateEntity;
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
        .build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<Certificate> getCertificateBySerialNumber(@NonNull String serialNumber) {
    return ObjectifyService.run(() -> {
          CertificateEntity entity = ofy().load().key(Key.create(CertificateEntity.class, serialNumber))
              .now();
          return Optional.ofNullable(entity).map(this::fromEntity);
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
  public List<Certificate> getCertificatesById(String certificateId) {
    String owner = Certificate.extractOnlyId(certificateId);
    return ObjectifyService.run(() -> ofy().load()
        .type(CertificateEntity.class)
        .filter("owner", owner)
        .list().stream()
        .map(this::fromEntity)
        .filter((c) -> c.matchesId(certificateId))
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
        .build();
    return CertificateUtils.enrichCertificate(certificate, pemService);
  }
}
