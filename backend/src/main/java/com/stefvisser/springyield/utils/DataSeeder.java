package com.stefvisser.springyield.utils;

import com.github.javafaker.Faker;
import com.stefvisser.springyield.models.*;
import com.stefvisser.springyield.repositories.AccountRepository;
import com.stefvisser.springyield.repositories.TransactionRepository;
import com.stefvisser.springyield.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DataSeeder {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final Faker faker;
    private final List<Transaction> generatedTransactions = new ArrayList<>();

    public DataSeeder(UserRepository userRepository, AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;

        this.faker = new Faker(new Locale("nl", "NL"));
        this.bCryptPasswordEncoder = new BCryptPasswordEncoder();
    }

    @PostConstruct
    /*
     * Initializes default seeder data.
     * Called by the application startup process to populate the database
     * With data such as default users, accounts, and transactions.
     *
     * The @PostConstruct annotation (Jakarta EE) specification
     * is used to signal a method that should be called after dependency injection
     * is complete but before the bean is put into service.
     * So this method will be executed automatically
     */
    public void initializeDefaultSeederData() {
        this.addDefaultUsers();
        this.addRandomUsers(250);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Default IoC Methods
    // -----------------------------------------------------------------------------------------------------------------

    // @Transactional // This is now covered by initializeDefaultUsers
    public void addDefaultUsers() {
        List<User> users = new ArrayList<>();
        for (UserRole role : UserRole.values()) {
            User user = new User(
                    role.name().toLowerCase(),
                    role.name().toLowerCase(),
                    bCryptPasswordEncoder.encode("pass"),
                    role.name().toLowerCase() + "@springyield.com",
                    123456789,
                    "0612345678",
                    role,
                    new ArrayList<>()
            );
            for (int i = 0; i < 3; i++) {
                Account account = this.createAccount(this.getRandomIban());
                account.setUser(user);
                user.getAccounts().add(account);
            }
            users.add(user);
        }
        createAtmsUser(); // Create a user for ATMS system
        userRepository.saveAll(users); // Save default role users first
    }

    // @Transactional // Consider if this method also needs to be transactional if it involves lazy loading or multiple DB operations
    private void addRandomUsers(int count) {
        List<User> fakeUsers = this.generateFakeUsers(count);
        userRepository.saveAll(fakeUsers);
        List<Transaction> transactions = generatedTransactions;
        if (transactions != null && !transactions.isEmpty()) {
            transactionRepository.saveAll(transactions);
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    private List<User> generateFakeUsers(int count) {
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

    private User createUser(String firstName, String lastName, String email, String password) {
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

    private Account createAccount(String iban) {
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

    private Transaction createInitialDepositTransaction(String iban, BigDecimal amount) {
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

    private void createAtmsUser() {
        User atmsUser = new User(
                "ATMS",
                "System",
                bCryptPasswordEncoder.encode("atmspass"),
                "atms@springyield.com",
                987654321,
                "0687654321",
                UserRole.ADMIN,
                new ArrayList<>() // Initialize accounts list
        );

        // Create ATMS account using constructor directly
        Account atmsAccount = new Account(null, atmsUser,
                new Iban.Builder().countryCode(CountryCode.NL).bankCode("ATMS").buildRandom().toFormattedString(),
                java.time.LocalDate.now(), AccountType.PAYMENT,
                new BigDecimal("10000000.00"), new BigDecimal("5000000.00"), new BigDecimal("999999999.99"),
                new BigDecimal("-1000000.00"), AccountStatus.ACTIVE, new ArrayList<>());

        atmsUser.getAccounts().add(atmsAccount);
        userRepository.save(atmsUser); // Save user, which cascades to account
    }
}

