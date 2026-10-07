package com.optibrain.cloud.adapter.aws;

import com.optibrain.cloud.aws.AwsClientFactory;
import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.port.ResourceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Shared behaviour for AWS resource scanners.
 *
 * <p>Centralises the fault isolation so each scanner body stays a straight mapping from
 * an SDK model to a {@link CloudResource}, without repeating try/catch and logging.
 */
public abstract class AwsResourceScanner implements ResourceScanner {

    /**
     * Explicit, because Lombok's generated {@code log} is private and subclasses need it.
     */
    protected static final Logger log = LoggerFactory.getLogger(AwsResourceScanner.class);

    protected final AwsClientFactory clients;
    private final String scannerName;

    protected AwsResourceScanner(AwsClientFactory clients, String scannerName) {
        this.clients = clients;
        this.scannerName = scannerName;
    }

    @Override
    public String name() {
        return scannerName;
    }

    @Override
    public final List<CloudResource> scan(com.optibrain.cloud.port.ResourceQuery query) {
        try {
            List<CloudResource> found = load();
            return found == null ? Collections.emptyList() : found;
        } catch (Exception e) {
            // One unavailable service omits its section rather than failing the inventory.
            log.warn("Resource scan '{}' unavailable, omitting: {}", scannerName, e.getMessage());
            return Collections.emptyList();
        }
    }

    /** Performs the API calls. Implementations may assume exceptions are handled. */
    protected abstract List<CloudResource> load();

    protected String region() {
        return clients.region();
    }

    /** Runs a supplier, returning an empty list if it fails. */
    protected <T> List<T> safe(Supplier<List<T>> work) {
        try {
            List<T> result = work.get();
            return result == null ? Collections.emptyList() : result;
        } catch (Exception e) {
            log.warn("Scanner '{}' sub-operation failed: {}", scannerName, e.getMessage());
            return Collections.emptyList();
        }
    }

/** Flattens an SDK tag list into a map, skipping null keys. */
    protected static Map<String, String> tagsOf(List<software.amazon.awssdk.services.ec2.model.Tag> tags) {
        Map<String, String> result = new HashMap<>();
        if (tags != null) {
            tags.forEach(t -> {
                if (t.key() != null) {
                    result.put(t.key(), t.value() == null ? "" : t.value());
                }
            });
        }
        return result;
    }

    /**
     * Records a spec, omitting absent values.
     *
     * <p>{@code String.valueOf(null)} yields the literal string {@code "null"}, which then
     * reaches the API response and the UI as though it were a measured value. An absent
     * spec and a spec whose value is the word "null" are different things, and only the
     * former is true here, so the key is left out entirely.
     *
     * @param specs the resource's spec map, mutated in place
     * @param key spec name
     * @param value spec value, may be null
     */
    protected static void spec(Map<String, String> specs, String key, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value);
        if (!text.isBlank()) {
            specs.put(key, text);
        }
    }
}