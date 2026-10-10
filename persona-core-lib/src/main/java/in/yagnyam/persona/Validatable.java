package in.yagnyam.persona;

/**
 * Contract for self-validating domain objects of the Persona Protocol.
 *
 * <p>Replaces the legacy {@code in.yagnyam.proxy.ProxyBaseObject} interface
 * (see the migration notes in {@code SPEC.md}): every object that crosses a
 * trust boundary must be checked with {@link #isValid()} before it is signed
 * or consumed, and malformed input must answer {@code false} rather than
 * throwing, so the verification layer can reject cleanly (protocol §4,
 * §11 rule 4).
 */
public interface Validatable {

  /**
   * Self-check of internal invariants (protocol §11 rule 4).
   *
   * <p>Implementations must be side-effect free and must not throw on
   * malformed state; an invalid object simply answers {@code false}.
   *
   * @return {@code true} if this object satisfies its domain invariants
   */
  boolean isValid();
}
