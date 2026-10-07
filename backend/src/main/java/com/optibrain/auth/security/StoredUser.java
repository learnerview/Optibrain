package com.optibrain.auth.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Immutable snapshot of a user's credentials.
 *
 * <p>Unlike Spring Security's {@code User}, this deliberately does not implement
 * {@code CredentialsContainer}: no part of the authentication chain may erase its
 * password, so a successful login can never invalidate the next one (see
 * {@code UserStoreLoginTest}).
 *
 * <p>Carries the tenant a user belongs to so {@code TenantFilter} can derive the
 * {@code TenantContext} from the authenticated principal instead of trusting a header.
 */
public record StoredUser(String username, String password, String tenantId, boolean enabled,
                  Collection<? extends GrantedAuthority> authorities)
        implements UserDetails {

    public StoredUser {
        authorities = List.copyOf(authorities);
    }

    static StoredUser of(String username, String password, String tenantId, boolean enabled,
                         Collection<? extends GrantedAuthority> authorities) {
        return new StoredUser(username, password, tenantId, enabled, authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}