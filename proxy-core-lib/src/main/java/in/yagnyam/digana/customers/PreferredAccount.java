package in.yagnyam.digana.customers;


import com.fasterxml.jackson.annotation.JsonInclude;
import in.yagnyam.digana.AccountNumber;
import lombok.*;


/**
 * Default account to receive the funds
 * TODO:
 * <ol>
 *     <li>Rename this to receiving account</li>
 *     <li>Add more attributes to support more countries</li>
 * </ol>
 */
@Getter
@Builder(toBuilder = true)
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreferredAccount {

    private AccountNumber diganAccountNumber;

    private String customerName;

    private String customerAccountNumber;

}