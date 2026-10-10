package in.yagnyam.persona;

import java.util.regex.Pattern;

/**
 * Realm: a named deployment scope (protocol §3.5).
 *
 * <p>Realms isolate environments ({@code production}, {@code staging},
 * {@code dev-alice}), namespace persona ids, and bind to a specific
 * Registrar. Personas from different realms must never be conflated
 * (protocol §11 rule 12).
 */
public final class Realm {

  /** Production realm. */
  public static final String PRODUCTION = "production";

  /** Staging realm. */
  public static final String STAGING = "staging";

  /**
   * Realm segment: lowercase letters and digits, optionally hyphen-separated
   * (accepts the protocol's own {@code dev-alice} example, §3.5).
   */
  private static final Pattern SEGMENT_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

  private Realm() {}

  /**
   * Checks that {@code realm} is a non-empty, dot-separated sequence of
   * non-empty lowercase segments, e.g. {@code production} or
   * {@code dev-alice.eu-west}.
   *
   * @param realm candidate realm identifier
   * @return {@code true} if the identifier is a well-formed realm
   */
  public static boolean isValidRealm(String realm) {
    if (realm == null || realm.isEmpty()) {
      return false;
    }
    for (String segment : realm.split("\\.", -1)) {
      if (!SEGMENT_PATTERN.matcher(segment).matches()) {
        return false;
      }
    }
    return true;
  }
}
