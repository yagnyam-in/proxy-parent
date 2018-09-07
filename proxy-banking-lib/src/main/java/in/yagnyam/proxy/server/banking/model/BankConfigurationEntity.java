package in.yagnyam.proxy.server.banking.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import in.yagnyam.proxy.Proxy;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Cache
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(of = {"bankId", "privateKeyId", "bankName"})
public class BankConfigurationEntity {

  @Id
  private String bankId;

  private String privateKeyId;

  private String bankName;

  @Ignore
  private Proxy proxy;

}
