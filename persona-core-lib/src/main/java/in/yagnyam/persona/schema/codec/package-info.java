/**
 * Payload codecs: the Avro-style binary encoding ({@code docs/SCHEMA.md} §3) and the canonical
 * JSON encoding (§4), both driven by a {@link in.yagnyam.persona.schema.PersonaSchema} and both
 * operating on the value model of §4.2 — a {@code Map<String,Object>} from field name to value.
 */
package in.yagnyam.persona.schema.codec;
