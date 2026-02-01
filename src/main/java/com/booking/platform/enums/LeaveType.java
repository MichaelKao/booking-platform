package com.booking.platform.enums;

/**
 * 請假類型
 *
 * @author Developer
 * @since 1.0.0
 */
public enum LeaveType {

    /**
     * 事假
     */
    PERSONAL("事假"),

    /**
     * 病假
     */
    SICK("病假"),

    /**
     * 休假
     */
    VACATION("休假"),

    /**
     * 特休
     */
    ANNUAL("特休"),

    /**
     * 其他
     */
    OTHER("其他");

    private final String description;

    LeaveType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
