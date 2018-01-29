package in.yagnyam.digana;


import lombok.*;

import java.util.Map;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "requestNumber")
public class ChequeBookRequest {

    @NonNull
    private String requestNumber;

    @NonNull
    private String accountNumber;

    @NonNull
    @Singular
    private Map<Integer, Integer> denominations;
}
