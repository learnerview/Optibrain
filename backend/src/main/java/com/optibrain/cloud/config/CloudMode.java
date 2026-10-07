package com.optibrain.cloud.config;

/**
 * Execution targets. There are exactly two, and they differ only in endpoint and
 * credentials - never in code.
 *
 * <p>Both are served by the same {@code AwsCloudProviderAdapter}, making real AWS SDK
 * calls. {@link #SANDBOX} points those calls at a LocalStack container; {@link #AWS}
 * points them at Amazon. Every service, controller and analysis rule above the port is
 * shared, so the sandbox exercises production code rather than a parallel fake.
 *
 * <ul>
 *   <li>{@link #SANDBOX} - LocalStack. No AWS account, no AWS cost. Requires Docker.</li>
 *   <li>{@link #AWS} - real AWS, via the instance IAM role, environment credentials or
 *       {@code ~/.aws/credentials}.</li>
 * </ul>
 *
 * <p>There is deliberately no "mock" mode. A mode that short-circuits the SDK produces
 * a demo that is guaranteed to diverge from production, which is precisely how this
 * project came to believe it was sandboxed while calling real AWS.
 */
public enum CloudMode {

    /** LocalStack-backed sandbox. Safe: nothing outside the container is affected. */
    SANDBOX,

    /** Real AWS. */
    AWS;

    public static CloudMode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return SANDBOX;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown cloud.mode '" + raw + "'. Supported values: SANDBOX, AWS. "
                            + "There is no 'mock' mode by design - use SANDBOX for local work.", e);
        }
    }

    /** True when actions cannot reach real infrastructure. */
    public boolean isSandboxed() {
        return this == SANDBOX;
    }
}