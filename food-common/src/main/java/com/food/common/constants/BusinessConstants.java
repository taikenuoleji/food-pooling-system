package com.food.common.constants;

/**
 * 业务常量
 */
public final class BusinessConstants {

    private BusinessConstants() {}

    // ========== 错误码 ==========
    public static final int CODE_SUCCESS = 0;
    public static final int CODE_PARAM_ERROR = 4001;
    public static final int CODE_UNAUTHORIZED = 4003;
    public static final int CODE_NOT_FOUND = 4004;
    public static final int CODE_POOL_CONFLICT = 4009;
    public static final int CODE_SERVER_ERROR = 5000;
    public static final int CODE_DOWNSTREAM_FAIL = 5001;
    public static final int CODE_PAYMENT_FAIL = 5003;

    // ========== 拼单状态 ==========
    public static final String POOL_STATUS_CREATED = "CREATED";
    public static final String POOL_STATUS_FORMING = "FORMING";
    public static final String POOL_STATUS_FORMED = "FORMED";
    public static final String POOL_STATUS_ORDERED = "ORDERED";
    public static final String POOL_STATUS_DISSOLVED = "DISSOLVED";

    // ========== 订单状态 ==========
    public static final String ORDER_STATUS_PENDING = "PENDING_CONFIRM";
    public static final String ORDER_STATUS_CONFIRMED = "CONFIRMED";
    public static final String ORDER_STATUS_DELIVERING = "DELIVERING";
    public static final String ORDER_STATUS_COMPLETED = "COMPLETED";
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED";

    // ========== 支付状态 ==========
    public static final String PAY_STATUS_PENDING = "PENDING";
    public static final String PAY_STATUS_SUCCESS = "SUCCESS";
    public static final String PAY_STATUS_FAILED = "FAILED";

    // ========== 参与者状态 ==========
    public static final String PARTICIPANT_ACTIVE = "ACTIVE";
    public static final String PARTICIPANT_LEFT = "LEFT";
    public static final String PARTICIPANT_KICKED = "KICKED";

    // ========== ID前缀 ==========
    public static final String ID_PREFIX_USER = "U";
    public static final String ID_PREFIX_POOL = "P";
    public static final String ID_PREFIX_ORDER = "ORD";
    public static final String ID_PREFIX_PARTICIPANT = "PT";
    public static final String ID_PREFIX_PAYMENT_BATCH = "PB";
    public static final String ID_PREFIX_PAYMENT_RECORD = "PR";
    public static final String ID_PREFIX_ADDRESS = "ADDR";
}
