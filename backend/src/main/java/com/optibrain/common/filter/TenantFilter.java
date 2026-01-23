package com.optibrain.common.filter;

import com.optibrain.common.context.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

public class TenantFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        String tenantId = httpRequest.getHeader("X-TENANT");
        String userId = httpRequest.getHeader("X-USER");

        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
        }
        if (userId != null) {
            TenantContext.setUserId(userId);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
