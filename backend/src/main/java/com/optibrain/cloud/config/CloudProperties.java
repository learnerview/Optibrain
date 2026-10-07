package com.optibrain.cloud.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Single source of truth for {@code cloud.*}.
 *
 * <p>This replaces the previous {@code CloudConfig}, which bound the same property
 * ({@code cloud.mode}) in two different places and compared it against raw strings in
 * six classes. Spring relaxed binding is used here, so {@code CLOUD_MODE=AWS} and
 * {@code cloud.mode=AWS} are equivalent.
 *
 * <p>{@code @Component} is required in addition to {@code @ConfigurationProperties}:
 * without it the class is never registered and every injection point fails at startup.
 */
@Component
@Validated
@ConfigurationProperties(prefix = "cloud")
public class CloudProperties {

    /** Execution target. Bound to the enum so a typo fails fast at startup. */
    @NotNull
    private CloudMode mode = CloudMode.SANDBOX;

    /**
     * Safety interlock for mutating operations. Defaults to true so an
     * unconfigured deployment can never terminate anything.
     */
    private boolean dryRun = true;

    private final Aws aws = new Aws();

    private final Sandbox sandbox = new Sandbox();

    public CloudMode getMode() {
        return mode;
    }

    public void setMode(CloudMode mode) {
        this.mode = mode == null ? CloudMode.SANDBOX : mode;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public Aws getAws() {
        return aws;
    }

    public Sandbox getSandbox() {
        return sandbox;
    }

    public static class Aws {

        @NotBlank
        private String region = "us-east-1";

        /**
         * Cost Explorer is a global service that only accepts us-east-1. Kept as an
         * explicit, named source rather than hardcoded at each call site.
         */
        private String costExplorerRegion = "us-east-1";

        /**
         * Whether a tenant without an IAM role may use the ambient credential chain when
         * {@code cloud.mode=AWS}. Defaults to false so a misconfigured tenant can never
         * silently act on the OptiBrain instance's own account. Set to true only for a
         * single-account deployment where the application is the only identity involved.
         */
        private boolean allowAmbientFallback = false;

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getCostExplorerRegion() {
            return costExplorerRegion;
        }

        public void setCostExplorerRegion(String costExplorerRegion) {
            this.costExplorerRegion = costExplorerRegion;
        }

        public boolean isAllowAmbientFallback() {
            return allowAmbientFallback;
        }

        public void setAllowAmbientFallback(boolean allowAmbientFallback) {
            this.allowAmbientFallback = allowAmbientFallback;
        }
    }

    /**
     * Connection details for the LocalStack sandbox.
     *
     * <p>Only read when {@link #mode} is {@code SANDBOX}. It replaces the loose
     * {@code cloud.localstack.*} keys that were previously read with ad-hoc
     * {@code @Value} lookups and could drift from the rest of the configuration.
     */
    public static class Sandbox {

        private String endpoint = "http://localhost:4566";

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint == null || endpoint.isBlank()
                    ? "http://localhost:4566" : endpoint;
        }
    }
}