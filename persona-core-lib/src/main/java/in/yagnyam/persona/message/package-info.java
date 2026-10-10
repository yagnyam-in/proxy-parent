/**
 * The five orthogonal message-model contracts of the Persona Protocol
 * (protocol §5).
 *
 * <p>A domain payload implements any subset of these contracts:
 * {@link in.yagnyam.persona.message.SignableMessage} (§5.1, the base signing
 * contract), {@link in.yagnyam.persona.message.RequestMessage} (§5.2,
 * idempotent flows), {@link in.yagnyam.persona.message.AddressableMessage}
 * (§5.3, routing), {@link in.yagnyam.persona.message.AlertMessage} (§5.4,
 * broadcast) and {@link in.yagnyam.persona.message.MultiSignableMessage}
 * (§5.5, quorum). The contracts are encoding- and transport-agnostic; the
 * envelope layer in {@code in.yagnyam.persona.envelope} turns them into
 * signed, verifiable envelopes (protocol §6, §7.2, §7.3).
 */
package in.yagnyam.persona.message;
