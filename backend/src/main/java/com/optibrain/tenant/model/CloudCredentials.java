package com.optibrain.tenant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudCredentials {
    private String accessKeyId;
    private String secretAccessKey;
    private String region;
}
