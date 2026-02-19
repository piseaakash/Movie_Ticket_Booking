package com.xyz.entertainment.ticketing.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xyz.entertainment.ticketing.user.domain.UserAccount;
import com.xyz.entertainment.ticketing.user.dto.LoginRequest;
import com.xyz.entertainment.ticketing.user.dto.RegisterUserRequest;
import com.xyz.entertainment.ticketing.user.dto.UserResponse;
import com.xyz.entertainment.ticketing.user.repository.UserAccountRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Plain unit tests for {@link UserAccountService} without Mockito or Spring.
 * We use simple in-memory fakes to avoid any environment issues with agents.
 */
class UserAccountServicePlainTest {

    private InMemoryUserAccountRepository repository;
    private PasswordEncoder passwordEncoder;
    private UserAccountService service;

    @BeforeEach
    void setUp() {
        this.repository = new InMemoryUserAccountRepository();
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.service = new UserAccountService(repository, passwordEncoder);
    }

    @Test
    void register_creates_new_user_when_email_not_taken() {
        RegisterUserRequest request = RegisterUserRequest.builder()
                .email("Alice@example.com")
                .fullName(" Alice Example ")
                .password("password123")
                .build();

        UserResponse response = service.register(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getFullName()).isEqualTo("Alice Example");

        UserAccount stored = repository.findByEmailIgnoreCase("alice@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("password123", stored.getPasswordHash())).isTrue();
    }

    @Test
    void register_throws_when_email_already_exists() {
        RegisterUserRequest request = RegisterUserRequest.builder()
                .email("bob@example.com")
                .fullName("Bob")
                .password("secret123")
                .build();

        service.register(request);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void login_returns_user_when_credentials_are_valid() {
        RegisterUserRequest register = RegisterUserRequest.builder()
                .email("carol@example.com")
                .fullName("Carol Example")
                .password("topsecret")
                .build();
        UserResponse registered = service.register(register);

        LoginRequest login = LoginRequest.builder()
                .email("carol@example.com")
                .password("topsecret")
                .build();

        UserResponse loggedIn = service.login(login);

        assertThat(loggedIn.getId()).isEqualTo(registered.getId());
        assertThat(loggedIn.getEmail()).isEqualTo("carol@example.com");
        assertThat(loggedIn.getFullName()).isEqualTo("Carol Example");
    }

    @Test
    void login_throws_when_user_not_found() {
        LoginRequest login = LoginRequest.builder()
                .email("missing@example.com")
                .password("irrelevant")
                .build();

        assertThatThrownBy(() -> service.login(login))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_throws_when_password_does_not_match() {
        RegisterUserRequest register = RegisterUserRequest.builder()
                .email("dave@example.com")
                .fullName("Dave Example")
                .password("correct-password")
                .build();
        service.register(register);

        LoginRequest loginWrong = LoginRequest.builder()
                .email("dave@example.com")
                .password("wrong-password")
                .build();

        assertThatThrownBy(() -> service.login(loginWrong))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    /**
     * Very small in-memory fake for {@link UserAccountRepository}.
     * It is deterministic and sufficient for exercising service logic.
     */
    private static class InMemoryUserAccountRepository implements UserAccountRepository {

        private final Map<Long, UserAccount> byId = new HashMap<>();
        private final Map<String, Long> idByEmail = new HashMap<>();
        private long nextId = 1L;

        @Override
        public Optional<UserAccount> findByEmailIgnoreCase(String email) {
            Long id = idByEmail.get(email.toLowerCase());
            if (id == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public boolean existsByEmailIgnoreCase(String email) {
            return idByEmail.containsKey(email.toLowerCase());
        }

        @Override
        public <S extends UserAccount> S save(S entity) {
            Long id = entity.getId();
            if (id == null) {
                id = nextId++;
            }
            UserAccount stored = UserAccount.builder()
                    .id(id)
                    .email(entity.getEmail())
                    .fullName(entity.getFullName())
                    .passwordHash(entity.getPasswordHash())
                    .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now())
                    .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt() : Instant.now())
                    .build();
            byId.put(id, stored);
            idByEmail.put(stored.getEmail().toLowerCase(), id);
            @SuppressWarnings("unchecked")
            S cast = (S) stored;
            return cast;
        }

        // The remaining JpaRepository methods are not needed in these tests and are left unimplemented.
        @Override public java.util.List<UserAccount> findAll() { throw new UnsupportedOperationException(); }
        @Override public java.util.List<UserAccount> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public long count() { return byId.size(); }
        @Override public void deleteById(Long aLong) { throw new UnsupportedOperationException(); }
        @Override public void delete(UserAccount entity) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllById(Iterable<? extends Long> longs) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends UserAccount> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { throw new UnsupportedOperationException(); }
        @Override public <S extends UserAccount> java.util.List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public Optional<UserAccount> findById(Long aLong) { return Optional.ofNullable(byId.get(aLong)); }
        @Override public boolean existsById(Long aLong) { return byId.containsKey(aLong); }
        @Override public void flush() { }
        @Override public <S extends UserAccount> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends UserAccount> java.util.List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<UserAccount> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch() { throw new UnsupportedOperationException(); }
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public UserAccount getOne(Long aLong) { throw new UnsupportedOperationException(); }
        @Override public UserAccount getById(Long aLong) { return byId.get(aLong); }
        @Override public UserAccount getReferenceById(Long aLong) { throw new UnsupportedOperationException(); }
        @Override public java.util.List<UserAccount> findAll(org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public org.springframework.data.domain.Page<UserAccount> findAll(org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends UserAccount> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends UserAccount> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends UserAccount> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends UserAccount> long count(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends UserAccount> boolean exists(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends UserAccount> java.util.Optional<S> findOne(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends UserAccount, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }
}

