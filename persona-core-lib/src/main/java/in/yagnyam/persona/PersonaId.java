package in.yagnyam.persona;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Persona identifier (protocol §3.2; {@code SPEC.md} §3.1.2, was
 * {@code ProxyId}).
 *
 * <p>Canonical string form (what {@link #uniqueId()} and {@link #toString()}
 * produce):
 * <pre>
 * &lt;id&gt;                     # unpinned — matches any certificate for this id
 * &lt;id&gt;#&lt;thumbprint&gt;        # pinned to a specific certificate
 * </pre>
 *
 * <p>{@code id} is an opaque, URL-safe string chosen by the Registrar;
 * {@code thumbprint} is the 64-character lowercase-hex SHA-256 digest of the
 * certificate's DER encoding.
 *
 * <p>The wildcard token is {@code any} (or {@code any#any}); see
 * {@link #any()}.
 */
public record PersonaId(String id, Optional<String> sha256Thumbprint) implements Validatable {

  /** Wildcard id token (protocol §3.2: schemas may accept "any signer"). */
  static final String WILDCARD_ID = "any";

  /** Wildcard thumbprint token; exempt from the hex rule for the wildcard only. */
  static final String WILDCARD_THUMBPRINT = "any";

  private static final Pattern THUMBPRINT_PATTERN = Pattern.compile("[0-9a-f]{64}");

  public PersonaId {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(sha256Thumbprint, "sha256Thumbprint");
    if (id.indexOf('#') >= 0) {
      throw new IllegalArgumentException("PersonaId id must not contain '#': " + id);
    }
    sha256Thumbprint.ifPresent(thumbprint -> {
      if (!isAcceptableThumbprint(id, thumbprint)) {
        throw new IllegalArgumentException(
            "Invalid sha256Thumbprint (64 lowercase hex expected): " + thumbprint);
      }
    });
  }

  /**
   * Parses a canonical {@code <id>} or {@code <id>#<thumbprint>} form.
   *
   * <p>Rejected: {@code null}, empty id, leading {@code #}, more than one
   * {@code #}, and any present thumbprint that is not 64 lowercase hex
   * characters (the wildcard {@code any#any} being the sole exception).
   *
   * @param uniqueId canonical string form
   * @return parsed persona id
   * @throws IllegalArgumentException on any malformed input
   */
  public static PersonaId of(String uniqueId) {
    Objects.requireNonNull(uniqueId, "uniqueId");
    if (uniqueId.startsWith("#")) {
      throw new IllegalArgumentException("Invalid PersonaId (id must not start with '#'): " + uniqueId);
    }
    int separator = uniqueId.indexOf('#');
    if (separator < 0) {
      return of(uniqueId, null);
    }
    String id = uniqueId.substring(0, separator);
    String thumbprint = uniqueId.substring(separator + 1);
    if (thumbprint.indexOf('#') >= 0) {
      throw new IllegalArgumentException("Invalid PersonaId (more than one '#'): " + uniqueId);
    }
    return of(id, thumbprint);
  }

  /**
   * Creates a persona id, optionally pinned to a certificate thumbprint.
   *
   * @param id              registrar-issued id, non-empty
   * @param sha256Thumbprint 64 lowercase hex characters, or {@code null} for
   *                          an unpinned id
   * @return persona id
   * @throws IllegalArgumentException on an empty id, a {@code #} inside the
   *     id, or a malformed thumbprint
   */
  public static PersonaId of(String id, String sha256Thumbprint) {
    Objects.requireNonNull(id, "id");
    if (id.isEmpty()) {
      throw new IllegalArgumentException("PersonaId id must not be empty");
    }
    if (id.indexOf('#') >= 0) {
      throw new IllegalArgumentException("PersonaId id must not contain '#': " + id);
    }
    if (sha256Thumbprint == null) {
      return new PersonaId(id, Optional.empty());
    }
    if (!isAcceptableThumbprint(id, sha256Thumbprint)) {
      throw new IllegalArgumentException(
          "Invalid sha256Thumbprint (64 lowercase hex expected): " + sha256Thumbprint);
    }
    return new PersonaId(id, Optional.of(sha256Thumbprint));
  }

  /**
   * Creates the pinned persona id of {@code certificate} (owner + certificate
   * thumbprint).
   *
   * @param certificate persona certificate
   * @return pinned persona id
   */
  public static PersonaId of(PersonaCertificate certificate) {
    Objects.requireNonNull(certificate, "certificate");
    return of(certificate.owner(), certificate.sha256Thumbprint());
  }

  /**
   * Returns the wildcard persona id {@code any#any} (protocol §3.2), for use
   * in schemas that intend "any signer is acceptable".
   *
   * @return wildcard persona id
   */
  public static PersonaId any() {
    return new PersonaId(WILDCARD_ID, Optional.of(WILDCARD_THUMBPRINT));
  }

  /**
   * Delegation rule (protocol §3.2). A persona id {@code X} can sign on behalf
   * of {@code other} iff:
   * <ul>
   *   <li>{@code other} is the wildcard; or</li>
   *   <li>{@code other.id == X.id} and {@code other} is unpinned; or</li>
   *   <li>{@code X == other} exactly.</li>
   * </ul>
   *
   * @param other id being signed for (may be {@code null}, then {@code false})
   * @return {@code true} if this id may sign on behalf of {@code other}
   */
  public boolean canSignOnBehalfOf(PersonaId other) {
    if (other == null) {
      return false;
    }
    if (other.isWildcard()) {
      return true;
    }
    if (other.id.equals(id) && other.sha256Thumbprint.isEmpty()) {
      return true;
    }
    return equals(other);
  }

  /**
   * @return {@code true} for the wildcard spellings {@code any} and
   *     {@code any#any}
   */
  public boolean isWildcard() {
    return WILDCARD_ID.equals(id)
        && (sha256Thumbprint.isEmpty() || WILDCARD_THUMBPRINT.equals(sha256Thumbprint.get()));
  }

  /**
   * @return canonical string form {@code id} or {@code id#thumbprint}
   */
  public String uniqueId() {
    return sha256Thumbprint.map(thumbprint -> id + "#" + thumbprint).orElse(id);
  }

  /**
   * @return {@code true} when the id is a non-empty string (protocol §11
   *     rule 7: equality is structural over id and thumbprint)
   */
  @Override
  public boolean isValid() {
    return !id.isEmpty();
  }

  @Override
  public String toString() {
    return uniqueId();
  }

  private static boolean isAcceptableThumbprint(String id, String thumbprint) {
    if (WILDCARD_THUMBPRINT.equals(thumbprint)) {
      // The non-hex "any" thumbprint exists only for the wildcard constant.
      return WILDCARD_ID.equals(id);
    }
    return isCanonicalThumbprint(thumbprint);
  }

  /**
   * Whether {@code thumbprint} is a canonical certificate thumbprint (64
   * lowercase hex characters), excluding the wildcard token.
   *
   * <p>Package-private so identity records (e.g. {@code Persona}) can tell a
   * canonical thumbprint from an absent or malformed one without duplicating
   * the grammar.
   *
   * @param thumbprint candidate thumbprint, may be {@code null}
   * @return {@code true} for a 64-character lowercase hex string
   */
  static boolean isCanonicalThumbprint(String thumbprint) {
    return thumbprint != null && THUMBPRINT_PATTERN.matcher(thumbprint).matches();
  }
}
