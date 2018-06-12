package in.yagnyam.proxy;


public interface SignableMessage {

    String signer();

    String toReadableString();

    boolean isValid();
}
