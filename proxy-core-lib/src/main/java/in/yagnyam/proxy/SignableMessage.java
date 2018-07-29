package in.yagnyam.proxy;


import in.yagnyam.proxy.services.MessageSerializerService;

/**
 * Message that can be signed by Proxy.
 * <p>
 * Message should be annotated with Jackson bindings to be serialized properly.
 *
 * @see MessageSerializerService
 */
public interface SignableMessage {

    /**
     * Proxy Id of the Subject that must sign this message?
     *
     * @return Signed Proxy Id
     */
    ProxyId signer();

    /**
     * Return this message in human readable format
     *
     * @return Message in Human readable format
     */
    String toReadableString();

    /**
     * Tests if the message is valid
     *
     * @return true if message is valid, false otherwise
     */
    boolean isValid();

    /**
     * Type of the Message
     *
     * @return Type of the Message
     */
    default String getType() {
        return getClass().getName();
    }

    /**
     * Set type of the message. Never invoke directly, meant to be used by Json DeSerializer
     *
     * @param type Type of the message
     */
    default void setType(String type) {
        if (!getType().equals(type)) {
            throw new IllegalArgumentException("Invalid Type " + type + ". It must be " + getType());
        }
    }
}
