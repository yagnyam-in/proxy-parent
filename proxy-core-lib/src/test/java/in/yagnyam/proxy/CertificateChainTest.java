package in.yagnyam.proxy;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CertificateChainTest {

  @Test
  public void testConstruction() {
    assertNotNull(CertificateChain.builder().build());
  }

}
