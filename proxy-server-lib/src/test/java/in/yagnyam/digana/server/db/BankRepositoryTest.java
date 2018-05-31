package in.yagnyam.digana.server.db;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;
import in.yagnyam.digana.server.model.Bank;
import in.yagnyam.proxy.server.db.RepositoryTestHelper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static in.yagnyam.proxy.server.TestUtils.sampleBank;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class BankRepositoryTest extends RepositoryTestHelper {

    private BankRepository bankRepository = BankRepository.builder().build();

    @BeforeClass
    public static void registerBank() {
        ObjectifyService.register(Bank.class);
    }


    @Test
    public void testGetBank_NotExists() {
        assertFalse(bankRepository.getBank("BID").isPresent());
    }


    @Test
    public void testGetBank() {
        Bank bank = sampleBank("BID");
        ObjectifyService.run(new VoidWork() {
            @Override
            public void vrun() {
                ofy().save().entity(bank).now();
            }
        });
        assertTrue(bankRepository.getBank("BID").isPresent());
        assertEquals(bank, bankRepository.getBank("BID").get());
    }

    @Test
    public void testAllBanks_NotExists() {
        assertTrue(bankRepository.allBanks().isEmpty());
    }


    @Test
    public void testAllBanks() {
        Bank bank = sampleBank("BID");
        ObjectifyService.run(new VoidWork() {
            @Override
            public void vrun() {
                ofy().save().entity(bank).now();
            }
        });
        assertEquals(1, bankRepository.allBanks().size());
        assertEquals(bank, bankRepository.allBanks().get(0));
    }


    @Test
    public void testSaveBank() {
        Bank bank = sampleBank("BID");
        bankRepository.saveBank(bank);
        Bank actual = ObjectifyService.run(() -> ofy().load().key(Key.create(Bank.class, "BID")).now());
        assertEquals(actual, bank);
    }
}