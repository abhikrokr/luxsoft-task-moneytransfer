package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.MoneyTransfer;
import com.dws.challenge.exception.AccountIdNotFoundException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.MinTransferAmountException;
import com.dws.challenge.exception.NotEnoughBalanceException;
import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  @Test
  void updateAccount() {
    String from = UUID.randomUUID().toString();
    Account account = new Account(from);
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    Account updateAccount = this.accountsService.getAccount(from);
    assertThat(updateAccount.getBalance()).isEqualByComparingTo(new BigDecimal(1000));
    updateAccount.setBalance(new BigDecimal(2000));
    this.accountsService.updateAccount(updateAccount);
    assertThat(this.accountsService.getAccount(from).getBalance()).isEqualByComparingTo(new BigDecimal(2000));
  }

  @Test
  void TransferMoneyTest() {

    String from = UUID.randomUUID().toString();
    Account fromAccount = new Account(from);
    fromAccount.setBalance(new BigDecimal(100));
    this.accountsService.createAccount(fromAccount);

    String too = UUID.randomUUID().toString();
    Account toAccount = new Account(too);
    toAccount.setBalance(new BigDecimal(100));
    this.accountsService.createAccount(toAccount);

    MoneyTransfer mo = new MoneyTransfer(from,too,new BigDecimal(50));
    accountsService.transferMoney(mo);


    Account pay = accountsService.getAccount(from);
    Account rei = accountsService.getAccount(too);
    assertThat(pay.getBalance()).isEqualByComparingTo(new BigDecimal(50));
    assertThat(rei.getBalance()).isEqualByComparingTo(new BigDecimal(150));
  }

  @Test
  void concurrentTransferMoneyTestDifferentAccount() {

    Random ran = new Random();
    ExecutorService e = Executors.newFixedThreadPool(10);
    for(int i = 0;i < 1000;i++){
      e.submit(new Runnable() {
        //simulating multiple transactions in parallel
        @Override
        public void run() {
          String from = UUID.randomUUID().toString();
          Account fromAccount = new Account(from,new BigDecimal(10000));
          accountsService.createAccount(fromAccount);

          String too = UUID.randomUUID().toString();
          Account toAccount = new Account(too,new BigDecimal(0));
          accountsService.createAccount(toAccount);

          BigDecimal amount = new BigDecimal(ran.nextInt(10)+1);
          MoneyTransfer mo = new MoneyTransfer(from,too,amount);
          accountsService.transferMoney(mo);

          Account pay = accountsService.getAccount(from);
          Account rei = accountsService.getAccount(too);
          assertThat(pay.getBalance().add(rei.getBalance())).isEqualByComparingTo(new BigDecimal(10000));
        }
      });
    }
  }

  @Test
  void concurrentTransferMoneyTestSameAccount() {

    String from = UUID.randomUUID().toString();
    Account fromAccount = new Account(from);
    fromAccount.setBalance(new BigDecimal(10000));
    this.accountsService.createAccount(fromAccount);

    String too = UUID.randomUUID().toString();
    Account toAccount = new Account(too);
    toAccount.setBalance(new BigDecimal(0));
    this.accountsService.createAccount(toAccount);

    Random ran = new Random();
    ExecutorService e = Executors.newFixedThreadPool(10);
    for(int i = 0;i < 1000;i++){
      e.submit(new Runnable() {
        //simulating multiple transactions in parallel
        @Override
        public void run() {
          BigDecimal amount = new BigDecimal(ran.nextInt(10)+1);
          MoneyTransfer mo = new MoneyTransfer(from,too,amount);
          accountsService.transferMoney(mo);
        }
      });
    }

    Account pay = accountsService.getAccount(from);
    Account rei = accountsService.getAccount(too);
    //All the transaction should be consistent and sum of both balance should be 10000
    //As all transaction where made from account having balance 10000 to account having balance 0
    assertThat(pay.getBalance().add(rei.getBalance())).isEqualByComparingTo(new BigDecimal(10000));
  }

  @Test
  void TransferMoneyNotEnoughBalanceExceptionTest() {
    String from = UUID.randomUUID().toString();
    Account fromAccount = new Account(from);
    fromAccount.setBalance(new BigDecimal(100));
    this.accountsService.createAccount(fromAccount);

    String too = UUID.randomUUID().toString();
    Account toAccount = new Account(too);
    toAccount.setBalance(new BigDecimal(100));
    this.accountsService.createAccount(toAccount);

    MoneyTransfer mo = new MoneyTransfer(from,too,new BigDecimal("100.1"));
    try{
      accountsService.transferMoney(mo);
      fail();
    }catch(NotEnoughBalanceException e){
      assertThat(e.getMessage()).isEqualTo("Not Enough Balance");
      Account pay = accountsService.getAccount(from);
      Account rei = accountsService.getAccount(too);
      assertThat(pay.getBalance()).isEqualByComparingTo(new BigDecimal(100));
      assertThat(rei.getBalance()).isEqualByComparingTo(new BigDecimal(100));
    }
  }

  @Test
  void TransferMoneyMinTransferAmountExceptionTest() {
    String from = UUID.randomUUID().toString();
    Account fromAccount = new Account(from);
    fromAccount.setBalance(new BigDecimal(100));
    this.accountsService.createAccount(fromAccount);

    String too = UUID.randomUUID().toString();
    Account toAccount = new Account(too);
    toAccount.setBalance(new BigDecimal(100));
    this.accountsService.createAccount(toAccount);

    MoneyTransfer mo = new MoneyTransfer(from,too,new BigDecimal("0.00"));
    try{
      accountsService.transferMoney(mo);
      fail();
    }catch(MinTransferAmountException e){
      assertThat(e.getMessage()).isEqualTo("Minimum transfer amount should be more than 0.1");
      Account pay = accountsService.getAccount(from);
      Account rei = accountsService.getAccount(too);
      assertThat(pay.getBalance()).isEqualByComparingTo(new BigDecimal(100));
      assertThat(rei.getBalance()).isEqualByComparingTo(new BigDecimal(100));
    }
  }

  @Test
  void TransferMoneyAccountIdNotFoundExceptionTest() {
    String from = UUID.randomUUID().toString();
    Account fromAccount = new Account(from);
    fromAccount.setBalance(new BigDecimal(100));
    this.accountsService.createAccount(fromAccount);

    String too = UUID.randomUUID().toString();
    Account toAccount = new Account(too);
    toAccount.setBalance(new BigDecimal(100));
    this.accountsService.createAccount(toAccount);

    String random = UUID.randomUUID().toString();
    MoneyTransfer mo = new MoneyTransfer(random,too,new BigDecimal("10.00"));
    try{
      accountsService.transferMoney(mo);
      fail();
    }catch(AccountIdNotFoundException e){
      assertThat(e.getMessage()).isEqualTo("Account not found : "+random);
      try{
        mo = new MoneyTransfer(from,random,new BigDecimal("10.00"));
        accountsService.transferMoney(mo);
        fail();
      }catch(AccountIdNotFoundException e1){
        assertThat(e.getMessage()).isEqualTo("Account not found : "+random);
      }
    }
  }

}
