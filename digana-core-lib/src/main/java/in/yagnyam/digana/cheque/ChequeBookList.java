package in.yagnyam.digana.cheque;

import lombok.*;

import java.util.List;


@Data
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ChequeBookList {

    @NonNull
    private List<ChequeBook> items;

}
