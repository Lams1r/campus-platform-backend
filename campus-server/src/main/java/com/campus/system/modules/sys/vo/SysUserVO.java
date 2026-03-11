package com.campus.system.modules.sys.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息视图对象（脱敏，不含密码）
 */
@Data
public class SysUserVO {

    private Long id;
    private String username;
    private String realName;
    private String avatar;
    private Integer gender;
    private String phone;
    private String email;
    private String deptName;
    private String className;
    private Integer userType;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;

    /** 拥有的角色名称列表 */
    private List<String> roleNames;
}
