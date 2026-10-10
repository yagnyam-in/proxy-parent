package in.yagnyam.persona.crypto;

import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Single point of BouncyCastle JCE provider registration (protocol §8).
 *
 * <p>All JCE operations in this package (and in the registrar) go through
 * {@link #provider()}, which guarantees the provider is registered exactly
 * once, before first use, and never registered twice (duplicate registration
 * produces {@code Security.addProvider} warnings and can shadow older
 * copies of the provider).
 */
public final class BouncyCastle {

  /** Registered provider name: {@code BC}. */
  public static final String PROVIDER_NAME = BouncyCastleProvider.PROVIDER_NAME;

  private BouncyCastle() {}

  /**
   * Returns the registered BouncyCastle provider, registering it on first use.
   *
   * <p>This method is safe to call from static initializers and is
   * thread-safe.
   *
   * @return the singleton BouncyCastle JCE provider
   */
  public static synchronized BouncyCastleProvider provider() {
    BouncyCastleProvider provider = (BouncyCastleProvider) Security.getProvider(PROVIDER_NAME);
    if (provider == null) {
      provider = new BouncyCastleProvider();
      Security.addProvider(provider);
    }
    return provider;
  }
}
