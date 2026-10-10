/**
 * Envelope format, signing and verification of the Persona Protocol
 * (protocol §6, §7.2-§7.5).
 *
 * <p>{@link in.yagnyam.persona.envelope.SignedEnvelope} and
 * {@link in.yagnyam.persona.envelope.MultiSignedEnvelope} carry a payload as
 * opaque bytes together with the envelope fields that determine how the
 * payload is interpreted; {@link in.yagnyam.persona.envelope.SigningPreimage}
 * binds those fields to the payload bytes in every signature (protocol
 * §6.4); {@link in.yagnyam.persona.envelope.EnvelopeSigner} implements the
 * §7.2 signing flow; {@link in.yagnyam.persona.envelope.EnvelopeVerifier}
 * implements the §7.3 verification flow, including recursive nested-envelope
 * verification (§6.3, §7.5) and the §7.4 multi-signature quorum.
 *
 * <p>The payload bytes are signed exactly once and verified as the same
 * exact bytes; no layer re-serializes them (protocol §6.1, §11 rule 10).
 */
package in.yagnyam.persona.envelope;
