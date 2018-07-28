package in.yagnyam.proxy;


public interface SignableMessage {

    ProxyId signer();

    String toReadableString();

    boolean isValid();
}
