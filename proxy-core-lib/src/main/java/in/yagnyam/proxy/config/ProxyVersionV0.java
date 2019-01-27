package in.yagnyam.proxy.config;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor(staticName = "instance")
public class ProxyVersionV0 implements ProxyVersion {

  public static String VERSION = "v0";

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public List<Set<String>> getValidSignatureAlgorithmSets() {
    return Collections.singletonList(Collections.singleton("SHA256WithRSAEncryption"));
  }

  @Override
  public Set<String> getPreferredSignatureAlgorithmSet() {
    return Collections.singleton("SHA256WithRSAEncryption");
  }

  @Override
  public List<String> getValidEncryptionAlgorithms() {
    return Collections.singletonList("RSA/NONE/OAEPwithSHA-256andMGF1Padding");
  }

  @Override
  public String getPreferredEncryptionAlgorithm() {
    return "RSA/NONE/OAEPwithSHA-256andMGF1Padding";
  }

  @Override
  public String getCertificateSignatureAlgorithm() {
    return "SHA256WithRSAEncryption";
  }

  @Override
  public String getKeyGenerationAlgorithm() {
    return "RSA";
  }

  @Override
  public int getKeySize() {
    return 2048;
  }
}
