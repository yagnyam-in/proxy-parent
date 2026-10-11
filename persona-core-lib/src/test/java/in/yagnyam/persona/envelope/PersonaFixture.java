package in.yagnyam.persona.envelope;

import static java.nio.charset.StandardCharsets.UTF_8;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.PersonaKey;
import in.yagnyam.persona.crypto.BouncyCastleCryptographyService;
import in.yagnyam.persona.crypto.CryptographyService;
import in.yagnyam.persona.crypto.CsrService;
import in.yagnyam.persona.crypto.Hash;
import in.yagnyam.persona.crypto.PemService;
import in.yagnyam.persona.crypto.PersonaVersion;
import in.yagnyam.persona.registrar.InProcessRegistrar;
import in.yagnyam.persona.registrar.RegisteredPersona;
import java.security.KeyPair;
import java.util.Objects;
import java.util.UUID;

/**
 * Test fixture: a persona minted through the real in-process registrar,
 * together with the locally held key that registered it.
 *
 * <p>Mirrors the two-step flow of protocol §7.1 (see
 * {@code PersonaRegistrationFlow}) but keeps the {@link KeyPair}, which the
 * envelope tests need for signing.
 */
final class PersonaFixture {

  private static final CryptographyService CRYPTO = BouncyCastleCryptographyService.instance();

  private final InProcessRegistrar registrar;
  private final RegisteredPersona registered;
  private final PersonaKey key;
  private final KeyPair keyPair;

  private PersonaFixture(
      InProcessRegistrar registrar, RegisteredPersona registered, PersonaKey key, KeyPair keyPair) {
    this.registrar = registrar;
    this.registered = registered;
    this.key = key;
    this.keyPair = keyPair;
  }

  /**
   * Runs the full registration flow against {@code registrar}: issue id,
   * generate the key pair per the registrar's version policy, CSR, hash the
   * revocation passphrase, register.
   *
   * @param registrar target registrar
   * @return the fixture
   */
  static PersonaFixture mint(InProcessRegistrar registrar) {
    try {
      PersonaVersion version = registrar.version();
      String requestId = UUID.randomUUID().toString();
      String personaId = registrar.issuePersonaId(requestId);
      KeyPair keyPair =
          CRYPTO.generateKeyPair(version.keyGenerationAlgorithm(), version.keySize());
      Hash passPhraseHash =
          CRYPTO.getHash(version.preferredHashAlgorithm(), "revocation-pass".getBytes(UTF_8));
      String csr = CsrService.instance()
          .buildCsr(personaId, keyPair, version.preferredSignatureAlgorithmSet().iterator().next());
      RegisteredPersona registered =
          registrar.registerPersona(requestId, personaId, passPhraseHash, csr);
      PersonaKey key =
          PersonaKey.of(PersonaId.of(registered.certificate()), keyPair, PemService.instance());
      return new PersonaFixture(registrar, registered, key, keyPair);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to mint test persona", e);
    }
  }

  /** @return the registrar that issued this persona's certificate */
  InProcessRegistrar registrar() {
    return registrar;
  }

  /** @return the registered persona (id pinned to its certificate) */
  RegisteredPersona registered() {
    return registered;
  }

  /** @return the persona key pinned to the certificate ({@code id#thumbprint}) */
  PersonaKey key() {
    return key;
  }

  /** @return the underlying key pair */
  KeyPair keyPair() {
    return keyPair;
  }

  /**
   * @return the pinned persona id {@code <id>#<thumbprint>} (the certified
   *     identity)
   */
  PersonaId pinnedId() {
    return registered.persona().id();
  }

  /** @return the unpinned persona id {@code <id>} (the logical identity) */
  PersonaId unpinnedId() {
    return PersonaId.of(registered.persona().id().id());
  }

  /**
   * The same key material under a different persona-id spelling, e.g. the
   * unpinned {@code alice} instead of {@code alice#<thumbprint>} — what the
   * protocol §6.2 deduplication rule treats as one signer.
   *
   * @param id the spelling to use
   * @return a persona key with the same private key under {@code id}
   */
  PersonaKey keyAs(PersonaId id) {
    Objects.requireNonNull(id, "id");
    PersonaKey reIded = new PersonaKey(
        id,
        key.privateKeyPem(),
        null,
        key.privateKeySha256Thumbprint(),
        key.publicKeyPem(),
        key.publicKeySha256Thumbprint());
    reIded.setPrivateKey(keyPair.getPrivate());
    reIded.setPublicKey(keyPair.getPublic());
    return reIded;
  }
}
