package com.optibrain.common.config;

import com.optibrain.auth.repository.AppUserRepository;
import com.optibrain.common.filter.TenantFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    /**
     * The only registration of {@link TenantFilter}. A component annotation would create a
     * second bean and a conflicting bean name; this single registration also pins the
     * order (default, after the Spring Security chain) and the URL scope ({@code /api/*}).
     */
    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(
            AppUserRepository userRepository) {
        FilterRegistrationBean<TenantFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TenantFilter(userRepository));
        registrationBean.addUrlPatterns("/api/*");
        return registrationBean;
    }
}