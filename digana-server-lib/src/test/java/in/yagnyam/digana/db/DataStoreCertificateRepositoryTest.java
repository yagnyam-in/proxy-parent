package in.yagnyam.digana.db;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.cache.AsyncCacheFilter;
import in.yagnyam.digana.model.Certificate;
import lombok.val;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.internal.matchers.Null;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.Closeable;
import java.io.IOException;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static in.yagnyam.digana.TestUtils.sampleCertificate;
import static in.yagnyam.digana.TestUtils.sampleCertificateForFingerPrint;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class DataStoreCertificateRepositoryTest {

    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
    private Closeable session;

    private DataStoreCertificateRepository certificateRepository = DataStoreCertificateRepository.builder().build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setUpBeforeClass() {
        ObjectifyService.setFactory(new ObjectifyFactory());
        ObjectifyService.register(Certificate.class);
    }

    @Before
    public void setUp() {
        this.helper.setUp();
        this.session = ObjectifyService.begin();
    }

    @After
    public void tearDown() throws IOException {
        AsyncCacheFilter.complete();
        this.session.close();
        this.helper.tearDown();
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
