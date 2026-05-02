# Campus Platform 部署手册

> 把后端、数据库、Redis 部署到 **A 服务器(4核4G,114.132.77.204)**;
> 前端 + Nginx 反代部署到 **B 服务器(2核2G,8.138.10.124)**;
> 域名 `linyangsheng.top` 解析到 B 服务器,Nginx 把 `/api/*` 反代到 A 的 8080。
>
> 推到 main 分支即自动构建发布(GitHub Actions)。

---

## 总览

```
浏览器
  ↓ HTTPS (443)
B 服务器 8.138.10.124  Nginx
  ├── /          → /opt/campus-frontend/dist
  ├── /api/*     → http://114.132.77.204:8080/api/*
  └── /uploads/* → http://114.132.77.204:8080/uploads/*
                          ↓
                A 服务器 114.132.77.204  Spring Boot (8080)
                          ↓ 127.0.0.1
                MySQL 8 + Redis 7 + /opt/campus-platform/uploads
```

---

## 一、DNS 解析

去阿里云 / 腾讯云 DNS 控制台,加两条记录:

| 主机记录 | 类型 | 解析值 |
|---|---|---|
| `@` | A | `8.138.10.124` |
| `www` | CNAME | `linyangsheng.top` |

等几分钟后用 `ping linyangsheng.top` 验证。

---

## 二、A 服务器(后端 + DB)一次性准备

> 操作系统假设是 Ubuntu 22.04。CentOS 把 `apt` 换成 `dnf` 即可。

### 1. 装基础环境

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk mysql-server redis-server curl rsync
java -version  # 应显示 17
```

### 2. 创建 deploy 用户和目录

```bash
sudo useradd -m -s /bin/bash deploy
sudo mkdir -p /opt/campus-platform/{app,logs,uploads}
sudo chown -R deploy:deploy /opt/campus-platform
```

### 3. 给 deploy 配 SSH 公钥(GitHub Actions 用)

**本地** 先生成一对密钥(只为部署专用,不要复用个人密钥):

```bash
ssh-keygen -t ed25519 -f ~/.ssh/campus_backend_deploy -C "github-actions-backend" -N ""
```

把 **公钥** `~/.ssh/campus_backend_deploy.pub` 内容复制,在 A 服务器上:

```bash
sudo -u deploy mkdir -p /home/deploy/.ssh
sudo -u deploy chmod 700 /home/deploy/.ssh
sudo -u deploy bash -c 'cat >> /home/deploy/.ssh/authorized_keys << "EOF"
<把 campus_backend_deploy.pub 全部内容粘到这里>
EOF'
sudo -u deploy chmod 600 /home/deploy/.ssh/authorized_keys
```

测试:

```bash
ssh -i ~/.ssh/campus_backend_deploy deploy@114.132.77.204 "echo ok"
```

### 4. 给 deploy 免密重启服务的权限

```bash
sudo tee /etc/sudoers.d/deploy <<'EOF'
deploy ALL=(ALL) NOPASSWD: /bin/systemctl restart campus-platform, /bin/systemctl status campus-platform, /usr/bin/journalctl -u campus-platform *
EOF
sudo chmod 440 /etc/sudoers.d/deploy
```

### 5. MySQL 初始化

```bash
sudo mysql_secure_installation   # 设置 root 密码,其它都选 Y
sudo mysql -uroot -p
```

进入 MySQL 后执行:

```sql
CREATE DATABASE campus_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'campus_app'@'127.0.0.1' IDENTIFIED BY '换成强密码';
GRANT ALL PRIVILEGES ON campus_system.* TO 'campus_app'@'127.0.0.1';
FLUSH PRIVILEGES;
EXIT;
```

确保 MySQL 只监听本地:

```bash
sudo sed -i 's/^bind-address.*/bind-address = 127.0.0.1/' /etc/mysql/mysql.conf.d/mysqld.cnf
sudo systemctl restart mysql
```

把项目里的 `sql/` 目录拷到服务器导入(也可以手工在 Navicat 里导):

```bash
# 在 A 服务器上(假设你已经把 sql/ 上传)
mysql -ucampus_app -p campus_system < /tmp/sql/init.sql
```

### 6. Redis 配置

```bash
sudo sed -i 's/^# requirepass .*/requirepass 换成强密码/' /etc/redis/redis.conf
sudo sed -i 's/^bind .*/bind 127.0.0.1/' /etc/redis/redis.conf
sudo systemctl restart redis-server
redis-cli -a 换成强密码 ping  # 应返回 PONG
```

### 7. 写环境变量文件

```bash
sudo cp /tmp/app.env.example /opt/campus-platform/app/app.env
sudo nano /opt/campus-platform/app/app.env   # 替换里面所有"换成强密码"
sudo chown deploy:deploy /opt/campus-platform/app/app.env
sudo chmod 600 /opt/campus-platform/app/app.env
```

> 模板见仓库 `deploy/app.env.example`。

### 8. 安装 systemd 单元

把仓库的 `deploy/campus-platform.service` 上传到 A 服务器:

```bash
sudo cp /tmp/campus-platform.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable campus-platform
# 此时还没有 jar,先不要启动,等首次部署
```

### 9. 防火墙 / 安全组

在云控制台(阿里云/腾讯云)配置安全组:

| 端口 | 协议 | 来源 | 用途 |
|---|---|---|---|
| 22 | TCP | 你的家庭 IP | SSH |
| 22 | TCP | GitHub Actions IP 段(可临时全开,后期收紧) | CI 部署 |
| 8080 | TCP | `8.138.10.124/32`(B 服务器) | 仅供 B 反代访问 |

> 不要开 3306 和 6379 给公网。

---

## 三、B 服务器(前端 + Nginx)一次性准备

### 1. 装 Nginx 和 certbot

```bash
sudo apt update
sudo apt install -y nginx certbot python3-certbot-nginx rsync
```

### 2. 创建 deploy 用户和目录

```bash
sudo useradd -m -s /bin/bash deploy
sudo mkdir -p /opt/campus-frontend/dist
sudo chown -R deploy:deploy /opt/campus-frontend
```

### 3. 配前端部署用 SSH 公钥

本地再生成一对(和后端那对**分开**):

```bash
ssh-keygen -t ed25519 -f ~/.ssh/campus_frontend_deploy -C "github-actions-frontend" -N ""
```

把公钥贴到 B 服务器 `/home/deploy/.ssh/authorized_keys`(同 A 服务器步骤 3)。

### 4. 写 Nginx 配置(先 HTTP,等证书后再改 HTTPS)

把仓库 `deploy/nginx-campus.conf` 上传到 B 服务器:

```bash
sudo cp /tmp/nginx-campus.conf /etc/nginx/conf.d/campus.conf
```

**首次还没证书**,先临时把 HTTPS 部分注释掉,只保留 80 端口配置最简版本:

```bash
sudo tee /etc/nginx/conf.d/campus.conf <<'EOF'
server {
    listen 80;
    server_name linyangsheng.top www.linyangsheng.top;

    root /opt/campus-frontend/dist;
    index index.html;

    location /.well-known/acme-challenge/ { root /var/www/certbot; }

    location / { try_files $uri $uri/ /index.html; }

    location /api/ {
        proxy_pass http://114.132.77.204:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
EOF

sudo mkdir -p /var/www/certbot
sudo nginx -t && sudo systemctl reload nginx
```

### 5. 申请 HTTPS 证书

DNS 解析生效后:

```bash
sudo certbot --nginx -d linyangsheng.top -d www.linyangsheng.top
# 按提示输入邮箱、同意协议
# certbot 会自动改写 nginx 配置加 HTTPS 块,并设置 80→443 跳转
```

certbot 完成后,**用仓库 `deploy/nginx-campus.conf` 覆盖** 之前的临时配置(里面有完整的 SPA 缓存、安全头、上传反代):

```bash
sudo cp /tmp/nginx-campus.conf /etc/nginx/conf.d/campus.conf
sudo nginx -t && sudo systemctl reload nginx
```

证书每 90 天自动续:

```bash
sudo systemctl status certbot.timer  # 应该是 active (waiting)
```

### 6. 防火墙 / 安全组

| 端口 | 协议 | 来源 | 用途 |
|---|---|---|---|
| 22 | TCP | 你的家庭 IP | SSH |
| 80 | TCP | `0.0.0.0/0` | HTTP(跳 HTTPS) |
| 443 | TCP | `0.0.0.0/0` | HTTPS |

---

## 四、配置 GitHub Secrets

### 后端仓库 `campus-platform-backend` → Settings → Secrets and variables → Actions

| Name | Value |
|---|---|
| `BACKEND_SSH_KEY` | `~/.ssh/campus_backend_deploy` 的**私钥**全部内容(以 `-----BEGIN ...-----` 开头) |
| `BACKEND_HOST` | `114.132.77.204` |
| `BACKEND_USER` | `deploy` |

### 前端仓库 `campus-platform-frontend` → Settings → Secrets and variables → Actions

| Name | Value |
|---|---|
| `FRONTEND_SSH_KEY` | `~/.ssh/campus_frontend_deploy` 的**私钥**全部内容 |
| `FRONTEND_HOST` | `8.138.10.124` |
| `FRONTEND_USER` | `deploy` |

> 旧的 `SERVER_SSH_KEY` / `SERVER_IP` / `SERVER_USER` 可以删掉。

---

## 五、首次发布

### 1. 后端

直接 push 到 main:

```bash
cd /f/softwareProject/campusPlatform
git add .
git commit -m "chore: 生产环境部署配置"
git push origin main
```

去 GitHub 仓库 Actions 页观察 workflow 跑完。第一次会失败,因为服务还没起。SSH 上 A 服务器手动启动一次:

```bash
ssh deploy@114.132.77.204
sudo systemctl start campus-platform
sudo systemctl status campus-platform   # 看是否 active (running)
sudo journalctl -u campus-platform -f   # 实时日志,看启动是否成功
```

成功后再次推送任意小改动验证 CI 自动部署。

### 2. 前端

```bash
cd /f/softwareProject/campus-platform-frontend/campus-platform-frontend
git add .
git commit -m "chore: 生产环境构建配置"
git push origin main
```

GitHub Actions 跑完后,浏览器访问 `https://linyangsheng.top` 应能看到登录页。

---

## 六、验证清单

按顺序逐一确认:

- [ ] `ping linyangsheng.top` 解析到 `8.138.10.124`
- [ ] `https://linyangsheng.top` 打开能看到前端
- [ ] 浏览器 F12 网络面板,登录请求打到 `https://linyangsheng.top/api/auth/login` 返回 200
- [ ] A 服务器上 `sudo systemctl status campus-platform` 显示 active (running)
- [ ] A 服务器上 `tail -f /opt/campus-platform/logs/campus-platform.log` 有正常请求日志
- [ ] 上传文件功能正常(浏览器上传 → 落到 A 的 `/opt/campus-platform/uploads/`)

---

## 七、常用运维命令(A 服务器)

```bash
# 看服务状态
sudo systemctl status campus-platform

# 重启
sudo systemctl restart campus-platform

# 看实时日志
sudo journalctl -u campus-platform -f
tail -f /opt/campus-platform/logs/campus-platform.log

# 看错误日志
tail -f /opt/campus-platform/logs/campus-platform-error.log

# 紧急回滚(部署后发现问题)
cd /opt/campus-platform/app
mv app.jar app.jar.broken
mv app.jar.bak app.jar
sudo systemctl restart campus-platform
```

## 八、常用运维命令(B 服务器)

```bash
# 测试 nginx 配置
sudo nginx -t

# 重载 nginx(改完配置后)
sudo systemctl reload nginx

# 看访问日志
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log

# 手动续证书(正常 certbot.timer 自动续)
sudo certbot renew --dry-run
```

---

## 九、常见问题

### Q1. GitHub Actions 报 `Permission denied (publickey)`
- 检查 secret `BACKEND_SSH_KEY` 是否完整复制(包括 BEGIN/END 行)
- 公钥是否正确加到 `/home/deploy/.ssh/authorized_keys`
- 服务器 `/home/deploy/.ssh` 权限必须 700,`authorized_keys` 必须 600

### Q2. 前端打开页面但 API 请求 502 / Bad Gateway
- A 服务器服务挂了:`ssh deploy@114.132.77.204 sudo systemctl status campus-platform`
- A 服务器安全组没放行 8080 给 B 的 IP
- Nginx 的 upstream IP 写错(`/etc/nginx/conf.d/campus.conf`)

### Q3. 后端起不来 / 启动后立刻挂
- `sudo journalctl -u campus-platform -n 200 --no-pager`
- 99% 是 `app.env` 里数据库/Redis 密码不对,或 MySQL/Redis 没起来

### Q4. 健康检查失败导致 CI 自动回滚
- workflow 里检查 `/api/doc.html`,如果你删了 knife4j 改用别的健康端点,需要同步改 `.github/workflows/deploy.yml` 的 `curl` 路径(推荐改成 actuator):
  - 在 pom 加 `spring-boot-starter-actuator`
  - 改成 `curl -fsS http://127.0.0.1:8080/api/actuator/health`

### Q5. 上传的图片访问 404
- 检查 A 服务器 `/opt/campus-platform/uploads/` 目录权限是否 deploy:deploy
- 后端的资源映射(WebMvcConfigurer)是否把 `/uploads/**` 映射到 `${campus.upload-path}`

---

## 十、后续可选优化

1. **后端容器化**:加 Dockerfile + docker-compose,把 MySQL/Redis 也容器化,迁移更方便
2. **对象存储**:上传文件改用阿里云 OSS / 腾讯云 COS,A 服务器无状态,扩容容易
3. **监控**:加 Prometheus + Grafana 监控 JVM、MySQL
4. **CDN**:静态资源走 CDN,2G 服务器带宽不再是瓶颈
5. **蓝绿/灰度**:同机起两个端口,Nginx upstream 切换,实现零停机部署
