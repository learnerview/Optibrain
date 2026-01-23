package com.optibrain.cloud.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DiscoveryResult {
    private String provider;
    private List<String> instances;
}
