package in.yagnyam.proxy.server.db;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static in.yagnyam.proxy.server.TestUtils.sampleCertificate;
import static in.yagnyam.proxy.server.TestUtils.sampleCertificateForFingerPrint;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import in.yagnyam.proxy.Certificate;
import in.yagnyam.proxy.server.model.CertificateEntity;
import in.yagnyam.proxy.services.PemService;
import java.security.cert.X509Certificate;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DataStoreCertificateRepositoryTest extends RepositoryTestHelper {

  private DataStoreCertificateRepository certificateRepository = certificateRepository();

  @BeforeClass
  public static void registerCertificate() {
    ObjectifyService.register(CertificateEntity.class);
  }

  @SneakyThrows
  private DataStoreCertificateRepository certificateRepository() {
    PemService pemService = mock(PemService.class);
    when(pemService.decodeCertificate(anyString())).thenReturn(mock(X509Certificate.class));
    return DataStoreCertificateRepository.builder().pemService(pemService).build();
  }

  @Test
  public void testGetCertificate_InvalidArguments() {
    thrown.expect(NullPointerException.class);
    certificateRepository.getCertificateBySerialNumber(null, "sha256");
  }


  @Test
  public void testGetCertificate_NotExists() {
    assertFalse(certificateRepository.getCertificateBySerialNumber("1", null).isPresent());
  }


  @Test
  public void testGetCertificate_Exists() {
    Certificate certificate = sampleCertificate("2");
    ObjectifyService.run(new VoidWork() {
      @Override
      public void vrun() {
        ofy().save().entity(DataStoreCertificateRepository.toEntity(certificate)).now();
      }
    });
    assertTrue(certificateRepository.getCertificateBySerialNumber("2", null).isPresent());
    assertEquals(certificateRepository.getCertificateBySerialNumber("2", null).get(), certificate);
  }


  @Test
  public void testGetCertificatesBySha256Thumbprint_InvalidArguments() {
    thrown.expect(NullPointerException.class);
    certificateRepository.getCertificatesBySha256Thumbprint(null);
  }

  @Test
  public void testGetCertificatesBySha256Thumbprint_NotExists() {
    val results = certificateRepository.getCertificatesBySha256Thumbprint("1");
    assertThat(results.size(), is(0));
  }

  @Test
  public void testGetCertificatesBySha256Thumbprint_Exists() {
    Certificate certificate = sampleCertificateForFingerPrint("123");
    ObjectifyService.run(new VoidWork() {
      @Override
      public void vrun() {
        ofy().save().entity(DataStoreCertificateRepository.toEntity(certificate)).now();
      }
    });
    val results = certificateRepository.getCertificatesBySha256Thumbprint("123");
    assertThat(results.size(), is(1));
  }


  @Test
  public void testSaveCertificate_InvalidArguments() {
    thrown.expect(NullPointerException.class);
    certificateRepository.saveCertificate(null);
  }


  @Test
  public void testSaveCertificate() {
    Certificate certificate = sampleCertificate("3");
    certificateRepository.saveCertificate(certificate);
    CertificateEntity dbEntity = ObjectifyService
        .run(() -> ofy().load().key(Key.create(CertificateEntity.class, "3")).now());
    assertEquals(certificate, certificateRepository.fromEntity(dbEntity));
  }


}
