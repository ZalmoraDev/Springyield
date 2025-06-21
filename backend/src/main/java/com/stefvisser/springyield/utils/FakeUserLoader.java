package com.stefvisser.springyield.utils;

import com.github.javafaker.Faker;
import com.stefvisser.springyield.models.*;
import com.stefvisser.springyield.services.UserService;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class FakeUserLoader {
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final Faker faker;
    private final List<Transaction> generatedTransactions = new ArrayList<>();

    public FakeUserLoader(UserService service) {
        Locale locale = new Locale("nl", "NL");
        this.faker = new Faker(locale);
        this.bCryptPasswordEncoder = service.getPasswordEncoder();
    }

    public List<User> generateFakeUsers(int count) {
        List<User> fakeUsers = new ArrayList<>();
        List<String> generatedIbans = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            String firstName = faker.name().firstName();
            String lastName = faker.name().lastName();
            String email = firstName.toLowerCase() + "." +
                           lastName.toLowerCase() +
                           "@gmail.com";
            String password = bCryptPasswordEncoder.encode("pass");
            String iban = getRandomIban();

            generatedIbans.add(iban);

            User user = createUser(firstName, lastName, email, password);
            Account account = createAccount(iban);
            user.getAccounts().add(account);
            account.setUser(user);

            Transaction depositTransaction = createInitialDepositTransaction(iban, account.getBalance());
            generatedTransactions.add(depositTransaction);

            fakeUsers.add(user);
        }
        // Generate transactions between accounts
        generateTransactions(generatedIbans);

        return fakeUsers;
    }

    public User createUser(String firstName, String lastName, String email, String password) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(password);
        user.setBsnNumber(faker.number().numberBetween(100000000, 999999999));
        user.setPhoneNumber(faker.phoneNumber().phoneNumber());

        if (faker.number().numberBetween(0, 100) < 80) {
            user.setRole(UserRole.APPROVED);
        } else {
            user.setRole(UserRole.UNAPPROVED);
        }
        return user;
    }

    public Account createAccount(String iban) {
        Account account = new Account();
        account.setIban(iban);
        account.setBalance(BigDecimal.valueOf(faker.number().randomDouble(2, -1000, 10000)));
        account.setDailyLimit(BigDecimal.valueOf(faker.number().randomDouble(2, 1000, 10000)));
        account.setAbsoluteLimit(BigDecimal.valueOf(faker.number().randomDouble(2, 1000, 10000)));
        account.setBalanceLimit(BigDecimal.valueOf(faker.number().randomDouble(2, -1000, 0)));
        account.setStatus(AccountStatus.ACTIVE); // Set all newly created accounts as active

        if (faker.number().numberBetween(0, 100) < 70) {
            account.setAccountType(AccountType.PAYMENT);
        } else {
            account.setAccountType(AccountType.SAVINGS);
        }
        return account;
    }

    public Transaction createInitialDepositTransaction(String iban, BigDecimal amount) {
        Transaction depositTransaction = new Transaction();
        depositTransaction.setFromAccount(iban);
        depositTransaction.setToAccount(iban);
        depositTransaction.setTransferAmount(amount);
        depositTransaction.setTimestamp(LocalDateTime.now().minusDays(faker.number().numberBetween(0, 30)));
        depositTransaction.setReference(String.valueOf(faker.number().numberBetween(100000, 999999)));
        depositTransaction.setDescription("Initial deposit");

        if (faker.number().numberBetween(0, 100) < 90) {
            depositTransaction.setTransactionType(Transaction.TransactionType.DEPOSIT);
        } else {
            // This case seems unlikely for an initial deposit, but keeping original logic
            depositTransaction.setTransactionType(Transaction.TransactionType.TRANSFER);
        }
        return depositTransaction;
    }

    public void generateTransactions(List<String> ibans) {
        for (String fromIban : ibans) {
            String toIban = getRandomIban(ibans, fromIban);
            Transaction transaction = new Transaction();
            transaction.setFromAccount(fromIban);
            transaction.setToAccount(toIban);
            transaction.setTransferAmount(BigDecimal.valueOf(faker.number().randomDouble(2, 1, 100)));
            transaction.setTimestamp(LocalDateTime.now().minusDays(faker.number().numberBetween(0, 30)));
            transaction.setReference("TR" + faker.number().numberBetween(100000, 999999));
            transaction.setTransactionType(Transaction.TransactionType.TRANSFER);
            transaction.setDescription("Random transfer");

            generatedTransactions.add(transaction);
        }
    }

    public List<Transaction> getGeneratedTransactions() {
        return generatedTransactions;
    }

    private String getRandomIban(List<String> ibans, String excludeIban) {
        List<String> availableIbans = new ArrayList<>(ibans);
        availableIbans.remove(excludeIban);
        return availableIbans.get(faker.number().numberBetween(0, availableIbans.size()));
    }

    public String getRandomIban() {
        return new Iban.Builder()
                .countryCode(CountryCode.NL).bankCode("SPYD")
                .buildRandom().toFormattedString();
    }
}

