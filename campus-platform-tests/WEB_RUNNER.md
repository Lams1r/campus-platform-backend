# 一键测试按钮页面

按钮页面面向使用测试平台的同学设计。每张测试卡片都会说明：

- 能测什么：这个测试关注哪个风险点。
- 通过什么方式测：它是访问页面、调用接口、传异常参数，还是做并发请求。
- 什么时候适合跑：部署后、上线前、接口变更后、权限变更后等。

## 测试套餐

- 全量共有 `127` 条自动化用例。
- 每个测试套餐绑定独立测试文件，只显示自己的用例，不会混入其他页面的测试。

- `smoke` 冒烟测试：通过 HTTP 访问首页、核心页面和基础 API，最快判断服务是否活着。
- `blackbox` 黑盒部署测试：只通过公网 URL 访问，不登录服务器、不连数据库，检查外部用户看到的系统是否可用。
- `deployment` 部署质量测试：检查静态资源、Content-Type、CORS 预检、API 错误页和 SPA fallback。
- `frontend` 前端页面测试：访问登录、注册、工作台、系统管理、教学管理、校园服务等页面路由。
- `contract` 契约测试：检查接口是否统一返回 `{code,msg,data}`、分页结构、验证码结构和时间格式。
- `negative` 负向输入测试：发送非法分页、错误 token、疑似 SQL/XSS/路径穿越输入，验证系统不会 500 或泄露堆栈。
- `business` 业务模块测试：检查公告、图书、报修、校园卡、宿舍、课程、考勤、请假、消息等只读业务接口。
- `api` 接口测试：批量调用公开接口和登录态接口，检查后端整体健康度。
- `rbac` 权限测试：使用无 token、坏 token、学生 token、管理员 token 检查权限边界。
- `security` 安全测试：检查敏感路径、错误堆栈、安全响应头等基础安全项。
- `performance` 性能测试：多次请求与小并发访问首页和核心 API，观察耗时和 500 错误。
- `full` 全量测试：执行全部已配置测试，适合最终验收和每日巡检。

## 用例数量

- 冒烟测试：`13` 条
- 黑盒部署测试：`10` 条
- 部署质量测试：`4` 条
- 前端页面测试：`20` 条
- 契约测试：`3` 条
- 负向输入测试：`12` 条
- 业务模块测试：`3` 条
- 接口测试：`40` 条
- 权限测试：`9` 条
- 安全测试：`8` 条
- 性能测试：`5` 条
- 全量测试：`127` 条

## 启动按钮页面

## 默认测试账号

测试包已经绑定三类账号：

- 学生：`ldx1` / `123456`
- 教师：`ldx3` / `123456`
- 管理员：`ldx2` / `123456`

这些账号用于生成 `STUDENT_TOKEN`、`TEACHER_TOKEN`、`ADMIN_TOKEN`，开启登录态接口和权限测试。

## 自动登录绕过验证码

队友后端需要配置：

```bash
export CAMPUS_TEST_LOGIN_ENABLED=true
export CAMPUS_TEST_LOGIN_SECRET=campus-test-login
```

测试平台会在登录时带请求头：

```text
X-Test-Login-Secret: campus-test-login
```

并使用固定测试验证码：

```text
captchaKey=__campus_test_login__
captchaCode=__campus_test_login__
```

不开后端开关时，这个能力不会生效，系统仍然走正常验证码登录。

```bash
cd /opt/campus-platform-tests
cp .env.example .env
bash scripts/start_web_runner.sh
```

通过 Nginx 访问：

```text
http://43.134.21.21/campus-test-runner/
```

按钮页面会调用：

```bash
bash scripts/run_tests.sh --target http://8.138.10.124 --api http://114.132.77.204:8080/api --profile <测试类型>
```

## systemd 后台运行

```bash
sudo cp deploy/campus-test-runner.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now campus-test-runner
sudo systemctl status campus-test-runner
```

## Nginx 配置

把 `deploy/nginx-campus-tests.conf` 加入现有 `server {}`：

```nginx
location /campus-tests/ {
    alias /opt/campus-platform-tests/reports/;
    index index.html;
    autoindex on;
}

location /campus-test-runner/ {
    proxy_pass http://127.0.0.1:8099/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

## 页面地址

```text
按钮入口：http://43.134.21.21/campus-test-runner/
报告总入口：http://43.134.21.21/campus-tests/index.html
冒烟实时页：http://43.134.21.21/campus-tests/smoke/latest-live.html
冒烟看板：http://43.134.21.21/campus-tests/smoke/latest-dashboard.html
接口实时页：http://43.134.21.21/campus-tests/api/latest-live.html
接口看板：http://43.134.21.21/campus-tests/api/latest-dashboard.html
全量看板：http://43.134.21.21/campus-tests/full/latest-dashboard.html
```

## 访问令牌

`.env.example` 默认包含：

```env
WEB_RUNNER_TOKEN=campus-test
```

页面会要求输入这个 token 才能触发测试。可以改成自己的密码。

## AI 总结页

每个测试目录都会生成：

```text
latest-summary.html
```

配置 `MIMO_API_KEY` 后，测试结束会调用小米 MiMo 兼容 OpenAI 接口生成 AI 总结；未配置时使用本地规则总结。密钥只放服务器 `.env`，不要写入源码。
