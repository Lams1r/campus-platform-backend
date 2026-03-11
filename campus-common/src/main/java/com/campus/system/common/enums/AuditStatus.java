package com.campus.system.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批/流转状态枚举（通用）
 */
@Getter
@AllArgsConstructor
public enum AuditStatus {

    PENDING(0, "待审核"),
    REJECTED(1, "已驳回"),
    APPROVED(2, "已归档/已通过");

    private final int code;
    private final String label;

    public static AuditStatus fromCode(int code) {
        for (AuditStatus s : values()) {
            if (s.code == code) return s;
        }
        return PENDING;
    }
}
