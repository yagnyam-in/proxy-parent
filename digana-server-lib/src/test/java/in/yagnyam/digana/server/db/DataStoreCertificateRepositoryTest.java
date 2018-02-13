package in.yagnyam.digana.server.db;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import in.yagnyam.digana.server.model.Certificate;
import lombok.val;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static in.yagnyam.digana.server.TestUtils.sampleCertificate;
import static in.yagnyam.digana.server.TestUtils.sampleCertificateForFingerPrint;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class DataStoreCertificateRepositoryTest extends RepositoryTestHelper {

    private DataStoreCertificateRepository certificateRepository = DataStoreCertificateRepository.builder().build();

    @BeforeClass
    public static void registerCertificate() {
        ObjectifyService.register(Certificate.class);
    }


    @Test
    public void testGetCertificate_InvalidArguments() {
        thrown.expect(NullPointerException.class);
        certificateRepository.getCertificate(null);
    }


    @Test
    public void testGetCertificate_NotExists() {
        assertFalse(certificateRepository.getCertificate("1").isPresent());
    }


    @Test
    public void testGetCertificate_Exists() {
        Certificate certificate = sampleCertificate("2");
        ObjectifyService.run(new VoidWork() {
            @Override
            public void vrun() {
                ofy().save().entity(certificate).now();
            }
        });
        assertTrue(certificateRepository.getCertificate("2").isPresent());
        assertEquals(certificateRepository.getCertificate("2").get(), certificate);
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
                ofy().save().entity(certificate).now();
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
        assertEquals(certificate, ObjectifyService.run(() -> ofy().load().key(Key.create(Certificate.class, "3")).now()));
    }


}
