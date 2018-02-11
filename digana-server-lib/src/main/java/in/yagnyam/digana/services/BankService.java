package in.yagnyam.digana.services;

import in.yagnyam.digana.db.BankRepository;
import in.yagnyam.digana.model.Bank;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class BankService {

    private final BankRepository bankRepository;

    @Builder
    public BankService(@NonNull BankRepository bankRepository) {
        this.bankRepository = bankRepository;
    }

    public Optional<Bank> getBank(@NonNull String bankNumber) {
        return bankRepository.getBank(bankNumber);
    }

    public List<Bank> allBanks() {
        return bankRepository.allBanks();
    }

    public void saveBank(@NonNull Bank bank) {
        log.info("Savings Bank {}", bank);
        bankRepository.saveBank(bank);
    }
}
