package in.yagnyam.proxy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class CertificateChainTest {

    @Test
    public void testConstruction() {
        assertNotNull(CertificateChain.builder().build());
    }

}
