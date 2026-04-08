# Swagger 中文化与 API 乱码修复 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 让 Knife4j/Swagger 页面中的项目标题、接口说明、参数说明和模型字段说明显示为中文，并修复 API 可见层中的现有乱码。

**Architecture:** 通过 OpenAPI 配置、Controller 注解和模型 `@Schema` 注解补齐中文文档信息。先用测试锁定 OpenAPI 输出中的关键中文文本，再逐层修复配置、公共模型、控制器和直接暴露的实体，保证不改变接口协议。

**Tech Stack:** Spring Boot 3, Knife4j OpenAPI 3, JUnit 5, Mockito, Maven

---

### Task 1: 建立 OpenAPI 文档回归测试

**Files:**
- Create: `campus-server/src/test/java/com/campus/system/config/OpenApiDocumentationTest.java`
- Modify: `campus-server/pom.xml`

**Step 1: Write the failing test**

编写一个基于 Spring MVC 的 OpenAPI 测试，请求 `/v3/api-docs` 并断言返回内容中包含当前期望的中文标题、至少一个中文接口摘要和至少一个中文字段说明。

**Step 2: Run test to verify it fails**

Run: `mvn -pl campus-server -am -Dtest=OpenApiDocumentationTest test`
Expected: FAIL，原因是当前文档标题、接口摘要或字段说明还不是中文。

**Step 3: Write minimal implementation**

为测试补齐所需测试依赖与最小测试上下文配置，使测试可以稳定访问 OpenAPI 输出。

**Step 4: Run test to verify it passes**

Run: `mvn -pl campus-server -am -Dtest=OpenApiDocumentationTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add campus-server/pom.xml campus-server/src/test/java/com/campus/system/config/OpenApiDocumentationTest.java
git commit -m "test: add openapi documentation coverage"
```

### Task 2: 修复 OpenAPI 配置和公共模型说明

**Files:**
- Modify: `campus-server/src/main/java/com/campus/system/config/SwaggerConfig.java`
- Modify: `campus-common/src/main/java/com/campus/system/common/api/Result.java`
- Modify: `campus-common/src/main/java/com/campus/system/common/api/PageResult.java`
- Modify: `campus-common/src/main/java/com/campus/system/common/entity/BaseEntity.java`

**Step 1: Write the failing test**

在 `OpenApiDocumentationTest` 中增加断言，要求项目标题、描述和公共返回字段说明为正确中文。

**Step 2: Run test to verify it fails**

Run: `mvn -pl campus-server -am -Dtest=OpenApiDocumentationTest test`
Expected: FAIL，原因是标题或公共模型说明尚未修复。

**Step 3: Write minimal implementation**

修复 Swagger 配置中的乱码，并为统一返回结构、分页结构和基础实体字段补充 `@Schema` 中文描述。

**Step 4: Run test to verify it passes**

Run: `mvn -pl campus-server -am -Dtest=OpenApiDocumentationTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add campus-server/src/main/java/com/campus/system/config/SwaggerConfig.java campus-common/src/main/java/com/campus/system/common/api/Result.java campus-common/src/main/java/com/campus/system/common/api/PageResult.java campus-common/src/main/java/com/campus/system/common/entity/BaseEntity.java
git commit -m "feat: document common openapi models in chinese"
```

### Task 3: 中文化认证与系统管理接口

**Files:**
- Modify: `campus-server/src/main/java/com/campus/system/modules/auth/controller/AuthController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/auth/dto/LoginDTO.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/auth/vo/LoginVO.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/auth/vo/CaptchaVO.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/sys/controller/SysUserController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/sys/controller/SysRoleController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/sys/controller/SysMenuController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/sys/controller/SysLogController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/sys/controller/SysDictController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/sys/dto/SysUserCreateDTO.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/sys/dto/SysUserUpdateDTO.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/sys/dto/SysUserQueryDTO.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/sys/vo/SysUserVO.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/sys/vo/MenuTreeVO.java`

**Step 1: Write the failing test**

在 `OpenApiDocumentationTest` 中增加断言，要求认证和系统管理模块中出现中文标签、中文接口摘要和登录/用户相关字段说明。

**Step 2: Run test to verify it fails**

Run: `mvn -pl campus-server -am -Dtest=OpenApiDocumentationTest test`
Expected: FAIL，原因是相关模块尚未中文化。

**Step 3: Write minimal implementation**

为认证和系统管理接口补充 `@Tag`、`@Operation`、`@Parameter`、`@Schema` 注解，并修复 API 可见乱码字符串。

**Step 4: Run test to verify it passes**

Run: `mvn -pl campus-server -am -Dtest=OpenApiDocumentationTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add campus-server/src/main/java/com/campus/system/modules/auth campus-server/src/main/java/com/campus/system/modules/sys
git commit -m "feat: localize auth and system api docs"
```

### Task 4: 中文化校园服务模块接口

**Files:**
- Modify: `campus-server/src/main/java/com/campus/system/modules/svc/controller/DashboardController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/svc/controller/CampusRepairController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/svc/controller/CampusNoticeController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/svc/controller/CampusDormController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/svc/controller/CampusCardController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/svc/controller/CampusBookController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/svc/entity/*.java`

**Step 1: Write the failing test**

在 `OpenApiDocumentationTest` 中增加断言，要求校园服务模块文档中出现中文标签、图书相关接口摘要和图书实体字段说明。

**Step 2: Run test to verify it fails**

Run: `mvn -pl campus-server -am -Dtest=OpenApiDocumentationTest test`
Expected: FAIL

**Step 3: Write minimal implementation**

为校园服务模块控制器和直接暴露到 Swagger 的实体补中文注解，修复服务模块中会展示到接口文档或错误提示的乱码文案。

**Step 4: Run test to verify it passes**

Run: `mvn -pl campus-server -am -Dtest=OpenApiDocumentationTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add campus-server/src/main/java/com/campus/system/modules/svc
git commit -m "feat: localize campus service api docs"
```

### Task 5: 中文化教学模块接口

**Files:**
- Modify: `campus-server/src/main/java/com/campus/system/modules/edu/controller/EduAttendanceController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/edu/controller/EduCourseController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/edu/controller/EduEvaluationController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/edu/controller/EduLeaveController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/edu/controller/EduMaterialController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/edu/controller/EduScoreController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/edu/controller/EduTimetableController.java`
- Modify: `campus-server/src/main/java/com/campus/system/modules/edu/entity/*.java`

**Step 1: Write the failing test**

在 `OpenApiDocumentationTest` 中增加断言，要求教学模块文档中出现中文标签、课程或考勤相关接口摘要和教学实体字段说明。

**Step 2: Run test to verify it fails**

Run: `mvn -pl campus-server -am -Dtest=OpenApiDocumentationTest test`
Expected: FAIL

**Step 3: Write minimal implementation**

为教学模块控制器和直接暴露的实体补中文文档注解，并修复会进入接口文档的乱码文本。

**Step 4: Run test to verify it passes**

Run: `mvn -pl campus-server -am -Dtest=OpenApiDocumentationTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add campus-server/src/main/java/com/campus/system/modules/edu
git commit -m "feat: localize education api docs"
```

### Task 6: 完整验证与人工验收

**Files:**
- Modify: `docs/plans/2026-04-08-swagger-chinese-design.md`
- Modify: `docs/plans/2026-04-08-swagger-chinese-implementation.md`

**Step 1: Write the failing test**

补充最终的 OpenAPI 断言，确保四大模块的关键中文文案都出现在文档输出中。

**Step 2: Run test to verify it fails**

Run: `mvn -pl campus-server -am -Dtest=OpenApiDocumentationTest test`
Expected: FAIL，如果某个模块仍缺少关键说明。

**Step 3: Write minimal implementation**

补齐遗漏的文档注解或乱码修复，并更新计划文档中的实际执行情况说明。

**Step 4: Run test to verify it passes**

Run: `mvn -pl campus-server -am test`
Run: `mvn -pl campus-server -am package -DskipTests`
Expected: 全部通过

**Step 5: Commit**

```bash
git add docs/plans/2026-04-08-swagger-chinese-design.md docs/plans/2026-04-08-swagger-chinese-implementation.md campus-common campus-server
git commit -m "feat: localize swagger docs and fix api mojibake"
```
