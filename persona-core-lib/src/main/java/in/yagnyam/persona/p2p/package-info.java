/**
 * Persona-to-persona messaging (protocol §13 reference architecture): the
 * transport-agnostic language pieces every P2P transport needs — a minimal
 * {@link in.yagnyam.persona.p2p.PersonaTransport} contract, an in-memory
 * implementation for tests and demos, the
 * {@link in.yagnyam.persona.p2p.PersonaMessenger} tying signer, verifier and
 * transport together, and the bounded
 * {@link in.yagnyam.persona.p2p.RequestDeduplicator} of protocol §5.2.
 */
package in.yagnyam.persona.p2p;
