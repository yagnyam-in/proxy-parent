package in.yagnyam.proxy.server.services;

import in.yagnyam.proxy.server.db.RequestRepository;
import in.yagnyam.proxy.server.model.RequestEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class RequestServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @InjectMocks
    private RequestService requestService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private void assertNullPointerExceptionThrown(String message, Runnable runnable) {
        try {
            runnable.run();
            fail(message);
        } catch (NullPointerException ne) {
            log.debug(message, ne);
        }
    }

    @Test
    public void testRequestExistsWithId_InvalidArguments() {
        expectedException.expect(NullPointerException.class);
        requestService.requestExistsWithId(null, null);
    }


    @Test
    public void testRequestExistsWithId_NotExists() {
        when(requestRepository.getRequest(anyString())).thenReturn(Optional.empty());
        assertFalse(requestService.requestExistsWithId("123", null));
    }


    @Test
    public void testRequestExistsWithId_Exists() {
        when(requestRepository.getRequest(anyString())).thenReturn(Optional.of(mock(RequestEntity.class)));
        assertTrue(requestService.requestExistsWithId("123", "RT"));
    }

    @Test
    public void testSaveRequest_Invalid() {
        assertNullPointerExceptionThrown("Null Request", () -> requestService.saveRequest(null));
        assertNullPointerExceptionThrown("Null Arguments", () -> requestService.saveRequest(null, null));
    }

    @Test
    public void testSave_Request() {
        RequestEntity requestEntity = mock(RequestEntity.class);
        requestService.saveRequest(requestEntity);
        verify(requestRepository, times(1)).saveRequest(eq(requestEntity));
    }

}