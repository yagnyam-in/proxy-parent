package in.yagnyam.digana.cheque;

import lombok.*;

/**
 * <p>Bank represents any financial institution which maintains users money</p>
 * 
 * @author Yagnyam
 *
 */
@Data
@EqualsAndHashCode(of="bankNumber")
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(staticName = "of")
public class Bank {

	@NonNull
	private String bankNumber;

	private String name;

}
