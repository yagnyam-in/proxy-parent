package in.yagnyam.digana.payments;

import lombok.*;

import java.util.List;

@Data
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class PaymentRequestList {

    @NonNull
    private List<PaymentRequest> items;
}
