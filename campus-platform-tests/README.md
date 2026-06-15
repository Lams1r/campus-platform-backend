# Campus Platform 自动化测试套件

这是部署在你的测试服务器上的黑盒自动化测试工程，用来测试前端和后端：

```text
前端：http://8.138.10.124
后端 API：http://114.132.77.204:8080/api
```

你的测试服务器报告入口：

```text
http://43.134.21.21/campus-tests/
```

## 一键按钮页面

部署后可以打开：

```text
http://43.134.21.21/campus-test-runner/
```

页面里有这些按钮：

- 冒烟测试：`13` 条，最快检查服务是否活着。
- 黑盒部署测试：`10` 条，检查公网访问、Nginx 转发、SPA 刷新、静态资源。
- 部署质量测试：`4` 条，专门检查上线配置、API 错误页、资源类型、CORS 预检。
- 前端页面测试：`20` 条，巡检登录、注册、工作台、系统、教学、校园服务页面。
- 契约测试：`3` 条，检查统一返回结构、分页结构、验证码结构和时间格式。
- 负向输入测试：`12` 条，检查非法分页、错误 token、疑似注入和错误方法。
- 业务模块测试：`3` 条，集中跑校园和教务模块核心只读接口。
- 接口测试：`40` 条，检查接口契约、业务读取接口、时间格式。
- 权限测试：`9` 条，检查匿名访问、错误 token、学生访问管理员接口等边界。
- 安全测试：`8` 条，检查敏感路径、错误堆栈、安全响应头。
- 性能测试：`5` 条，检查首页和核心 API 响应时间。
- 全量测试：`127` 条，跑全部已配置测试。

注意：每个按钮页面现在绑定独立测试文件，只显示自己的用例；各套餐数量相加等于全量 `127` 条。

按钮页面详细说明见 `WEB_RUNNER.md`。

## 快速运行

```bash
cd /opt/campus-platform-tests
cp .env.example .env
bash scripts/run_tests.sh --target http://8.138.10.124 --api http://114.132.77.204:8080/api --profile smoke
```

全量测试：

```bash
bash scripts/run_tests.sh --target http://8.138.10.124 --api http://114.132.77.204:8080/api --profile full
```

单独跑某类测试：

```bash
bash scripts/run_tests.sh --target http://8.138.10.124 --api http://114.132.77.204:8080/api --profile deployment
bash scripts/run_tests.sh --target http://8.138.10.124 --api http://114.132.77.204:8080/api --profile contract
bash scripts/run_tests.sh --target http://8.138.10.124 --api http://114.132.77.204:8080/api --profile negative
bash scripts/run_tests.sh --target http://8.138.10.124 --api http://114.132.77.204:8080/api --profile frontend
bash scripts/run_tests.sh --target http://8.138.10.124 --api http://114.132.77.204:8080/api --profile business
```

## 默认测试账号

`.env.example` 已绑定以下测试账号：

- 学生：`ldx1`，密码：`123456`
- 教师：`ldx3`，密码：`123456`
- 管理员：`ldx2`，密码：`123456`

需要跑登录态、权限、业务测试时，可以用这些账号通过 `scripts/get_token.py` 生成 token，然后填回 `.env`。

## 自动登录绕过验证码

登录态测试统一走后端测试专用登录旁路，稳定且可控。

队友后端需要部署支持测试登录的版本，并设置：

```bash
export CAMPUS_TEST_LOGIN_ENABLED=true
export CAMPUS_TEST_LOGIN_SECRET=campus-test-login
```

测试包 `.env.example` 已配置相同密钥：

```env
TEST_LOGIN_SECRET=campus-test-login
TEST_LOGIN_CAPTCHA_KEY=__campus_test_login__
TEST_LOGIN_CAPTCHA_CODE=__campus_test_login__
```

开启后，登录态、权限、业务测试会自动用 `ldx1`、`ldx3`、`ldx2` 获取 token。也可以手动生成：

```bash
python scripts/get_token.py --role student --auto
python scripts/get_token.py --role teacher --auto
python scripts/get_token.py --role admin --auto
```

## 启动按钮页面

临时启动：

```bash
cd /opt/campus-platform-tests
bash scripts/start_web_runner.sh
```

systemd 后台启动：

```bash
sudo cp deploy/campus-test-runner.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now campus-test-runner
```

Nginx 反代配置见：

```text
deploy/nginx-campus-tests.conf
```

## 报告页面

报告总入口：

```text
http://43.134.21.21/campus-tests/index.html
```

每个测试套餐都有独立目录，例如：

```text
冒烟实时页：http://43.134.21.21/campus-tests/smoke/latest-live.html
冒烟看板：http://43.134.21.21/campus-tests/smoke/latest-dashboard.html
接口实时页：http://43.134.21.21/campus-tests/api/latest-live.html
接口看板：http://43.134.21.21/campus-tests/api/latest-dashboard.html
接口 AI 总结：http://43.134.21.21/campus-tests/api/latest-summary.html
安全实时页：http://43.134.21.21/campus-tests/security/latest-live.html
安全看板：http://43.134.21.21/campus-tests/security/latest-dashboard.html
安全 AI 总结：http://43.134.21.21/campus-tests/security/latest-summary.html
全量看板：http://43.134.21.21/campus-tests/full/latest-dashboard.html
全量 AI 总结：http://43.134.21.21/campus-tests/full/latest-summary.html
```

## 小米 AI 总结

测试跑完会自动生成 `latest-summary.html`。如果 `.env` 里配置了小米 MiMo API Key，会调用小米 AI 根据真实测试结果生成总结；没有配置时会使用本地规则生成兜底总结。

```bash
cd /opt/campus-platform-tests
grep -q '^MIMO_API_KEY=' .env || echo 'MIMO_API_KEY=' >> .env
sudo systemctl restart campus-test-runner
```

把 `MIMO_API_KEY=` 后面填成你的真实密钥即可，不建议写进源码或截图展示。

pytest 原始 HTML 报告会归档在：

```text
/opt/campus-platform-tests/reports/<测试类型>/
```

## 上传命令

在 Windows PowerShell 执行：

```powershell
scp C:\Users\a1627\Desktop\campus-platform\release\campus-platform-tests-deploy-20260615-ai.zip ubuntu@43.134.21.21:/opt/
```

服务器解压：

```bash
ssh ubuntu@43.134.21.21
cd /opt
sudo unzip -o campus-platform-tests-deploy-20260615-ai.zip
sudo chown -R ubuntu:ubuntu /opt/campus-platform-tests
cd /opt/campus-platform-tests
cp .env.example .env
```

## 说明

默认不会执行删除、审核、充值、导入、状态修改等会影响真实数据的接口。登录态和权限测试需要在 `.env` 里配置 `ADMIN_TOKEN`、`TEACHER_TOKEN`、`STUDENT_TOKEN`，没有配置时会自动跳过相关用例。
