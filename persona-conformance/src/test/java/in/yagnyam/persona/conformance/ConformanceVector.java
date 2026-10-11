package in.yagnyam.persona.conformance;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One vector entry from a vector file (see {@code vectors/README.md}).
 *
 * @param file     the vector file the entry came from, as a repository-relative path
 * @param kind     the file's {@code kind}, which fixes the entry's contract
 * @param document the entry as parsed JSON; never {@code null}
 */
record ConformanceVector(String file, String kind, JsonNode document) {

  /** The value of an {@code expect} key that marks a negative vector. */
  static final String EXPECT_REJECT = "reject";

  /** @return the stable, globally unique vector id */
  String id() {
    return text("id");
  }

  /** @return the normative citation this vector pins */
  String spec() {
    return text("spec");
  }

  /** @return what the vector proves */
  String description() {
    return text("description");
  }

  /**
   * @return {@code true} when the vector is negative: the operation must fail
   */
  boolean expectsRejection() {
    JsonNode node = document.get("expect");
    if (node == null) {
      return false;
    }
    if (node.isTextual() && EXPECT_REJECT.equals(node.textValue())) {
      return true;
    }
    throw new VectorFormatException(
        describe() + ": 'expect' must be \"reject\" or absent, found " + node);
  }

  /** @return the rule (with citation) that mandates the rejection */
  String rejectionRule() {
    return text("rejectionRule");
  }

  /** @return {@code true} when the entry carries the key */
  boolean has(String key) {
    return document.has(key);
  }

  /** @return the entry's value for {@code key}, or {@code null} when absent */
  JsonNode node(String key) {
    return document.get(key);
  }

  /**
   * Returns the entry's value for {@code key}, failing hard when absent.
   *
   * @param key the key
   * @return the value, which may itself be JSON {@code null}
   * @throws VectorFormatException when the key is missing
   */
  JsonNode require(String key) {
    JsonNode node = document.get(key);
    if (node == null) {
      throw new VectorFormatException(describe() + ": missing key '" + key + "'");
    }
    return node;
  }

  /**
   * Returns the entry's textual value for {@code key}.
   *
   * @param key the key
   * @return the text (a JSON {@code null} is absent, not an empty string)
   * @throws VectorFormatException when the key is missing or not a string
   */
  String text(String key) {
    JsonNode node = document.get(key);
    if (node == null || node.isNull()) {
      throw new VectorFormatException(describe() + ": missing key '" + key + "'");
    }
    if (!node.isTextual()) {
      throw new VectorFormatException(describe() + ": key '" + key + "' must be a string, found " + node);
    }
    return node.textValue();
  }

  /**
   * Returns the entry's textual value for {@code key}, or {@code null} when the
   * key is absent or JSON {@code null} (used for optional values like an
   * unpinned thumbprint).
   *
   * @param key the key
   * @return the text, or {@code null}
   * @throws VectorFormatException when the key is present but not a string
   */
  String optionalText(String key) {
    JsonNode node = document.get(key);
    if (node == null || node.isNull()) {
      return null;
    }
    if (!node.isTextual()) {
      throw new VectorFormatException(describe() + ": key '" + key + "' must be a string, found " + node);
    }
    return node.textValue();
  }

  /**
   * Returns the entry's boolean value for {@code key}.
   *
   * @param key the key
   * @return the boolean
   * @throws VectorFormatException when the key is missing or not a boolean
   */
  boolean booleanValue(String key) {
    JsonNode node = document.get(key);
    if (node == null || !node.isBoolean()) {
      throw new VectorFormatException(describe() + ": key '" + key + "' must be a boolean, found " + node);
    }
    return node.booleanValue();
  }

  /**
   * Returns the entry's integral value for {@code key}.
   *
   * @param key the key
   * @return the int value
   * @throws VectorFormatException when the key is missing or not an integer
   */
  int intValue(String key) {
    JsonNode node = document.get(key);
    if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
      throw new VectorFormatException(describe() + ": key '" + key + "' must be an int, found " + node);
    }
    return node.intValue();
  }

  /** @return {@code "id" (kind) from file}, for failure messages */
  String describe() {
    String id = document != null && document.hasNonNull("id") && document.get("id").isTextual()
        ? document.get("id").textValue()
        : "<missing id>";
    return id + " (" + kind + ") from " + file;
  }
}
