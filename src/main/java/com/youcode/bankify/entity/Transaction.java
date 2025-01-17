package com.youcode.bankify.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.youcode.bankify.util.serializer.LocalDateTimeDeserializer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@Document(indexName = "transactions")
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "Transaction")
public class Transaction {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column( name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "type" , nullable = false)
    private String type;

    @Column(name = "date")
    private OffsetDateTime date;

    @Column(name = "status" , nullable = true)
    private String status;

    @ManyToOne
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
