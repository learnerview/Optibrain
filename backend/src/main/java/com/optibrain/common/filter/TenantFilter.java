package com.optibrain.common.filter;

import com.optibrain.auth.repository.AppUserRepository;
import com.optibrain.auth.security.StoredUser;
import com.optibrain.common.context.TenantContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Derives the {@link TenantContext} from the authenticated principal, never from headers.
 *
 * <p>The tenant a request runs under is the membership stored on the authenticated user,
 * so a caller cannot move between tenants by sending {@code X-TENANT}. The legacy
 * {@code X-TENANT} / {@code X-USER} headers are tolerated only when they exactly match
 * the principal, and a mismatch is rejected with 403; they are never used to select a
 * tenant themselves.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantFilter implements Filter {

    private final AppUserRepository userRepository;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && auth.getPrincipal() instanceof UserDetails
                    && !"anonymousUser".equals(auth.getName())) {
                String username = auth.getName();
                String tenantId = null;
                if (auth.getPrincipal() instanceof StoredUser stored) {
                    tenantId = stored.tenantId();
                }
                if (tenantId == null) {
                    tenantId = userRepository.findByUsername(username)
                            .map(com.optibrain.auth.model.AppUser::getTenantId)
                            .orElse(null);
                }
                if (tenantId == null) {
                    reject(response, "The authenticated user has no tenant membership");
                    return;
                }
                TenantContext.setUserId(username);
                TenantContext.setTenantId(tenantId);

                String headerTenant = request.getHeader("X-TENANT");
                if (headerTenant != null && !headerTenant.isBlank()
                        && !tenantId.equals(headerTenant)) {
                    reject(response, "X-TENANT does not match the authenticated user's tenant");
                    return;
                }
                String headerUser = request.getHeader("X-USER");
                if (headerUser != null && !headerUser.isBlank()
                        && !username.equals(headerUser)) {
                    reject(response, "X-USER does not match the authenticated user");
                    return;
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"success\":false,\"message\":\""
                + message
                + "\",\"data\":null}";
        response.getWriter().write(body);
    }
}