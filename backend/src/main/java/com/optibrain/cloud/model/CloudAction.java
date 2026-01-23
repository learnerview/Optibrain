package com.optibrain.cloud.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudAction {
    private String actionId;
    private String resourceId;
    private String actionType; // SCALE_UP, SCALE_DOWN
    private String status;
    
    // Alias method for backward compatibility
    public String getType() {
        return actionType;
    }
}
