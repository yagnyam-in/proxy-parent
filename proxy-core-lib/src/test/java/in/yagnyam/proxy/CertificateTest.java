package in.yagnyam.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class CertificateTest {

  private void assertIllegalArgument(String message, Runnable runnable) {
    try {
      runnable.run();
      fail(message);
    } catch (IllegalArgumentException e) {
      log.debug(message + " - is not true", e);
    }
  }

  @Test
  public void testExtractOnlyId_Invalid() {
    assertIllegalArgument("null is invalid certificate id", () -> Certificate.extractOnlyId(null));
    assertIllegalArgument("empty string is invalid certificate id",
        () -> Certificate.extractOnlyId(" "));
    assertIllegalArgument("certificate id with more than 2 tokens is invalid",
        () -> Certificate.extractOnlyId("abc#dev#123"));
  }

  @Test
  public void testExtractOnlyId() {
    assertEquals("123", Certificate.extractOnlyId("123"));
    assertEquals("abc", Certificate.extractOnlyId("abc"));
    assertEquals("456", Certificate.extractOnlyId("456#123"));
  }

  @Test
  public void testExtractSha256Thumbprint_Invalid() {
    assertIllegalArgument("null is invalid certificate id",
        () -> Certificate.extractSha256Thumbprint(null));
    assertIllegalArgument("empty string is invalid certificate id",
        () -> Certificate.extractSha256Thumbprint(" "));
    assertIllegalArgument("certificate id with more than 2 tokens is invalid",
        () -> Certificate.extractSha256Thumbprint("abc#dev#123"));
  }

  @Test
  public void testExtractSha256Thumbprint() {
    assertFalse(Certificate.extractSha256Thumbprint("123").isPresent());
    assertFalse(Certificate.extractSha256Thumbprint("abc").isPresent());
    assertTrue(Certificate.extractSha256Thumbprint("456#123").isPresent());
    assertEquals("123", Certificate.extractSha256Thumbprint("456#123").get());
  }
}
