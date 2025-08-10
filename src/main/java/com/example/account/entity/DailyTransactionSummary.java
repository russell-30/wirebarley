package com.example.account.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "daily_transaction_summaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DailyTransactionSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private Instant date;

    @Column(nullable = false)
    private BigDecimal totalWithdraw;

    @Column(nullable = false)
    private BigDecimal totalTransfer;

    private Instant updatedAt;

    @Version
    private Long version;

    public void addWithdraw(BigDecimal amount) {
        this.totalWithdraw = this.totalWithdraw.add(amount);
        this.updatedAt = Instant.now();
    }

    public void addTransfer(BigDecimal amount) {
        this.totalTransfer = this.totalTransfer.add(amount);
        this.updatedAt = Instant.now();
    }
}