package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class AccountsControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private AccountsService accountsService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void prepareMockMvc() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

        // Reset the existing accounts before each test.
        accountsService.getAccountsRepository().clearAccounts();
    }

    @Test
    void createAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        Account account = accountsService.getAccount("Id-123");
        assertThat(account.getAccountId()).isEqualTo("Id-123");
        assertThat(account.getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    void createDuplicateAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNoAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNoBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNoBody() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNegativeBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountEmptyAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void getAccount() throws Exception {
        String uniqueAccountId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
        this.accountsService.createAccount(account);
        this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
                .andExpect(status().isOk())
                .andExpect(
                        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
    }

    @Test
    void transferMoney() throws Exception {
        String from = UUID.randomUUID().toString();
        Account fromAccount = new Account(from, new BigDecimal("123.45"));
        this.accountsService.createAccount(fromAccount);

        String to = UUID.randomUUID().toString();
        Account toAccount = new Account(to, new BigDecimal("100.00"));
        this.accountsService.createAccount(toAccount);

        this.mockMvc.perform(post("/v1/accounts/transferMoney").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountFrom\":\"" + from + "\",\"accountTo\":\"" + to + "\",\"amount\":23.45}")).andExpect(status().isOk());

        Account account = accountsService.getAccount(from);
        assertThat(account.getAccountId()).isEqualTo(from);
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");

        account = accountsService.getAccount(to);
        assertThat(account.getAccountId()).isEqualTo(to);
        assertThat(account.getBalance()).isEqualByComparingTo("123.45");
    }

    @Test
    void transferMoneyMoreThanBalance() throws Exception {
        String from = UUID.randomUUID().toString();
        Account fromAccount = new Account(from, new BigDecimal("123.45"));
        this.accountsService.createAccount(fromAccount);

        String to = UUID.randomUUID().toString();
        Account toAccount = new Account(to, new BigDecimal("100.00"));
        this.accountsService.createAccount(toAccount);

        this.mockMvc.perform(post("/v1/accounts/transferMoney").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountFrom\":\"" + from + "\",\"accountTo\":\"" + to + "\",\"amount\":123.46}")).andExpect(status().isBadRequest());

        Account account = accountsService.getAccount(from);
        assertThat(account.getAccountId()).isEqualTo(from);
        assertThat(account.getBalance()).isEqualByComparingTo("123.45");

        account = accountsService.getAccount(to);
        assertThat(account.getAccountId()).isEqualTo(to);
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void transferMoneyEqualBalance() throws Exception {
        String from = UUID.randomUUID().toString();
        Account fromAccount = new Account(from, new BigDecimal("123.45"));
        this.accountsService.createAccount(fromAccount);

        String to = UUID.randomUUID().toString();
        Account toAccount = new Account(to, new BigDecimal("100.00"));
        this.accountsService.createAccount(toAccount);

        this.mockMvc.perform(post("/v1/accounts/transferMoney").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountFrom\":\"" + from + "\",\"accountTo\":\"" + to + "\",\"amount\":123.45}")).andExpect(status().isOk());

        Account account = accountsService.getAccount(from);
        assertThat(account.getAccountId()).isEqualTo(from);
        assertThat(account.getBalance()).isEqualByComparingTo("0.00");

        account = accountsService.getAccount(to);
        assertThat(account.getAccountId()).isEqualTo(to);
        assertThat(account.getBalance()).isEqualByComparingTo("223.45");
    }

    @Test
    void transferMoneyRequestValidation() throws Exception {
        String from = UUID.randomUUID().toString();
        Account fromAccount = new Account(from, new BigDecimal("123.45"));
        this.accountsService.createAccount(fromAccount);

        String to = UUID.randomUUID().toString();
        Account toAccount = new Account(to, new BigDecimal("100.00"));
        this.accountsService.createAccount(toAccount);
        //Validate accountFrom
        this.mockMvc.perform(post("/v1/accounts/transferMoney").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountFrom\":\"" + UUID.randomUUID() + "\",\"accountTo\":\"" + to + "\",\"amount\":123.46}")).andExpect(status().isNotFound());
        //Validate accountTo
        this.mockMvc.perform(post("/v1/accounts/transferMoney").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountFrom\":\"" + from + "\",\"accountTo\":\"" + UUID.randomUUID() + "\",\"amount\":123.46}")).andExpect(status().isNotFound());
        //Validate negative transfer amount
        this.mockMvc.perform(post("/v1/accounts/transferMoney").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountFrom\":\"" + from + "\",\"accountTo\":\"" + to + "\",\"amount\":-123.46}")).andExpect(status().isBadRequest());
        //Validate minimum transfer amount
        this.mockMvc.perform(post("/v1/accounts/transferMoney").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountFrom\":\"" + from + "\",\"accountTo\":\"" + to + "\",\"amount\":0.01}")).andExpect(status().isBadRequest());

        Account account = accountsService.getAccount(from);
        assertThat(account.getAccountId()).isEqualTo(from);
        assertThat(account.getBalance()).isEqualByComparingTo("123.45");

        account = accountsService.getAccount(to);
        assertThat(account.getAccountId()).isEqualTo(to);
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }
}
