package in.yagnyam.proxy.server.utils;

import org.junit.Test;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServletUtilsTest {

    @Test
    public void asHttpServletRequest() {
        ServletRequest servletRequest = mock(ServletRequest.class);
        assertFalse(ServletUtils.asHttpServletRequest(servletRequest).isPresent());
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        assertTrue(ServletUtils.asHttpServletRequest(httpServletRequest).isPresent());
    }

    @Test
    public void getHeader() {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getHeader(eq("NonExisting"))).thenReturn(null);
        assertFalse(ServletUtils.getHeader(httpServletRequest, "NonExisting").isPresent());
        when(httpServletRequest.getHeader(eq("Existing"))).thenReturn("Value");
        assertTrue(ServletUtils.getHeader(httpServletRequest, "Existing").isPresent());
        assertEquals("Value", ServletUtils.getHeader(httpServletRequest, "Existing").get());
    }

    @Test
    public void getRequestURI() {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getRequestURI()).thenReturn("URI");
        assertTrue(ServletUtils.getRequestURI(httpServletRequest).isPresent());
        assertEquals("URI", ServletUtils.getRequestURI(httpServletRequest).get());
    }

}