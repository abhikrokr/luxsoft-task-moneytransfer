package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.MoneyTransfer;
import com.dws.challenge.exception.AccountIdNotFoundException;
import com.dws.challenge.exception.MinTransferAmountException;
import com.dws.challenge.exception.NotEnoughBalanceException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


@Service
public class AccountsService {

    @Getter
    private final AccountsRepository accountsRepository;

    private final NotificationService notificationService;

    @Autowired
    public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
        this.accountsRepository = accountsRepository;
        this.notificationService = notificationService;
    }

    private static final String PayerNotification = "Amount of $$$ has to sent to Account number ###";

    private static final String ReceiverNotification = "Amount of $$$ has been received from Account number ###";

    public void createAccount(Account account) {
        this.accountsRepository.createAccount(account);
    }

    public void updateAccount(Account account) {
        this.accountsRepository.updateAccount(account);
    }

    public Account getAccount(String accountId) {
        return this.accountsRepository.getAccount(accountId);
    }

    public void transferMoney(MoneyTransfer moneyTransfer) {
        if (new BigDecimal("0.1").max(moneyTransfer.getAmount()).equals(new BigDecimal("0.1"))) {
            throw new MinTransferAmountException();
        }
        String lock1 = moneyTransfer.getAccountFrom().hashCode() > moneyTransfer.getAccountTo().hashCode() ?
                moneyTransfer.getAccountFrom() : moneyTransfer.getAccountTo();
        String lock2 = moneyTransfer.getAccountFrom().hashCode() > moneyTransfer.getAccountTo().hashCode() ?
                moneyTransfer.getAccountTo() : moneyTransfer.getAccountFrom();

        synchronized (lock1.intern()) {
            synchronized (lock2.intern()) {
                Account fromAcc = this.accountsRepository.getAccount(moneyTransfer.getAccountFrom());
                if (null == fromAcc) {
                    throw new AccountIdNotFoundException(moneyTransfer.getAccountFrom());
                }
                Account tooAcc = this.accountsRepository.getAccount(moneyTransfer.getAccountTo());
                if (null == tooAcc) {
                    throw new AccountIdNotFoundException(moneyTransfer.getAccountTo());
                }
                if (fromAcc.getBalance().compareTo(moneyTransfer.getAmount()) < 0) {
                    throw new NotEnoughBalanceException();
                }

                fromAcc.updateBalance(moneyTransfer.getAmount(), false);
                updateAccount(fromAcc);
                tooAcc.updateBalance(moneyTransfer.getAmount(), true);
                updateAccount(tooAcc);

                this.notificationService.notifyAboutTransfer(fromAcc, PayerNotification
                        .replace("$$$", moneyTransfer.getAmount().toString())
                        .replace("###", tooAcc.getAccountId()));

                this.notificationService.notifyAboutTransfer(tooAcc, ReceiverNotification
                        .replace("$$$", moneyTransfer.getAmount().toString())
                        .replace("###", fromAcc.getAccountId()));

            }
        }
    }
}
