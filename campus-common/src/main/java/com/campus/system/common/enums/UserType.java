package com.campus.system.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户类型枚举
 */
@Getter
@AllArgsConstructor
public enum UserType {

    STUDENT(0, "学生"),
    TEACHER(1, "教师"),
    ADMIN(2, "管理员");

    private final int code;
    private final String label;

    public static UserType fromCode(int code) {
        for (UserType type : values()) {
            if (type.code == code) return type;
        }
        return STUDENT;
    }
}
