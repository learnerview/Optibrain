package com.optibrain.auth.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small in-memory user registry for local/dev authentication. Production deployments
 * should replace this with a database- or IdP-backed service; the interface is the seam.
 *
 * <p>{@link #add} stores an immutable snapshot rather than the submitted object, so a
 * successful authentication can never erase the stored password.
 */
@Component
public class UserStore {

    private final Map<String, StoredUser> users = new ConcurrentHashMap<>();
    private final Map<String, String> companies = new ConcurrentHashMap<>();

    public void add(UserDetails user) {
        add(user, null);
    }

    public void add(UserDetails user, String company) {
        users.put(user.getUsername(), new StoredUser(
                user.getUsername(), user.getPassword(), user.getAuthorities()));
        if (company != null) {
            companies.put(user.getUsername(), company);
        }
    }

    public String companyOf(String username) {
        return username == null ? null : companies.get(username);
    }

    public boolean exists(String username) {
        return username != null && users.containsKey(username);
    }

    public UserDetails get(String username) {
        return username == null ? null : users.get(username);
    }

    public UserDetailsService service() {
        return username -> {
            StoredUser u = users.get(username);
            if (u == null) {
                throw new UsernameNotFoundException("Unknown user");
            }
            return u;
        };
    }
}