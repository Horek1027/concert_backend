package com.concer.backend.kafka.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// UserSyncRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSyncRequest {
    private String action; // "INIT" 或 "UPDATE"
    private String nickname;
    private Integer status;
}
