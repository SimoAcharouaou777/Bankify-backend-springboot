package com.youcode.bankify.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter
@Setter
public class TransactionResponse {
    private BigDecimal amount;
    private String type;
    private String otherPartyUsername;
    private LocalDateTime date;
    private String status;



}
