package com.stefvisser.springyield.services;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import com.stefvisser.springyield.dto.*;
import com.stefvisser.springyield.models.*;
import com.stefvisser.springyield.repositories.AccountRepository;
import com.stefvisser.springyield.repositories.TransactionRepository;
import com.stefvisser.springyield.repositories.UserRepository;
import com.stefvisser.springyield.utils.FakeUserLoader;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Primary
@Service
class UserServiceImpl implements UserService {
    private final AccountService accountService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final FakeUserLoader fakeUserLoader;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TransactionRepository transactionRepository;

    public UserServiceImpl(AccountService accountService, UserRepository userRepository,
                           TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.accountService = accountService;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.fakeUserLoader = new FakeUserLoader(this); // Assuming FakeUserLoader constructor takes UserServiceImpl
    }

    @PostConstruct
    @Transactional // Add @Transactional here
    public void initializeDefaultUsers() {
        addDefaultUsers();
        addRandomUsers(250);
    }

    /**
     * Approves a user by changing their role to APPROVED and creating normal and savings accounts for them.
     *
     * @param userId the ID of the user to approve
     * @return UserProfileDto containing the updated user information
     * @ Transactional ensures that the operation is atomic, meaning either all changes are applied or none.
     */
    @Transactional
    public UserProfileDto approveUser(Long userId, BigDecimal dailyLimit, BigDecimal absoluteLimit) {
        User user = userRepository.findByUserId(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (user.getRole() != UserRole.UNAPPROVED) {
            throw new RuntimeException("User is already approved");
        }

        // Change role to APPROVED
        user.setRole(UserRole.APPROVED);
        userRepository.save(user);

        // Create accounts
//        BigDecimal dailyLimit = new BigDecimal("3000.00");
//        BigDecimal absoluteLimit = new BigDecimal("1000.00");
        BigDecimal balanceLimit = new BigDecimal("500.00");
        BigDecimal initialBalance = BigDecimal.ZERO;

        // Create normal account
        Account normalAccount = new Account();
        normalAccount.setUser(user);
        normalAccount.setAccountType(AccountType.PAYMENT);
        normalAccount.setDailyLimit(dailyLimit);
        normalAccount.setAbsoluteLimit(absoluteLimit);
        normalAccount.setBalanceLimit(balanceLimit);
        normalAccount.setBalance(initialBalance);
        normalAccount.setStatus(AccountStatus.ACTIVE);
        normalAccount.setIban(new Iban.Builder()
                .countryCode(CountryCode.NL).bankCode("SPYD")
                .buildRandom().toFormattedString()
        );
        Account savedNormalAccount = accountService.createAccount(normalAccount);

        // Create savings account
        Account savingsAccount = new Account();
        savingsAccount.setUser(user);
        savingsAccount.setAccountType(AccountType.SAVINGS);
        savingsAccount.setDailyLimit(dailyLimit);
        savingsAccount.setAbsoluteLimit(absoluteLimit);
        savingsAccount.setBalanceLimit(balanceLimit);
        savingsAccount.setBalance(initialBalance);
        savingsAccount.setStatus(AccountStatus.ACTIVE);
        savingsAccount.setIban(new Iban.Builder()
                .countryCode(CountryCode.NL).bankCode("SPYD")
                .buildRandom().toFormattedString()
        );
        Account savedSavingsAccount = accountService.createAccount(savingsAccount);

        user.getAccounts().add(savedNormalAccount);
        user.getAccounts().add(savedSavingsAccount);

        return new UserProfileDto(user);
    }

    public UserProfileDto login(UserLoginDto loginDto) {
        User user = userRepository.findByEmail(loginDto.getEmail());
        if (user != null) {
            if (passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
                return new UserProfileDto(user);
            }
        }
        return null;
    }

    public User getUserById(Long userId) {
        return userRepository.findByUserId(userId);
    }

    public User findByEmail(String mail) {
        return userRepository.findByEmail(mail);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public PaginatedDataDto<UserProfileDto> search(String query, UserRole role, int limit, int offset, boolean isAdmin) {
        return userRepository.search(query, role, limit, offset, isAdmin);
    }

    // @Transactional // Consider if this method also needs to be transactional if it involves lazy loading or multiple DB operations
    public void addRandomUsers(int count) {
        List<User> fakeUsers = fakeUserLoader.generateFakeUsers(count);
        userRepository.saveAll(fakeUsers);
        List<Transaction> transactions = fakeUserLoader.getGeneratedTransactions();
        if (transactions != null && !transactions.isEmpty()) {
            transactionRepository.saveAll(transactions);
        }
    }

    // @Transactional // This is now covered by initializeDefaultUsers
    public void addDefaultUsers() {
        List<User> users = new ArrayList<>();
        for (UserRole role : UserRole.values()) {
            User user = new User(
                    role.name().toLowerCase(),
                    role.name().toLowerCase(),
                    passwordEncoder.encode("pass"),
                    role.name().toLowerCase() + "@springyield.com",
                    123456789,
                    "0612345678",
                    role,
                    new ArrayList<>()
            );
            for (int i = 0; i < 3; i++) {
                Account account = fakeUserLoader.createAccount(fakeUserLoader.getRandomIban());
                account.setUser(user);
                user.getAccounts().add(account);
            }
            users.add(user);
        }
        createAtmsUser(); // Create a user for ATMS system
        userRepository.saveAll(users); // Save default role users first
    }

    private void createAtmsUser() {
        User atmsUser = new User(
                "ATMS", // First name
                "System",   // Last name
                passwordEncoder.encode("atmspass"), // Password
                "atms@springyield.com", // Email
                987654321, // BSN
                "0687654321", // Phone number
                UserRole.ADMIN, // Role - Assuming you might have or want a SYSTEM role
                new ArrayList<>() // Initialize accounts list
        );

        Account atmsAccount = new Account();
        atmsAccount.setUser(atmsUser);
        atmsAccount.setIban(new Iban.Builder().countryCode(CountryCode.NL).bankCode("ATMS").buildRandom().toFormattedString());
        atmsAccount.setAccountType(AccountType.PAYMENT);

        atmsAccount.setBalance(new BigDecimal("999999999.99"));
        // High limits appropriate for such a balance
        atmsAccount.setDailyLimit(new BigDecimal("10000000.00"));
        atmsAccount.setAbsoluteLimit(new BigDecimal("5000000.00"));
        // A very low (negative) balance limit, effectively no overdraft concern for this amount
        atmsAccount.setBalanceLimit(new BigDecimal("-1000000.00"));
        atmsAccount.setStatus(AccountStatus.ACTIVE);

        atmsUser.getAccounts().add(atmsAccount);
        userRepository.save(atmsUser); // Save user, which cascades to account
    }

    @Override
    @Transactional
    public void deleteUser(User executingUser, Long targetUserId) {
        // Get the authenticated user (the one performing the deletion)
        if (executingUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        // Get the target user (the one being deleted)
        User targetUser = this.getUserById(targetUserId);
        if (targetUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        if (!executingUser.getUserId().equals(targetUserId) && !executingUser.isEmployee()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this user");
        }

        if (executingUser.getUserId().equals(targetUserId) && executingUser.isEmployee()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Employees cannot delete their own accounts");
        }

        // Detach all accounts from the target user before deletion and mark them as inactive
        if (targetUser.getAccounts() != null && !targetUser.getAccounts().isEmpty()) {
            for (Account account : targetUser.getAccounts()) {
                account.setUser(null);
                account.setStatus(AccountStatus.DEACTIVATED); // Mark account as inactive when user is deleted
            }

            // Save the updated accounts, and remove them from the user's list
            accountRepository.saveAll(targetUser.getAccounts());
            targetUser.getAccounts().clear();
        }

        // Now delete the user
        userRepository.delete(targetUser);
    }

    @Override
    @Transactional
    public UserProfileDto updateUser(Long userId, UserUpdateDto userUpdateDto) {
        User user = userRepository.findByUserId(userId);

        if (user == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + userId);

        // Check for email uniqueness if email is being changed
        if (userUpdateDto.getEmail() != null && !userUpdateDto.getEmail().equals(user.getEmail())) {
            User existingUser = userRepository.findByEmail(userUpdateDto.getEmail());
            if (existingUser != null && !existingUser.getUserId().equals(userId))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        // Prevents empty fields from overwriting existing values
        if (userUpdateDto.getFirstName() != null) user.setFirstName(userUpdateDto.getFirstName());
        if (userUpdateDto.getLastName() != null) user.setLastName(userUpdateDto.getLastName());
        if (userUpdateDto.getEmail() != null) user.setEmail(userUpdateDto.getEmail());
        if (userUpdateDto.getBsnNumber() != null) user.setBsnNumber(userUpdateDto.getBsnNumber());
        if (userUpdateDto.getPhoneNumber() != null) user.setPhoneNumber(userUpdateDto.getPhoneNumber());
        if (userUpdateDto.getRole() != null) user.setRole(userUpdateDto.getRole());
        if (userUpdateDto.getPassword() != null) user.setPassword(passwordEncoder.encode(userUpdateDto.getPassword().trim()));

        return new UserProfileDto(userRepository.save(user));
    }

    @Override
    public BCryptPasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }
}
