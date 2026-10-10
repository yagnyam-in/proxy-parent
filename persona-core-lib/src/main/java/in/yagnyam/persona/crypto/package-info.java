/**
 * Cryptographic primitives and policy of the Persona Protocol (protocol §8).
 *
 * <p>Contains the BouncyCastle-backed cryptography service, PEM codec, CSR
 * builder, salted hash/HMAC/cipher-text records, and the versioned
 * algorithm policy ({@code PersonaVersion}).
 *
 * <p>This package is self-contained: it must not depend on the
 * {@code in.yagnyam.persona} root package so the crypto layer can be reused
 * stand-alone.
 */
package in.yagnyam.persona.crypto;
