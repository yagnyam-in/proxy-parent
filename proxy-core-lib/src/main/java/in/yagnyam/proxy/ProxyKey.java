package in.yagnyam.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import in.yagnyam.proxy.utils.ProxyUtils;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.*;

import java.security.PrivateKey;
import java.security.PublicKey;

@Getter
@Builder(toBuilder = true)
@ToString(of = {"id", "name", "localAlias"})
public class ProxyKey implements ProxyBaseObject {

    @NonNull
    private ProxyId id;

    private String name;

    private String localAlias;

    private String privateKeyEncoded;

    private CipherText privateKeyEncodedEncrypted;

    private String privateKeySha256Thumbprint;

    private String publicKeyEncoded;

    private String publicKeySha256Thumbprint;

    @Setter
    @JsonIgnore
    private PublicKey publicKey;

    @Setter
    @JsonIgnore
    private PrivateKey privateKey;

    @JsonIgnore
    public String getUniqueId() {
        return id.uniqueId();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return ProxyUtils.isValid(id)
                && (StringUtils.isValid(privateKeyEncoded) || ProxyUtils.isValid(privateKeyEncodedEncrypted));
    }
}
