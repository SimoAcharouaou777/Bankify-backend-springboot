package com.youcode.bankify.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
public class ScheduledTransferResponse {
    private Long id;
    private Long fromAccountId;
    private String fromAccountNumber;
    private Long toAccountId;
    private String toAccountNumber;
    private BigDecimal amount;
    private String frequency;
    private OffsetDateTime nextExecutionDate;
    private OffsetDateTime endDate;
}
