package com.optibrain.cloud.adapter.aws;

import com.optibrain.cloud.aws.AwsClientFactory;
import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.ResourceType;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.s3.model.Bucket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serverless and data-store coverage: Lambda, DynamoDB and S3.
 *
 * <p>These three carry recurring cost that a compute-and-storage inventory misses
 * entirely, so their absence understates a bill rather than merely narrowing a view.
 *
 * <p>Memory and read/write capacity are surfaced as specifications rather than cost.
 * Lambda is billed per GB-second and DynamoDB per request unit, neither of which a list
 * price captures; a fabricated figure would be worse than an explicit blank.
 */
@Component
public class ServerlessScanner extends AwsResourceScanner {

    public ServerlessScanner(AwsClientFactory clients) {
        super(clients, "serverless:lambda,dynamodb,s3");
    }

    @Override
    protected List<CloudResource> load() {
        List<CloudResource> all = new ArrayList<>();
        all.addAll(lambdaFunctions());
        all.addAll(dynamoTables());
        all.addAll(s3Buckets());
        return all;
    }

    private List<CloudResource> lambdaFunctions() {
        return safe(() -> clients.lambda()
                .listFunctions(software.amazon.awssdk.services.lambda.model
                        .ListFunctionsRequest.builder().build())
                .functions().stream()
                .map(this::toResource)
                .toList());
    }

    private CloudResource toResource(FunctionConfiguration fn) {
        Map<String, String> specs = new LinkedHashMap<>();
        specs.put("runtime", fn.runtime() == null ? "" : fn.runtimeAsString());
        specs.put("handler", String.valueOf(fn.handler()));
        specs.put("architecture", fn.architectures() == null || fn.architectures().isEmpty()
                ? "" : fn.architectures().get(0).toString());
        specs.put("memoryMB", fn.memorySize() == null ? "" : String.valueOf(fn.memorySize()));
        specs.put("timeoutSeconds", fn.timeout() == null ? "" : String.valueOf(fn.timeout()));
        specs.put("codeSizeBytes", fn.codeSize() == null ? "" : String.valueOf(fn.codeSize()));
        specs.put("lastModified", fn.lastModified() == null ? "" : fn.lastModified());
        if (fn.environment() != null && fn.environment().variables() != null) {
            specs.put("environmentVariableCount",
                    String.valueOf(fn.environment().variables().size()));
        }

        // Tags require a separate call per function, which is one API round trip each.
        // Skipped deliberately: the listing call does not carry them, and N calls for an
        // N-function account is a poor trade against an inventory refresh.
        Map<String, String> tags = Map.of();

        return new CloudResource(
                fn.functionName(), ResourceType.LAMBDA_FUNCTION, region(),
                "active", fn.functionName(), tags, specs, null, null, null, false);
    }

    private List<CloudResource> dynamoTables() {
        return safe(() -> clients.dynamoDb().listTables(software.amazon.awssdk.services.dynamodb.model.ListTablesRequest.builder().build()).tableNames().stream()
                .map(name -> describeTable(name))
                .filter(java.util.Objects::nonNull)
                .toList());
    }

    private CloudResource describeTable(String name) {
        TableDescription table;
        try {
            table = clients.dynamoDb().describeTable(software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest.builder().tableName(name).build()).table();
        } catch (Exception e) {
            log.debug("DescribeTable {} unavailable: {}", name, e.getMessage());
            return null;
        }

        Map<String, String> specs = new LinkedHashMap<>();
        specs.put("tableStatus", table.tableStatus() == null ? "" : table.tableStatusAsString());
        specs.put("billingMode", table.billingModeSummary() == null ? "" : table.billingModeSummary().toString());
        specs.put("itemCount", table.itemCount() == null ? "" : String.valueOf(table.itemCount()));
        specs.put("sizeBytes", table.tableSizeBytes() == null ? "" : String.valueOf(table.tableSizeBytes()));
        specs.put("readCapacityUnits", table.provisionedThroughput() == null
                || table.provisionedThroughput().readCapacityUnits() == null
                ? "" : String.valueOf(table.provisionedThroughput().readCapacityUnits()));
        specs.put("writeCapacityUnits", table.provisionedThroughput() == null
                || table.provisionedThroughput().writeCapacityUnits() == null
                ? "" : String.valueOf(table.provisionedThroughput().writeCapacityUnits()));
        specs.put("createdAt", table.creationDateTime() == null ? "" : table.creationDateTime().toString());

        return new CloudResource(
                table.tableName(), ResourceType.DYNAMODB_TABLE, region(),
                table.tableStatus() == null ? "unknown" : table.tableStatusAsString(),
                table.tableName(), Map.of(), specs, null, null,
                table.creationDateTime(), false);
    }

    private List<CloudResource> s3Buckets() {
        return safe(() -> clients.s3().listBuckets(software.amazon.awssdk.services.s3.model.ListBucketsRequest.builder().build()).buckets().stream()
                .map(this::toResource)
                .toList());
    }

    private CloudResource toResource(Bucket bucket) {
        Map<String, String> specs = new LinkedHashMap<>();
        specs.put("createdAt", bucket.creationDate() == null ? "" : bucket.creationDate().toString());

        return new CloudResource(
                bucket.name(), ResourceType.S3_BUCKET, region(),
                "exists", bucket.name(), Map.of(), specs, null, null,
                bucket.creationDate(), false);
    }
}