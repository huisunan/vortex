package com.vortex.run.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartRunRequest {
    private String flowCode;
    private List<String> businessIds;
}
