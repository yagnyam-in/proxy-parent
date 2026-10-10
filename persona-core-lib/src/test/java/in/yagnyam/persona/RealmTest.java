package in.yagnyam.persona;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Realm} (protocol §3.5): constants and realm grammar.
 */
class RealmTest {

  @Test
  void hasProductionAndStagingConstants() {
    assertThat(Realm.PRODUCTION).isEqualTo("production");
    assertThat(Realm.STAGING).isEqualTo("staging");
  }

  @Test
  void acceptsSimpleRealms() {
    assertThat(Realm.isValidRealm("production")).isTrue();
    assertThat(Realm.isValidRealm("staging")).isTrue();
    assertThat(Realm.isValidRealm("dev-alice")).isTrue();
  }

  @Test
  void acceptsDottedRealms() {
    assertThat(Realm.isValidRealm("dev-alice.eu-west")).isTrue();
    assertThat(Realm.isValidRealm("a.b.c")).isTrue();
    assertThat(Realm.isValidRealm("prod1.eu")).isTrue();
  }

  @Test
  void rejectsMissingRealms() {
    assertThat(Realm.isValidRealm(null)).isFalse();
    assertThat(Realm.isValidRealm("")).isFalse();
  }

  @Test
  void rejectsEmptySegments() {
    assertThat(Realm.isValidRealm(".")).isFalse();
    assertThat(Realm.isValidRealm("foo.")).isFalse();
    assertThat(Realm.isValidRealm(".foo")).isFalse();
    assertThat(Realm.isValidRealm("foo..bar")).isFalse();
  }

  @Test
  void rejectsUppercase() {
    assertThat(Realm.isValidRealm("Production")).isFalse();
    assertThat(Realm.isValidRealm("prod.EU")).isFalse();
  }

  @Test
  void rejectsMalformedSegments() {
    assertThat(Realm.isValidRealm("dev-alice-")).isFalse();
    assertThat(Realm.isValidRealm("-dev-alice")).isFalse();
    assertThat(Realm.isValidRealm("dev alice")).isFalse();
    assertThat(Realm.isValidRealm("dev_alice")).isFalse();
  }
}
