package com.optibrain.auth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A login must be repeatable. Spring's {@link User} implements
 * {@code CredentialsContainer} and its {@code eraseCredentials()} clears the mutable
 * password field; if the very same object is stored as the canonical credential for a
 * user, the successful authentication of that object erases the password and every later
 * login fails with "Empty encoded password". This pins UserStore's promise that stored
 * credentials survive authentication.
 */
class UserStoreLoginTest {

    @Test
    @DisplayName("the stored credentials survive a successful authentication")
    void storedCredentialsSurviveSuccessfulAuthentication() {
        UserStore store = new UserStore();
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        UserDetails user = User.builder()
                .username("alice")
                .password(encoder.encode("secret"))
                .roles("USER")
                .build();
        store.add(user, "Acme");

        AuthenticationManager manager = providerManager(store, encoder);

        // The first authentication passes the stored object itself into the chain as the
        // principal; with a mutable CredentialsContainer it would be erased here.
        manager.authenticate(new UsernamePasswordAuthenticationToken("alice", "secret"));

        assertThat(store.get("alice").getPassword())
                .as("password must still be present for the next login")
                .isNotNull()
                .isNotBlank();

        // A second, independent login must succeed as well.
        org.springframework.security.core.Authentication second =
                manager.authenticate(new UsernamePasswordAuthenticationToken("alice", "secret"));
        assertThat(second.isAuthenticated()).isTrue();
    }

    private ProviderManager providerManager(UserStore store, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(store.service());
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
}