package com.concer.backend.orders.Entity;

public enum OrderStatus {
    PROCESSING(0, "處理中"),
    SUCCESS(1, "成功"),
    FAILED(2, "失敗"),
    CANCELLED(3, "已取消"),
    LOCK_SUCCESS(4, "鎖定成功"), // 👈 中間狀態
    LOCK_FAILED(5, "鎖定失敗");   // 👈 中間狀態

    private final int code;
    private final String description;

    OrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static OrderStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }

        for (OrderStatus status : OrderStatus.values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown order status code: " + code);
    }
}
