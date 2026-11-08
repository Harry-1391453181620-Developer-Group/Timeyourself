# TimeYourself on Ubuntu

本文是 Ubuntu 端的运行、打包、安装和排错说明。Windows 行为不变，macOS 见 [MACOS.md](MACOS.md)。

## 1. 支持范围

| 项目 | 支持情况 |
|---|---|
| 发行版 | **Ubuntu 22.04 LTS / 24.04 LTS / 26.04 LTS** |
| 架构 | x86_64 (`amd64`)、aarch64 (`arm64`) |
| glibc | **>= 2.35**（CI 在 Ubuntu 22.04 上构建，一个包同时覆盖 22.04 / 24.04 / 26.04） |
| 桌面 | X11，或 Wayland（通过 XWayland） |
| Java | 安装包自带精简 JRE 26，**目标机器不需要装 Java** |
| 数据 | `$XDG_DATA_HOME/timeyourself`，默认 `~/.local/share/timeyourself` |

> 只支持 Ubuntu LTS。其他发行版不在支持范围内，[scripts/run-ubuntu.sh](scripts/run-ubuntu.sh) 和 [scripts/package-ubuntu.sh](scripts/package-ubuntu.sh) 会在非 Ubuntu 系统上直接报错退出。
>
> **Ubuntu 25.04 / 25.10 不支持**：它们是非 LTS 临时版，已经 EOL，请升级到 24.04 LTS 或 26.04 LTS。
>
> `jpackage` 不支持交叉编译：Windows/macOS 上不能直接产出 Ubuntu `.deb`，请用 Ubuntu 机器或 GitHub Actions。

## 2. 与 Windows / macOS 的差异

| 项目 | Windows | macOS | Ubuntu |
|---|---|---|---|
| 数据目录 | 程序目录下 `data/` | `~/Library/Application Support/TimeYourself` | `~/.local/share/timeyourself`（遵守 `XDG_DATA_HOME`） |
| 日志 | `data/timeyourself.log` | 同数据目录 | 同数据目录 |
| 安装位置 | `%LOCALAPPDATA%\TimeYourself` 或自选 | `/Applications/TimeYourself.app` | `/opt/timeyourself` |
| 安装包 | `.exe`（jpackage） | `.dmg` | `.deb` + 便携 `.tar.gz` |
| 端口 | 源码 8081；打包版自动选空闲端口 | 同左 | 同左 |
| 监听地址 | `127.0.0.1` | `127.0.0.1` | `127.0.0.1` |
| 中文字体 | 内置 Noto Sans SC | 内置 Noto Sans SC | 内置 Noto Sans SC |

Ubuntu 使用 XDG 目录是**必须**的：`.deb` 默认装到 `/opt/timeyourself`，普通用户对 `/opt` 没有写权限，如果继续用 `./data`，第一次启动就会因为建不了 SQLite 文件而所有接口 500。

## 3. 从源码运行

### 3.1 安装 JDK 26

Ubuntu 仓库通常没有 JDK 26，推荐 Temurin：

1. 打开 <https://adoptium.net/temurin/releases/?version=26>；
2. 选 Linux、对应架构（x64 = `x86_64`，aarch64 = `arm64`）、JDK、`.tar.gz`；
3. 解压并设置 `JAVA_HOME`：

```bash
sudo mkdir -p /opt/jdk-26
sudo tar -xzf ~/Downloads/OpenJDK26U-jdk_*_linux_*.tar.gz -C /opt/jdk-26 --strip-components=1

export JAVA_HOME=/opt/jdk-26
export PATH="$JAVA_HOME/bin:$PATH"
java -version    # 必须是 26
```

可以把上面两行 `export` 写进 `~/.bashrc`。

### 3.2 安装运行依赖

Ubuntu 桌面版一般已经具备；最小化安装需要补。**22.04 与 24.04/26.04 的 GTK/GLib/ALSA 包名不同**：

**Ubuntu 22.04 LTS：**

```bash
sudo apt update
sudo apt install -y \
  libgtk-3-0 libx11-6 libxext6 libxrender1 libxtst6 libxi6 \
  libglib2.0-0 libfontconfig1 libfreetype6 libnss3 libasound2 \
  libgstreamer1.0-0 libgstreamer-plugins-base1.0-0
```

**Ubuntu 24.04 / 26.04 LTS（两者包名相同）：**

```bash
sudo apt update
sudo apt install -y \
  libgtk-3-0t64 libx11-6 libxext6 libxrender1 libxtst6 libxi6 \
  libglib2.0-0t64 libfontconfig1 libfreetype6 libnss3 libasound2t64 \
  libgstreamer1.0-0 libgstreamer-plugins-base1.0-0
```

Maven：

```bash
sudo apt install -y maven
mvn -version
```

### 3.3 启动

```bash
cd /path/to/Time_Yourself
chmod +x scripts/run-ubuntu.sh
./scripts/run-ubuntu.sh
```

脚本会：

1. 检查当前系统是不是 Ubuntu（不是就直接报错）；
2. 检查 JDK 26 和 Maven；
3. 执行 `mvn spring-boot:run`。

启动后：

- 弹出 JavaFX 原生窗口（不是浏览器标签页）；
- 后端监听 `http://127.0.0.1:8081`；
- 数据目录：`~/.local/share/timeyourself/`。

另开终端验证：

```bash
curl -s http://127.0.0.1:8081/api/ping
# {"status":"ok","sqlite":"3.50.3"}
```

如果是在纯 SSH 会话里运行，没有 `DISPLAY` / `WAYLAND_DISPLAY`，JavaFX 窗口无法打开；请在桌面会话里运行，或只用 `TIMEYOURSELF_HEADLESS=1` 做接口测试（见第 8 节）。

## 4. 构建 `.deb`

### 4.1 构建依赖

```bash
sudo apt install -y fakeroot dpkg-dev
```

`jpackage --type deb` 需要 `dpkg`、`dpkg-deb` 和 `fakeroot`。只想要便携版可以跳过：

```bash
SKIP_DEB=1 ./scripts/package-ubuntu.sh
```

### 4.2 一键打包

```bash
cd /path/to/Time_Yourself
chmod +x scripts/package-ubuntu.sh
./scripts/package-ubuntu.sh
```

脚本依次做：

1. 检查系统是 Ubuntu；
2. `mvn -DskipTests clean package`；
3. `jpackage --type app-image` 生成便携版（自带 JRE）；
4. 打 `tar.gz`；
5. `jpackage --type deb` 生成 Ubuntu 安装包；
6. 校验 `.deb` 结构、GTK 依赖、写 `SHA256SUMS`。

产物：

```text
build/ubuntu/
├── app/TimeYourself/                                  # 便携版（可直接运行）
├── TimeYourself-2.0.0-x86_64-ubuntu.tar.gz            # 便携版压缩包
├── deb/timeyourself_2.0.0_amd64.deb                   # Ubuntu 安装包
└── SHA256SUMS
```

架构名：x86_64 机器产出 `amd64` deb，aarch64 机器产出 `arm64` deb。

**构建机选择：**

| 构建机 | 结果 |
|---|---|
| Ubuntu 22.04 | `.deb` 同时支持 22.04 / 24.04 / 26.04（推荐，CI 用这个） |
| Ubuntu 24.04 | `.deb` 要求 glibc 2.39，**不能在 22.04 上运行**；脚本会警告 |
| Ubuntu 26.04 | `.deb` 要求 glibc 2.43，**不能在 22.04 / 24.04 上运行**；脚本会警告 |

可选环境变量：

```bash
APP_VERSION=2.0.1 \
MAINTAINER="you@example.com" \
./scripts/package-ubuntu.sh
```

`MAINTAINER` 只填邮箱；jpackage 会自动拼成 `Vendor <email>` 写进 dpkg 的 control 文件。

### 4.3 没有 Ubuntu 机器

用仓库里的 GitHub Actions：[.github/workflows/ubuntu.yml](.github/workflows/ubuntu.yml)。

1. 推到 `main`，或手动 **Actions → Build Ubuntu DEB → Run workflow**；
2. 构建两个架构：`Ubuntu 22.04 x86_64`、`Ubuntu 22.04 aarch64`；
3. 另有 `Ubuntu 24.04 compatibility` 和 `Ubuntu 26.04 compatibility` 两个任务：下载 x86_64 产物，在对应版本上验证 `.deb` 依赖解析；
4. 下载 artifact：

```text
TimeYourself-Ubuntu-x86_64-run<N>-attempt<M>.zip
TimeYourself-Ubuntu-aarch64-run<N>-attempt<M>.zip
```

> arm64 runner 仅对 **public repository** 免费可用。如果仓库是私有的，删掉 matrix 里的 `ubuntu-22.04-arm` 一项即可，x86_64 不受影响。

## 5. 安装与卸载

### 5.1 安装

```bash
# 在项目根目录执行；arm64 把 amd64 换成 arm64
sudo apt install ./build/ubuntu/deb/timeyourself_2.0.0_amd64.deb
```

用 `apt install ./xxx.deb` 而不是 `dpkg -i`，这样缺失的依赖会被自动补齐。

`.deb` 通过依赖别名同时覆盖 22.04 / 24.04 / 26.04：

```text
libgtk-3-0 | libgtk-3-0t64
libglib2.0-0 | libglib2.0-0t64
libasound2 | libasound2t64
```

> `.deb` 的安装脚本会用 `xdg-desktop-menu` 注册菜单项，因此需要桌面环境。纯服务器/容器请用便携版 + `TIMEYOURSELF_HEADLESS=1`（只提供本机 API，不打开窗口）。

安装后：

| 内容 | 路径 |
|---|---|
| 程序 | `/opt/timeyourself` |
| 启动器 | `/opt/timeyourself/bin/TimeYourself` |
| 桌面菜单项 | 由 `xdg-desktop-menu` 安装到 `/usr/share/applications/`（文件名以 `timeyourself-` 开头） |
| 数据 | `~/.local/share/timeyourself/` |

从应用菜单搜索 **TimeYourself** 启动，或：

```bash
/opt/timeyourself/bin/TimeYourself
```

### 5.2 卸载

```bash
sudo apt remove timeyourself
```

`apt remove` 只删程序，**不会删** `~/.local/share/timeyourself/`。要彻底清除数据：

```bash
rm -rf ~/.local/share/timeyourself
```

### 5.3 便携版

```bash
tar -xzf TimeYourself-2.0.0-x86_64-ubuntu.tar.gz
./TimeYourself/bin/TimeYourself
```

便携版同样把数据写到 `~/.local/share/timeyourself`。如果想让数据跟着文件夹走：

```bash
TIMEYOURSELF_DATA_DIR="$PWD/TimeYourself/data" ./TimeYourself/bin/TimeYourself
```

## 6. 数据、备份与迁移

默认位置：

```text
~/.local/share/timeyourself/
├── timeyourself.db     # 全部任务/分类/计划/事件
└── timeyourself.log    # 启动和接口日志
```

可用环境变量覆盖：

```bash
TIMEYOURSELF_DATA_DIR=/path/to/data ./TimeYourself/bin/TimeYourself
# 源码运行同理：
TIMEYOURSELF_DATA_DIR=/path/to/data ./scripts/run-ubuntu.sh
```

打包版请用环境变量：jpackage 启动器不会把命令行里的 `-D...` 转成 JVM 系统属性。

备份只需复制一个文件：

```bash
cp ~/.local/share/timeyourself/timeyourself.db ~/timeyourself-backup.db
```

### 从旧的源码目录 `data/` 迁移

如果你之前在 Ubuntu 源码目录下已经用 `./data` 建过库（旧版本行为），把它搬到 XDG 目录：

```bash
mkdir -p ~/.local/share/timeyourself
cp data/timeyourself.db ~/.local/share/timeyourself/
```

也可以临时指定旧目录继续用：

```bash
TIMEYOURSELF_DATA_DIR="$PWD/data" ./scripts/run-ubuntu.sh
```

### 从 Windows 迁移

```bash
mkdir -p ~/.local/share/timeyourself
cp /media/usb/timeyourself.db ~/.local/share/timeyourself/
```

### 从 macOS 迁移

```bash
mkdir -p ~/.local/share/timeyourself
cp "$HOME/Library/Application Support/TimeYourself/timeyourself.db" \
   ~/.local/share/timeyourself/
```

> 不要用 `sudo` 启动应用。用 sudo 会把数据写到 `/root/.local/share/timeyourself`，而且 SQLite 文件属主会变成 root。
> 首次启动时程序会把数据目录权限收紧为 `700`、数据库为 `600`（仅当前用户可读写）。

## 7. 中文与字体

- 应用内置 `Noto Sans SC`（`static/fonts/`），CSS 里注册为 `TimeYourself Sans`，**不依赖系统安装中文字体**；
- 打包脚本会把整个 jar（含字体）放进 `.deb`，无需额外资源；
- JDK 26 默认 `file.encoding=UTF-8`（JEP 400），启动参数里也显式设置了 `-Dfile.encoding=UTF-8`；
- 如果界面中文显示为方块，检查：

```bash
fc-list :lang=zh | head
```

即使 `fc-list` 没有中文，内置字体也应该正常；如果仍是方块，把 `~/.local/share/timeyourself/timeyourself.log` 发出来。

### 输入法

JavaFX 通过 GTK 输入法模块工作。Ubuntu 上常见的是 ibus（默认）或 fcitx5：

```bash
# ibus（Ubuntu 默认）
export GTK_IM_MODULE=ibus
export XMODIFIERS=@im=ibus

# fcitx5
export GTK_IM_MODULE=fcitx
export QT_IM_MODULE=fcitx
export XMODIFIERS=@im=fcitx
```

把对应几行写进 `~/.profile` 或 `~/.bashrc`，重新登录。

## 8. 显示、缩放与 headless

### 8.1 Wayland

Ubuntu 24.04 / 26.04 默认 Wayland。JavaFX 26 通过 XWayland 运行；若出现窗口模糊、位置异常或输入法问题，强制走 X11：

```bash
GDK_BACKEND=x11 ./TimeYourself/bin/TimeYourself
```

### 8.2 高 DPI

```bash
GDK_SCALE=2 ./TimeYourself/bin/TimeYourself
```

JavaFX 也会读取 `GDK_SCALE` / `GDK_DPI_SCALE`；一般不需要手动设置。

### 8.3 headless（只用于 CI/接口测试）

```bash
TIMEYOURSELF_HEADLESS=1 \
TIMEYOURSELF_DATA_DIR=/tmp/ty-test \
/opt/timeyourself/bin/TimeYourself
```

`TIMEYOURSELF_HEADLESS=1` 不打开窗口，但仍启动 Tomcat，适合服务器/CI 冒烟测试。

> 用 jpackage 启动器时请用环境变量；`-Dtimeyourself.headless=true` 只在直接 `java -jar` 时有效，启动器不会把未知参数转成 JVM 系统属性。

## 9. 排错

| 现象 | 原因 | 处理 |
|---|---|---|
| `error while loading shared libraries: libgtk-3.so.0` | 缺 GTK3 | 22.04：`sudo apt install libgtk-3-0`；24.04/26.04：`sudo apt install libgtk-3-0t64` |
| `libasound.so.2: cannot open shared object file` | 缺 ALSA | 22.04：`sudo apt install libasound2`；24.04/26.04：`sudo apt install libasound2t64` |
| `libglib-2.0.so.0: cannot open shared object file` | 缺 GLib | 22.04：`sudo apt install libglib2.0-0`；24.04/26.04：`sudo apt install libglib2.0-0t64` |
| `libXtst.so.6: cannot open shared object file` | 缺 X11 扩展库 | `sudo apt install libxtst6` |
| `GLIBC_2.35 not found` | 系统太老 | 升级到 Ubuntu 22.04+，或在 22.04 上重新构建 |
| `No DISPLAY` / `Unable to open DISPLAY` | 在无图形会话的 SSH 里启动 | 到桌面会话启动，或加 `TIMEYOURSELF_HEADLESS=1` 仅跑接口 |
| 双击菜单无反应 | 启动器 stderr 不可见 | 终端执行 `/opt/timeyourself/bin/TimeYourself` 看报错 |
| 窗口白屏 / WebView 崩溃 | GPU/驱动问题 | 试 `JAVA_TOOL_OPTIONS="-Dprism.order=sw" /opt/timeyourself/bin/TimeYourself` |
| `Failed to create unpack directory` | `TMPDIR` 指向不可写或 noexec 的位置 | `TMPDIR=/var/tmp /opt/timeyourself/bin/TimeYourself` |
| 页面能开但所有操作报“请求失败” | 数据目录不可写 / 数据库没建 | 看 `~/.local/share/timeyourself/timeyourself.log`；确认不是用 sudo 启动 |
| `Port 8081 was already in use` | 源码运行，端口被占 | `ss -ltnp \| grep 8081`，杀掉旧进程；打包版用随机端口，不会撞 |
| 中文是方块 | 字体/编码异常 | 见第 7 节；确认 `jar tf ... \| grep NotoSansSC` 有输出 |
| 输入法不能输入中文 | GTK IM 模块未设置 | 见第 7 节“输入法” |
| `.deb` 安装后菜单没有图标 | 桌面缓存未刷新 | `sudo update-desktop-database`，注销重登 |
| `apt install` 报依赖缺失 | 系统是最小化安装 | 按 3.2 安装 GTK/X11/fontconfig 依赖 |
| `ERROR: only Ubuntu is supported` | 在非 Ubuntu 系统运行脚本 | 换 Ubuntu 22.04/24.04/26.04，或使用已构建好的 `.deb`/便携包 |

### 日志位置

```bash
tail -n 100 ~/.local/share/timeyourself/timeyourself.log
```

打包版如果窗口都出不来，用终端启动拿真实错误：

```bash
/opt/timeyourself/bin/TimeYourself
```

## 10. 安全说明

- 服务只监听 `127.0.0.1`，不会暴露到局域网；
- 打包版使用 `-Dserver.port=0` 自动选空闲端口，避免固定端口被其它程序占用；
- `LocalHostGuardFilter` 只接受 `localhost` / `127.0.0.1` / `::1` 的 `Host` 头，降低 DNS rebinding 风险；如确需通过主机名访问，把 `application.yml` 里的 `timeyourself.security.host-guard` 改为 `false`；
- 数据目录 `700`、数据库 `600`，同一台机器上的其它用户无法读取；
- `.deb` 安装到 `/opt`，普通用户不可写，程序不会以 root 运行；
- 不包含遥测、自动更新、外部网络请求；所有数据在本机 SQLite。

> **威胁模型**：本应用按“单机单用户”设计，没有登录/鉴权。同一台机器上的其他本地用户如果知道端口，可以通过 `127.0.0.1` 调用 API；Host guard 只能挡住浏览器里的 DNS rebinding，挡不住本机进程。多人共用一台电脑时，请使用独立的系统账户/虚拟机，或不要运行本应用。数据目录 `700`、数据库 `600` 只能防止直接读文件，不能替代鉴权。

> JDK 26 的 `jpackage` 会通过 `ldd` + `dpkg -S` 扫描随包二进制并自动生成 `Depends`；[scripts/package-ubuntu.sh](scripts/package-ubuntu.sh) 还通过 `--linux-package-deps` 显式声明了 GTK/X11/fontconfig 等 JavaFX 运行库（它们打包在 fat jar 里，`ldd` 扫不到）。最小化系统若仍报缺库，按 3.2 补齐。

## 11. 验收清单

在 Ubuntu 上装完后逐条确认：

1. `dpkg -l | grep timeyourself` 有记录，版本 `2.0.0`；
2. 应用菜单能启动，出现 JavaFX 窗口；
3. 新建任务：名称写 `数学作业：第一章`，能保存并正常显示中文；
4. 建 Category / Plan，Add Task 时自动新建不存在的分类/计划；
5. 切 List / Line，两种视图都正常；
6. 勾选完成、Rename / Reclarify / Redate / Delete 都可用；
7. 关闭窗口再打开，数据还在；
8. `~/.local/share/timeyourself/timeyourself.db` 存在，权限 `-rw-------`；
9. `~/.local/share/timeyourself/timeyourself.log` 有启动记录；
10. `sudo apt remove timeyourself` 后程序消失，数据目录仍在。

## 12. 相关文件

- [scripts/run-ubuntu.sh](scripts/run-ubuntu.sh) — Ubuntu 源码运行
- [scripts/package-ubuntu.sh](scripts/package-ubuntu.sh) — Ubuntu `.deb` + 便携包
- [.github/workflows/ubuntu.yml](.github/workflows/ubuntu.yml) — Ubuntu CI
- [src/main/java/com/timeyourself/TimeYourselfApplication.java](src/main/java/com/timeyourself/TimeYourselfApplication.java) — 平台数据目录 / 窗口 / headless
- [src/main/resources/application.yml](src/main/resources/application.yml) — 数据源、监听地址、Host guard
