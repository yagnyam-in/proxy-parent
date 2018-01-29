package in.yagnyam.digana.contacts;

import lombok.*;

import java.util.Map;

/**
 * Contact Update
 */
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
public class ContactUpdate {

    private String receiverNumber;

    private String phoneNumber;

    private String emailAddress;

    private String newCustomerNumber;
}
