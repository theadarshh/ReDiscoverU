package com.rediscoveru.dto;
import lombok.Data;
@Data
public class SetPasswordRequest {
    private String email;
    private String password;
}
