package in.yagnyam.proxy.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * Database Entity to store Certificate for this module
 */
@Entity
@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(of = "id")
public class PrivateKeyEntity {

  @Id
  @NonNull
  private String id;

  private String name;

  private String email;

  private String phoneNumber;

  private String postalAddress;

  @Index
  private String certificateSerialNumber;

  @Index
  private String certificateSha256Thumbprint;

  @JsonIgnore
  private String certificateRequestEncoded;

  @JsonIgnore
  private String privateKeyAlgorithm;

  @JsonIgnore
  private String privateKeyEncoded;

  @JsonIgnore
  private String certificateEncoded;

  @JsonIgnore
  @Setter
  @Ignore
  private PrivateKey privateKey;

  @JsonIgnore
  @Setter
  @Ignore
  private X509Certificate certificate;
}
