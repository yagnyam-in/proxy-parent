package in.yagnyam.digana.server.db;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import in.yagnyam.digana.server.model.Certificate;
import lombok.Builder;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;

import static com.googlecode.objectify.ObjectifyService.ofy;

/** Certificate Repository for storing and retrieving from Database */
@Builder
public class DataStoreCertificateRepository implements CertificateRepository {

  static {
    ObjectifyService.register(Certificate.class);
  }

  /**
   * Fetch the certificate from Database
   *
   * @param serialNumber Certificate Serial Number
   * @return Certificate with given Serial Number
   */
  @Override
  public Optional<Certificate> getCertificate(@NonNull String serialNumber) {
    return ObjectifyService.run(
        () ->
            Optional.ofNullable(
                ofy().load().key(Key.create(Certificate.class, serialNumber)).now()));
  }

  /**
   * Fetch the certificate from Database using SHA256 Thumbprint
   *
   * @param sha256Thumbprint Certificate SHA256 Thumbprint
   * @return Certificates matching Thumbprint
   */
  @Override
  public List<Certificate> getCertificatesBySha256Thumbprint(@NonNull String sha256Thumbprint) {
    return ObjectifyService.run(
        () ->
            ofy()
                .load()
                .type(Certificate.class)
                .filter("sha256Thumbprint", sha256Thumbprint)
                .list());
  }

  /**
   * Save the Certificate along with Certificate Request
   *
   * @param certificate Certificate
   */
  @Override
  public void saveCertificate(@NonNull Certificate certificate) {
    ObjectifyService.run(
        new VoidWork() {
          @Override
          public void vrun() {
            ofy().save().entity(certificate).now();
          }
        });
  }
}
