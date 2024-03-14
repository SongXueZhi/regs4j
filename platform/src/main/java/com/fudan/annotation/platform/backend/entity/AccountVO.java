package com.fudan.annotation.platform.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * description: login msg
 *
 * @author sunyujie
 * create: 2021-12-13 19:17
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountVO {
    private int accountId;
    private String accountName;
    private String email;
    private String avatar;
    private String role;
    private String token;
}