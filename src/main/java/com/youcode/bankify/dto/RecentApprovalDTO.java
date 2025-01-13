package com.youcode.bankify.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RecentApprovalDTO {
    private Long id;
    private String type;
    private String description;
    private LocalDateTime date;
}
