package in.yagnyam.persona.registrar;

import in.yagnyam.persona.PersonaCertificate;
import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.crypto.Hash;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;

/**
 * The Registrar: sole issuer of persona certificates within a Realm, i.e. the
 * Realm's certificate authority (protocol §3.4; {@code SPEC.md} §3.4).
 *
 * <p>Responsibilities (protocol §3.4, §7.1):
 * <ol>
 *   <li>Issue a fresh, unused persona id on request.</li>
 *   <li>Accept a CSR and issue a signed certificate binding that id to the
 *       presented public key.</li>
 *   <li>Accept revocation requests authenticated by the pre-registered
 *       revocation passphrase hash.</li>
 *   <li>Publish its root certificate so peers can pin it.</li>
 * </ol>
 */
public interface PersonaRegistrar {

  /**
   * Issues a fresh, unused persona id for {@code requestId} (protocol §7.1
   * step 1).
   *
   * @param requestId client-chosen unpredictable id (UUIDv4 or equivalent);
   *                  a second request with the same requestId is rejected
   * @return fresh persona id
   * @throws IllegalArgumentException on a duplicate requestId
   */
  String issuePersonaId(String requestId);

  /**
   * Registers a persona: validates the CSR (signature + subject CN matches
   * {@code personaId}) and issues an X.509 v3 certificate signed by the
   * Registrar root (protocol §7.1 step 3).
   *
   * @param requestId              the requestId used to issue
   *                                {@code personaId}; rejected on duplicates
   * @param personaId              persona id previously issued by this
   *                                registrar for {@code requestId}
   * @param revocationPassPhraseHash salted hash of the revocation passphrase
   *                                (protocol §3.3), stored as given
   * @param csrPem                 PKCS#10 CSR with subject {@code CN=personaId}
   * @return the registered persona with its certificate and chain
   * @throws GeneralSecurityException on crypto failures
   * @throws IllegalArgumentException on unknown/duplicate requestId, persona
   *     id mismatch, invalid CSR signature or CN mismatch
   */
  RegisteredPersona registerPersona(String requestId, String personaId, Hash revocationPassPhraseHash,
      String csrPem) throws GeneralSecurityException;

  /**
   * Revokes the persona (protocol §3.4, §11 rule 11): the passphrase is
   * verified against the stored salted hash before anything is revoked.
   * Revocation is id-granular — all certificates of the persona are revoked,
   * and revoking an already-revoked persona is an idempotent success.
   *
   * @param personaId             bare persona id (no {@code #} pin)
   * @param revocationPassPhrase  the passphrase registered at persona creation
   * @throws GeneralSecurityException if the stored hash cannot be verified
   * @throws IllegalArgumentException on unknown persona or wrong passphrase
   */
  void revoke(String personaId, String revocationPassPhrase) throws GeneralSecurityException;

  /**
   * @param personaId bare persona id
   * @return {@code true} if the persona is revoked
   */
  boolean isRevoked(String personaId);

  /**
   * @param certificate persona certificate
   * @return {@code true} if the certificate's persona is revoked
   */
  boolean isRevoked(PersonaCertificate certificate);

  /**
   * @return the trusted root certificates of this registrar (protocol §3.4
   *     rule 5); one root per Realm
   */
  List<PersonaCertificate> trustedRoots();

  /**
   * Looks up a certificate for a persona id (protocol §9.1).
   *
   * <p>Pinned id: exact thumbprint match, empty on mismatch. Unpinned id:
   * the latest certificate issued for that id. Revoked personas never resolve.
   *
   * @param personaId persona id (pinned or unpinned)
   * @return matching non-revoked certificate, or empty
   */
  Optional<PersonaCertificate> certificateOf(PersonaId personaId);
}
