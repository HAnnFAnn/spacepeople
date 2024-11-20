package com.spacepeople.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClientExecutionResult {
    private String clientName;
    private long executionTime;
    private SpacePeopleResponse response;
}
