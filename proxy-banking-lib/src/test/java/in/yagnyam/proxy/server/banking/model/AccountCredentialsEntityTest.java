package in.yagnyam.proxy.server.banking.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class AccountCredentialsEntityTest {

  @Test
  public void testBuild() {
    AccountCredentialsEntity credentialsEntity = AccountCredentialsEntity.accountCredentialsEntityBuilder()
        .credentialId("cid")
        .build();
    assertNotNull(credentialsEntity);
    assertEquals("cid", credentialsEntity.getCredentialId());
  }
}
