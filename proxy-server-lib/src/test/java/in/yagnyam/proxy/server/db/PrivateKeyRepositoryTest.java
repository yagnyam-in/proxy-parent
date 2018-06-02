package in.yagnyam.proxy.server.db;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import in.yagnyam.proxy.server.TestUtils;
import in.yagnyam.proxy.server.model.PrivateKeyEntity;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class PrivateKeyRepositoryTest extends RepositoryTestHelper {

    private PrivateKeyRepository issuerRepository = PrivateKeyRepository.builder().build();

    @BeforeClass
    public static void registerIssuerEntity() {
        ObjectifyService.register(PrivateKeyEntity.class);
    }

    @Test
    public void testGetIssuers_None() {
        assertThat(issuerRepository.getPrivateKeyEntities().size(), is(0));
    }

    @Test
    public void testGetIssuers_Exists() {
        PrivateKeyEntity privateKeyEntity = TestUtils.samplePrivateKeyEntity("0");
        ObjectifyService.run(new VoidWork() {
            @Override
            public void vrun() {
                ofy().save().entity(privateKeyEntity).now();
            }
        });
        assertThat(issuerRepository.getPrivateKeyEntities().size(), is(1));
    }


    @Test
    public void testGetIssuer_InvalidArguments() {
        thrown.expect(NullPointerException.class);
        issuerRepository.getPrivateKeyEntity(null);
    }

    @Test
    public void testGetIssuer_NotExists() {
        assertFalse(issuerRepository.getPrivateKeyEntity("1").isPresent());
    }

    @Test
    public void testGetIssuer_Valid() {
        PrivateKeyEntity privateKeyEntity = TestUtils.samplePrivateKeyEntity("2");
        ObjectifyService.run(new VoidWork() {
            @Override
            public void vrun() {
                ofy().save().entity(privateKeyEntity).now();
            }
        });
        assertTrue(issuerRepository.getPrivateKeyEntity(privateKeyEntity.getId()).isPresent());
        assertEquals(privateKeyEntity, issuerRepository.getPrivateKeyEntity(privateKeyEntity.getId()).get());
    }

    @Test
    public void testSaveIssuer_InvalidArguments() {
        thrown.expect(NullPointerException.class);
        issuerRepository.savePrivateKeyEntity(null);
    }


    @Test
    public void testSaveIssuer_Valid() {
        PrivateKeyEntity privateKeyEntity = TestUtils.samplePrivateKeyEntity("3");
        issuerRepository.savePrivateKeyEntity(privateKeyEntity);
        assertEquals(privateKeyEntity, ObjectifyService.run(() -> ofy().load().key(Key.create(PrivateKeyEntity.class, "3")).now()));
    }
}