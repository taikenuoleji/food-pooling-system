package com.food.common.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务ID生成器
 * 格式: 前缀 + 日期(8位) + 随机数(3位) + 序列号(4位)
 * 示例: U202605220010001, P202605220010001, ORD202605220010001
 */
public final class IdGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger COUNTER = new AtomicInteger(1);
    private static final int RANDOM_PART = ThreadLocalRandom.current().nextInt(100, 999);

    private IdGenerator() {}

    public static String generate(String prefix) {
        String date = LocalDate.now().format(DATE_FMT);
        int seq = COUNTER.getAndIncrement();
        if (seq > 9999) {
            COUNTER.set(1);
            seq = COUNTER.getAndIncrement();
        }
        return prefix + date + RANDOM_PART + String.format("%04d", seq);
    }
}
