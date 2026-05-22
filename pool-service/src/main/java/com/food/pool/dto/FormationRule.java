package com.food.pool.dto;

import lombok.Data;

@Data
public class FormationRule {
    private Integer minMembers;
    private Long minAmount;
    private Integer deadlineMinutes;
}
