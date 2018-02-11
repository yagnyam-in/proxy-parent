package in.yagnyam.digana.services;

import in.yagnyam.digana.db.BankRepository;
import in.yagnyam.digana.model.Bank;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static in.yagnyam.digana.TestUtils.sampleBank;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BankServiceTest {

    @Test
    public void testGetBank_NotExists() {
        BankRepository bankRepository = mock(BankRepository.class);
        when (bankRepository.getBank("BID")).thenReturn(Optional.empty());
        BankService bankService = BankService.builder().bankRepository(bankRepository).build();
        assertFalse(bankService.getBank("BID").isPresent());
    }

    @Test
    public void testGetBank() {
        Bank bank = sampleBank("BID");
        BankRepository bankRepository = mock(BankRepository.class);
        when (bankRepository.getBank("BID")).thenReturn(Optional.of(bank));
        BankService bankService = BankService.builder().bankRepository(bankRepository).build();
        assertTrue(bankService.getBank("BID").isPresent());
        assertEquals(bank, bankService.getBank("BID").get());
    }

    @Test
    public void testAllBanks_NotExists() {
        BankRepository bankRepository = mock(BankRepository.class);
        when (bankRepository.allBanks()).thenReturn(Collections.emptyList());
        BankService bankService = BankService.builder().bankRepository(bankRepository).build();
        assertTrue(bankService.allBanks().isEmpty());
    }

    @Test
    public void testAllBanks() {
        Bank bank1 = sampleBank("BID1");
        Bank bank2 = sampleBank("BID2");
        BankRepository bankRepository = mock(BankRepository.class);
        when (bankRepository.allBanks()).thenReturn(Arrays.asList(bank1, bank2));
        BankService bankService = BankService.builder().bankRepository(bankRepository).build();
        assertEquals(Arrays.asList(bank1, bank2), bankService.allBanks());
    }



    @Test
    public void testSaveBank() {
        Bank bank = sampleBank("BID");
        BankRepository bankRepository = mock(BankRepository.class);
        BankService bankService = BankService.builder().bankRepository(bankRepository).build();
        bankService.saveBank(bank);
        verify(bankRepository, times(1)).saveBank(eq(bank));
    }

    @Test(expected = NullPointerException.class)
    public void testBuilder_NoRepository() {
        BankService.builder().build();
    }

    @Test
    public void testBuilder() {
        BankService.builder().bankRepository(mock(BankRepository.class)).build();
    }

}