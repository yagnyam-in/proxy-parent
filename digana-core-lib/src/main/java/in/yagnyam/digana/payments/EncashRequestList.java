package in.yagnyam.digana.payments;

import in.yagnyam.digana.cheque.Cheque;
import lombok.*;

import java.util.List;

@Data
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class EncashRequestList {

    @NonNull
    private List<EncashRequest> items;
}
