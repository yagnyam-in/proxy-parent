package in.yagnyam.proxy.server.services;

import in.yagnyam.proxy.RequestMessage;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.server.BadRequestException;
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

import static org.junit.Assert.*;
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
    public void testAssertNewRequest_Unique() {
        when(requestRepository.getRequest(anyString())).thenReturn(Optional.empty());
        requestService.assertNewRequest("123", "RT");
    }

    @Test
    public void testAssertNewRequest_Duplicate() {
        expectedException.expect(BadRequestException.class);
        when(requestRepository.getRequest(anyString())).thenReturn(Optional.of(mock(RequestEntity.class)));
        requestService.assertNewRequest("123", "RT");
    }

    @Test
    public void testSaveRequestEntity_Invalid() {
        assertNullPointerExceptionThrown("Null Request", () -> requestService.saveRequestEntity(null));
        assertNullPointerExceptionThrown("Null Arguments", () -> requestService.saveRequestEntity(null, null));
    }

    @Test
    public void testSaveRequestEntity() {
        RequestEntity requestEntity = mock(RequestEntity.class);
        requestService.saveRequestEntity(requestEntity);
        verify(requestRepository, times(1)).saveRequest(eq(requestEntity));
    }

    @Test
    public void testSaveSignedRequestMessage() {
        SignableRequestMessage requestMessage = mock(SignableRequestMessage.class);
        when (requestMessage.requestId()).thenReturn("RID");
        SignedMessage<SignableRequestMessage> signedMessage = mock(SignedMessage.class);
        when (signedMessage.getMessage()).thenReturn(requestMessage);
        when (signedMessage.getType()).thenReturn("SM");
        requestService.saveSignedRequestMessage(signedMessage);
        verify(requestRepository, times(1)).saveRequest(any(RequestEntity.class));
    }


    @Test
    public void testSaveRequestMessage() {
        RequestMessage requestMessage = mock(RequestMessage.class);
        when (requestMessage.requestId()).thenReturn("RID");
        requestService.saveRequestMessage(requestMessage);
        verify(requestRepository, times(1)).saveRequest(any(RequestEntity.class));
    }


}