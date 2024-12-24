package com.youcode.bankify.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ErrorResponse {
    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    private int status;

    public ErrorResponse(){}

    public ErrorResponse(String message){
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.status = 400;
    }
}
