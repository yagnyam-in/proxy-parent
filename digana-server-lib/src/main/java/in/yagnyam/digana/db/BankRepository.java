package in.yagnyam.digana.db;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import in.yagnyam.digana.model.Bank;
import lombok.Builder;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;

import static com.googlecode.objectify.ObjectifyService.ofy;

@Builder
public class BankRepository {

    static {
        ObjectifyService.register(Bank.class);
    }

    public Optional<Bank> getBank(@NonNull String bankNumber) {
        return ObjectifyService.run(() ->
                Optional.ofNullable(ofy().load().key(Key.create(Bank.class, bankNumber)).now())
        );
    }

    public List<Bank> allBanks() {
        return ObjectifyService.run(() -> ofy().load().type(Bank.class).list());
    }

    public void saveBank(@NonNull Bank bank) {
        ObjectifyService.run(new VoidWork() {
            @Override
            public void vrun() {
                ofy().save().entity(bank).now();
            }
        });
    }
}
