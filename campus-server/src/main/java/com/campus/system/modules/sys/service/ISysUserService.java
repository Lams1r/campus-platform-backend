package com.campus.system.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.system.common.api.PageResult;
import com.campus.system.modules.sys.dto.SysUserCreateDTO;
import com.campus.system.modules.sys.dto.SysUserQueryDTO;
import com.campus.system.modules.sys.dto.SysUserUpdateDTO;
import com.campus.system.modules.sys.entity.SysUser;
import com.campus.system.modules.sys.vo.SysUserVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户管理业务接口
 */
public interface ISysUserService extends IService<SysUser> {

    /** 分页查询用户 */
    PageResult<SysUserVO> queryUserPage(SysUserQueryDTO query);

    /** 获取用户详情 */
    SysUserVO getUserDetail(Long userId);

    /** 新增用户 */
    void createUser(SysUserCreateDTO dto);

    /** 更新用户（不含密码） */
    void updateUser(SysUserUpdateDTO dto);

    /** 删除用户（逻辑删除） */
    void deleteUser(Long userId);

    /** 切换账号状态（启用/停用） */
    void toggleStatus(Long userId, Integer status);

    /** 重置密码 */
    void resetPassword(Long userId, String newPassword);

    /** Excel 导入用户 */
    String importUsers(MultipartFile file);

    /** Excel 导出用户 */
    void exportUsers(SysUserQueryDTO query, HttpServletResponse response);
}
