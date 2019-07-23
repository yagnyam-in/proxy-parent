package in.yagnyam.proxy.services;

import in.yagnyam.proxy.MultiSignableMessage;
import in.yagnyam.proxy.MultiSignedMessage;
import in.yagnyam.proxy.SignableMessage;
import in.yagnyam.proxy.SignedMessage;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Builder
@Slf4j
@SuppressWarnings("unchecked")
// TODO: Lot of duplication and not properly arranged.
public class MessageFactory {

    @NonNull
    private SignedMessageFactory signedMessageFactory;

    @NonNull
    private MultiSignedMessageFactory multiSignedMessageFactory;

    public <T extends SignableMessage> SignedMessage<T> buildSignedMessage(
            String signedMessage, Class<T> underlyingMessageClass)
            throws IOException, GeneralSecurityException {
        return signedMessageFactory.buildSignedMessage(signedMessage, underlyingMessageClass);
    }

    public <T extends SignableMessage> SignedMessage<T> verifyAndPopulateSignedMessage(
            SignedMessage signedMessage, Class<T> underlyingMessageClass)
            throws IOException, GeneralSecurityException {
        return signedMessageFactory.verifyAndPopulateSignedMessage(signedMessage, underlyingMessageClass);
    }

    public SignedMessage verifyAndPopulateSignedMessage(
            SignedMessage signedMessage) throws IOException, GeneralSecurityException {
        return signedMessageFactory.verifyAndPopulateSignedMessage(signedMessage);
    }

    public <T extends SignableMessage> SignedMessage<T> populateSignedMessage(
            SignedMessage signedMessage, Class<T> underlyingMessageClass)
            throws IOException, GeneralSecurityException {
        return signedMessageFactory.populateSignedMessage(signedMessage, underlyingMessageClass);
    }

    public SignedMessage populateSignedMessage(SignedMessage signedMessage)
            throws IOException, GeneralSecurityException {
        return signedMessageFactory.populateSignedMessage(signedMessage);
    }



    public <T extends MultiSignableMessage> MultiSignedMessage<T> buildMultiSignedMessage(
            String signedMessage, Class<T> underlyingMessageClass)
            throws IOException, GeneralSecurityException {
        return multiSignedMessageFactory.buildSignedMessage(signedMessage, underlyingMessageClass);
    }

    public <T extends MultiSignableMessage> MultiSignedMessage<T> verifyAndPopulateMultiSignedMessage(
            MultiSignedMessage signedMessage, Class<T> underlyingMessageClass)
            throws IOException, GeneralSecurityException {
        return multiSignedMessageFactory.verifyAndPopulateSignedMessage(signedMessage, underlyingMessageClass);
    }

    public MultiSignedMessage verifyAndPopulateMultiSignedMessage(
            MultiSignedMessage signedMessage) throws IOException, GeneralSecurityException {
        return multiSignedMessageFactory.verifyAndPopulateSignedMessage(signedMessage);
    }

    public <T extends MultiSignableMessage> MultiSignedMessage<T> populateMultiSignedMessage(
            MultiSignedMessage signedMessage, Class<T> underlyingMessageClass)
            throws IOException, GeneralSecurityException {
        return multiSignedMessageFactory.populateSignedMessage(signedMessage, underlyingMessageClass);
    }

    public MultiSignedMessage populateMultiSignedMessage(MultiSignedMessage signedMessage)
            throws IOException, GeneralSecurityException {
        return multiSignedMessageFactory.populateSignedMessage(signedMessage);
    }


}
