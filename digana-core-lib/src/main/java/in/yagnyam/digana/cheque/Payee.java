package in.yagnyam.digana.cheque;

import lombok.*;

/**
 * Cheque payee details
 * 
 * Not all fields are mandatory, depending on Payee few fields might be optional and few might be mandatory.
 * 
 * @author Yagnyam
 *
 */

@Data
@EqualsAndHashCode(of="payeeNumber")
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@RequiredArgsConstructor(staticName = "of")
public class Payee {

    @NonNull
	private String payeeNumber;

	private String name;

	private String address;

	private String phoneNumber;
	
	public static Payee of(String payeeNumber, String payeeName) {
		Payee payee = new Payee(payeeNumber);
		payee.name = payeeName;
		return payee;
	}
}
