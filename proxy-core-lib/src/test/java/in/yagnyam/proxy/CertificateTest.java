package in.yagnyam.proxy;

import static org.junit.Assert.assertEquals;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class CertificateTest {

  @Test
  public void testGetId() {
    Certificate certificate = TestUtils.sampleCertificateForId("123", "sha256");
    assertEquals("123", certificate.getId());
    assertEquals("123", certificate.getOwner());
  }

}
