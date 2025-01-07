package com.youcode.bankify.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TransferRequest {

    private Long fromAccount;
    private String  toAccount;
    private double amount;
    private String transactionType;
    private String frequency;

}
