package in.yagnyam.persona.p2p;

import in.yagnyam.persona.PersonaId;
import in.yagnyam.persona.Validatable;
import in.yagnyam.persona.envelope.VerificationResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The outcome of receiving an envelope off a {@link PersonaTransport}:
 * verified or rejected (never an exception for verification failures —
 * mirroring {@link VerificationResult}, protocol §4), plus the envelope's
 * message type, its signer identities and, <em>only when verified</em>, the
 * materialized payload.
 *
 * <p><strong>Fail-closed payload exposure.</strong> {@link #payload()} is
 * empty unless the envelope passed the full verification of protocol §7.3
 * (including nested envelopes and revocation at verification time): an
 * unverified payload is attacker-controlled bytes and must not reach a
 * domain handler. The metadata ({@link #messageType()}, {@link #signers()})
 * of a rejected message is populated when the envelope could be decoded —
 * useful for instrumenting rejections (protocol §14 item 9) — and is
 * {@code null}/empty when the bytes were not even a decodable envelope.
 *
 * @param multiSigned  {@code true} when the envelope was a
 *     {@code MultiSignedEnvelope} (protocol §6.2)
 * @param messageType  wire message type of the envelope, or {@code null}
 *     when the bytes were not decodable
 * @param signers      the signers declared by the envelope (one entry for a
 *     single-signed envelope), empty when undecodable; for a rejected
 *     message these are <em>claimed</em>, not verified, identities
 * @param verification the verification outcome with its reason
 * @param payload      the materialized domain message, present only when
 *     {@code verification.verified()}
 */
public record ReceivedMessage(
    boolean multiSigned,
    String messageType,
    List<PersonaId> signers,
    VerificationResult verification,
    Optional<Validatable> payload) {

  public ReceivedMessage {
    Objects.requireNonNull(signers, "signers");
    Objects.requireNonNull(verification, "verification");
    Objects.requireNonNull(payload, "payload");
    signers = List.copyOf(signers);
    if (verification.verified() && payload.isEmpty()) {
      throw new IllegalArgumentException("A verified received message must carry its payload");
    }
    if (!verification.verified() && payload.isPresent()) {
      throw new IllegalArgumentException(
          "A rejected received message must not expose its payload (fail closed)");
    }
  }

  /**
   * A verified received message (protocol §7.3 step 8).
   *
   * @param multiSigned whether the envelope was multi-signed
   * @param messageType wire message type
   * @param signers     verified signer identities
   * @param payload     materialized domain message
   * @return the received message
   */
  public static ReceivedMessage verified(
      boolean multiSigned, String messageType, List<PersonaId> signers, Validatable payload) {
    return new ReceivedMessage(multiSigned, Objects.requireNonNull(messageType, "messageType"),
        signers, VerificationResult.ok(), Optional.of(Objects.requireNonNull(payload, "payload")));
  }

  /**
   * A rejection of bytes that were not even a decodable envelope.
   *
   * @param reason why the bytes were rejected
   * @return the received message
   */
  public static ReceivedMessage rejected(String reason) {
    return new ReceivedMessage(false, null, List.of(), VerificationResult.fail(reason),
        Optional.empty());
  }

  /**
   * A rejection of a decoded envelope, retaining its claimed metadata for
   * instrumentation (protocol §14 item 9).
   *
   * @param multiSigned whether the envelope was multi-signed
   * @param messageType wire message type of the decoded envelope
   * @param signers     signers claimed by the decoded envelope
   * @param reason      why the envelope was rejected
   * @return the received message
   */
  public static ReceivedMessage rejected(
      boolean multiSigned, String messageType, List<PersonaId> signers, String reason) {
    return new ReceivedMessage(multiSigned, messageType, signers, VerificationResult.fail(reason),
        Optional.empty());
  }

  /**
   * @return {@code true} when the envelope verified (protocol §7.3) and
   *     {@link #payload()} is present
   */
  public boolean verified() {
    return verification.verified();
  }

  /**
   * Never echoes the payload: payloads MAY contain PII and log lines must
   * not leak them (protocol §12).
   */
  @Override
  public String toString() {
    return "ReceivedMessage[" + (multiSigned ? "multi" : "single")
        + ", messageType=" + messageType
        + ", signers=" + signers
        + ", " + verification + "]";
  }
}
