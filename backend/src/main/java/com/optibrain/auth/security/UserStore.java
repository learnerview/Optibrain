package com.optibrain.auth.security;

import com.optibrain.auth.model.AppUser;
import com.optibrain.auth.repository.AppUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * User registry backed by {@code app_users}.
 *
 * <p>Users are persisted so accounts survive a restart, registration is durable, and a
 * multi-instance deployment shares one identity store. {@link #add} stores an immutable
 * snapshot rather than the submitted object, so a successful authentication can never
 * erase the stored password.
 *
 * <p>The package-local no-argument constructor exists only for the unit test in this
 * package, which drives the registry without Spring or a database; all production wiring
 * goes through the repository-backed constructor.
 */
@Component
@Slf4j
public class UserStore implements Serializable {

    public static final String DEFAULT_TENANT = "default-tenant";

    private static final long serialVersionUID = 1L;

    private final AppUserRepository users;

    /** Package-local fallback store used by the no-database unit test. */
    private final Map<String, StoredUser> memory = new ConcurrentHashMap<>();

    public UserStore(AppUserRepository users) {
        this.users = users;
    }

    UserStore() {
        this.users = null;
    }

    public void add(UserDetails user) {
        add(user, null, DEFAULT_TENANT);
    }

    public void add(UserDetails user, String company) {
        add(user, company, DEFAULT_TENANT);
    }

    public void add(UserDetails user, String company, String tenantId) {
        if (exists(user.getUsername())) {
            return;
        }
        String resolvedTenant = tenantId == null || tenantId.isBlank() ? DEFAULT_TENANT : tenantId;
        String authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        if (users != null) {
            users.save(AppUser.builder()
                    .username(user.getUsername())
                    .passwordHash(user.getPassword())
                    .authoritiesCsv(authorities)
                    .tenantId(resolvedTenant)
                    .company(company)
                    .enabled(true)
                    .build());
        } else {
            memory.put(user.getUsername(), snapshot(user, resolvedTenant));
        }
    }

    public String companyOf(String username) {
        if (users != null) {
            return users.findByUsername(username).map(AppUser::getCompany).orElse(null);
        }
        return null;
    }

    public boolean exists(String username) {
        if (username == null) {
            return false;
        }
        if (users != null) {
            return users.existsByUsername(username);
        }
        return memory.containsKey(username);
    }

    public UserDetails get(String username) {
        if (username == null || !exists(username)) {
            return null;
        }
        return resolveSilently(username);
    }

    public UserDetailsService service() {
        return this::resolveSilently;
    }

    private UserDetails resolveSilently(String username) {
        if (username == null) {
            return null;
        }
        if (users != null) {
            AppUser entity = users.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
            if (!entity.isEnabled()) {
                throw new UsernameNotFoundException("Unknown user");
            }
            return new StoredUser(entity.getUsername(), entity.getPasswordHash(),
                    entity.getTenantId(), entity.isEnabled(), authorities(entity.getRoles()));
        }
        StoredUser user = memory.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("Unknown user");
        }
        return user;
    }

    private static StoredUser snapshot(UserDetails user, String tenantId) {
        return new StoredUser(user.getUsername(), user.getPassword(), tenantId, true,
                user.getAuthorities());
    }

    private static Collection<? extends GrantedAuthority> authorities(Collection<String> roles) {
        return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }
}