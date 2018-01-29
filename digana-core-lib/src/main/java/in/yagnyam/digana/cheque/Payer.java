package in.yagnyam.digana.cheque;

import lombok.*;

/**
 * Cheque payer details
 * 
 * Not all fields are mandatory, depending on Payer few fields might be optional and few might be mandatory.
 * 
 * @author Yagnyam
 *
 */

@Data
@EqualsAndHashCode(of="payerNumber")
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@RequiredArgsConstructor(staticName = "of")
public class Payer {

    @NonNull
	private String payerNumber;

	private String name;
	
	private String address;

	private String phoneNumber;
}
