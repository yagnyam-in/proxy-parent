package in.yagnyam.proxy.payments;


import lombok.*;

/**
 * <p>Amount class represents certain amount in a given currency</p>
 * 
 * @author Yagnyam
 * @see Currency
 *
 */
@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(staticName = "of")
public class Amount {

    @NonNull
	private Currency currency;

	private double value;
}
