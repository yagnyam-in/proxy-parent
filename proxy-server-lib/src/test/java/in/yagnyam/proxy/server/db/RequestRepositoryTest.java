package in.yagnyam.proxy.server.db;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import in.yagnyam.proxy.server.BadRequestException;
import in.yagnyam.proxy.server.InternalServerErrorException;
import in.yagnyam.proxy.server.model.RequestEntity;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class RequestRepositoryTest extends RepositoryTestHelper {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private RequestRepository requestRepository = RequestRepository.builder().build();

    @BeforeClass
    public static void registerRequestEntity() {
        ObjectifyService.register(RequestEntity.class);
    }

    @Test
    public void testGetRequest_InvalidArguments() {
        thrown.expect(NullPointerException.class);
        requestRepository.getRequest(null);
    }

    @Test
    public void testGetRequest_NotExists() {
        assertFalse(requestRepository.getRequest("1").isPresent());
    }

    @Test
    public void testGetRequest_Valid() {
        RequestEntity requestEntity = RequestEntity.builder().requestId("RID").requestType("RT").build();
        ObjectifyService.run(new VoidWork() {
            @Override
            public void vrun() {
                ofy().save().entity(requestEntity).now();
            }
        });
        assertTrue(requestRepository.getRequest(requestEntity.getRequestId()).isPresent());
        assertEquals(requestEntity, requestRepository.getRequest(requestEntity.getRequestId()).get());
    }

    @Test
    public void testSaveRequest_InvalidArguments() {
        thrown.expect(NullPointerException.class);
        requestRepository.saveRequest(null);
    }


    @Test
    public void testSaveRequest_Valid() {
        RequestEntity requestEntity = RequestEntity.builder().requestId("RID").requestType("RT").build();
        requestRepository.saveRequest(requestEntity);
        assertEquals(requestEntity, ObjectifyService.run(() -> ofy().load().key(Key.create(RequestEntity.class, "RID")).now()));
    }

    @Test
    public void testSaveRequest_DuplicateRequest() {
        expectedException.expect(BadRequestException.class);
        RequestEntity requestEntity = RequestEntity.builder().requestId("RID").requestType("RT").build();
        requestRepository.saveRequest(requestEntity);
        requestRepository.saveRequest(requestEntity);
    }

}