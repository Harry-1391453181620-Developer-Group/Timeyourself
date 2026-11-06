# TimeYourself on macOS

本文是 macOS 适配、中文乱码修复和 `.dmg` 打包的完整说明。

> **先说明一个硬限制**：`jpackage` 不能交叉编译。Windows 上可以编译出 jar，但生成 macOS 的 `.app` / `.dmg` 必须在 Mac 上执行。项目已经把 Mac 需要的代码、字体、图标和脚本都准备好了；在 Mac 上跑 `scripts/package-macos.sh` 即可产出安装包。

---

## 1. 环境要求

| 项目 | 要求 |
|---|---|
| macOS | 11 或更高（建议 macOS 13+） |
| JDK | **26**（Apple Silicon 用 arm64 包，Intel 用 x64 包） |
| Maven | 3.9+ |
| Xcode 命令行工具 | 生成 `.dmg` 不需要；如果要做签名/公证才需要 |

检查：

```bash
java -version
mvn -version
uname -m          # arm64 = Apple Silicon, x86_64 = Intel
```

如果 `java -version` 不是 26：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 26)
```

把上面这行写进 `~/.zshrc` 可以永久生效。

---

## 2. 直接跑源码

```bash
cd /path/to/Time_Yourself
chmod +x scripts/run-macos.sh
./scripts/run-macos.sh
```

等价于：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 26)
mvn spring-boot:run
```

启动后：

- 后端监听 `http://127.0.0.1:8081`；
- JavaFX 窗口自动打开；
- 数据目录（macOS 默认）：

```text
~/Library/Application Support/TimeYourself/
├── timeyourself.db
└── timeyourself.log
```

如果开发时想继续用项目里的 `data/`：

```bash
TIMEYOURSELF_DATA_DIR="$PWD/data" ./scripts/run-macos.sh
```

> macOS 上 `.app` 的当前工作目录通常是 `/`，把数据库放在 `.app` 旁边会写不进去，所以默认改到 `Application Support`。Windows 版本仍然使用程序目录下的 `data/`。

---

## 3. 打 macOS 安装包（.dmg）

```bash
cd /path/to/Time_Yourself
chmod +x scripts/package-macos.sh
./scripts/package-macos.sh
```

脚本会：

1. `mvn -DskipTests clean package`；
2. 从 `src/main/resources/static/images/appLogo-256.png` 生成 `.icns`；
3. 用 `jpackage --type app-image` 生成 `TimeYourself.app`；
4. 用 ad-hoc 签名保证 bundle 结构完整；
5. 用 `hdiutil` 生成带 `Applications` 快捷方式的 DMG。

产物：

```text
build/macos/TimeYourself-2.0.0-arm64.dmg     # Apple Silicon
build/macos/TimeYourself-2.0.0-x86_64.dmg    # Intel
```

安装：双击 DMG，把 `TimeYourself` 拖进 `Applications`。

### 3.1 为什么不用 `jpackage --type dmg`

`jpackage --type dmg` 也能直接出 DMG，但那样**无法在生成 DMG 前对 `.app` 签名**。脚本采用 `app-image → codesign → hdiutil` 的顺序，结构更接近正常 Mac 软件。

### 3.2 Apple Silicon / Intel

`jpackage` 只能生成**当前机器架构**的包。Apple Silicon 上打出来的是 arm64，Intel 上打出来的是 x86_64。要同时提供两种，需要分别在两种 Mac 上执行脚本；脚本会自动把架构写进 DMG 文件名。

### 3.3 没有 Mac？用 GitHub Actions 出包

仓库里已经放了 `.github/workflows/macos.yml`。把它推到 GitHub 后：

1. 打开仓库的 **Actions** 页；
2. 左侧选 **Build macOS DMG**；
3. 点 **Run workflow**；
4. 跑完后在该次运行页面底部下载 `TimeYourself-macOS-arm64` 和 `TimeYourself-macOS-x86_64` 两个 zip，里面就是 `.dmg`。

工作流用两个 runner：

- `macos-15`：Apple Silicon；
- `macos-15-intel`：Intel。

构建完成后它会强制检查两件事，任何一项失败都不会上传 artifact：

1. `codesign --verify --strict` 通过；
2. 主启动器带有 `com.apple.security.cs.disable-library-validation`（解决 `libjli.dylib` 的 Team ID 报错）。所以如果 Actions 红在 **Verify app signature and libjli entitlements**，看那一步的 `codesign -d --entitlements` 输出即可。

它通过 `actions/setup-java` 安装 Temurin JDK 26。如果某天 Temurin 26 在 runner 上不可用，把 `distribution` 换成 `zulu` 即可（脚本本身不依赖具体发行版）。

> 这是目前在 Windows 上“实际拿到 Mac 安装包”的最可行方式；`jpackage` 本身不能从 Windows 交叉编译出 macOS 包。

---

## 4. 中文乱码：原因和修复

项目源码和资源本身已经是 **UTF-8 无 BOM**。Mac 上出现“乱码”通常不是文件编码，而是下面两层原因之一：

### 4.1 JavaFX 在 macOS 15+ 的中文字体回退缺陷

OpenJDK 的这个问题描述得很清楚：macOS 15 起系统字体不再以可解析的字体文件存在，JavaFX 仍按文件模型加载字体，导致中文回退到**日文字体**，字形明显不对：

- [JDK-8374013: [macOS] Chinese fonts fallback to Japanese fonts](https://bugs.openjdk.org/browse/JDK-8374013)

修复方式：不要再依赖系统字体。项目已经把 **Noto Sans SC**（SIL Open Font License）随应用打包：

```text
src/main/resources/static/fonts/NotoSansSC-Regular.ttf
src/main/resources/static/fonts/NotoSansSC-Bold.ttf
src/main/resources/static/fonts/OFL.txt
```

`shared.css` 开头注册了 `TimeYourself Sans`，并让所有页面元素优先使用它。这样无论 macOS 是否能加载 PingFang，中文都会用内嵌字体渲染。

### 4.2 UTF-8 全链路

已经固定了这些位置：

| 层 | 配置 |
|---|---|
| Maven 编译 | `project.build.sourceEncoding=UTF-8`、`maven.compiler.encoding=UTF-8` |
| Maven 资源复制 | `maven-resources-plugin` 的 `encoding=UTF-8` |
| Spring HTTP | `server.servlet.encoding.charset/force=true` |
| 源码直接运行 | `spring-boot:run` 的 `jvmArguments` 加 `-Dfile.encoding=UTF-8` |
| 打包运行 | `jpackage` 的 `--java-options` 加 `-Dfile.encoding/-Dstdout.encoding/-Dstderr.encoding=UTF-8` |
| HTML | 所有页面都有 `<meta charset="UTF-8">` |

### 4.3 如果仍然乱码

按顺序检查：

1. **确认字体真的进了 jar**：

   ```bash
   jar tf target/Time_Yourself-2.0.jar | grep NotoSansSC
   ```

   应该看到 `BOOT-INF/classes/static/fonts/NotoSansSC-Regular.ttf`。

2. **确认页面加载了字体**：打开开发者工具（JavaFX WebView 没有内置 devtools，可先用浏览器访问 `http://127.0.0.1:8081/css/shared.css` 和 `/fonts/NotoSansSC-Regular.ttf`，确认 200）。

3. **区分“字形不对”和“字节乱码”**：
   - 字形像日文（比如“直”“骨”“包”的写法不同）→ 字体问题，确认 `TimeYourself Sans` 生效；
   - 出现 `�`、`æ—¥`、`????` → 是编码问题，检查数据库内容是否本来就是坏的，或请求/响应编码。

4. **看日志**：

   ```bash
   tail -n 100 "$HOME/Library/Application Support/TimeYourself/timeyourself.log"
   ```

---

## 5. 数据迁移 / 备份

Windows 上的数据库在：

```text
<程序目录>\data\timeyourself.db
```

复制到 Mac：

```bash
mkdir -p "$HOME/Library/Application Support/TimeYourself"
cp /path/to/timeyourself.db "$HOME/Library/Application Support/TimeYourself/"
```

SQLite 文件是跨平台的，直接复制即可。备份也只需要复制这一个文件。

如果数据库是旧版（没有 `category_id` / `plan_id` 等列），不要直接覆盖；先备份，再按 `src/main/resources/db/schema.sql` 的结构迁移。

---

## 6. Gatekeeper：第一次打开被拦截

本地构建、没有开发者签名的 `.app`，从网络下载后第一次打开可能提示“无法验证开发者”。

临时解决（自己用）：

1. 在 Finder 里右键 `TimeYourself.app` → **打开** → 再点 **打开**；
2. 或：

   ```bash
   xattr -dr com.apple.quarantine /Applications/TimeYourself.app
   ```

正式分发给别人，建议使用 Apple Developer ID 签名并公证：

```bash
# 需要 Keychain 里有 "Developer ID Application: ..." 证书
export CODESIGN_IDENTITY="Developer ID Application: Your Name (TEAMID)"
./scripts/package-macos.sh
```

然后在生成 DMG 后：

```bash
xcrun notarytool submit build/macos/TimeYourself-2.0.0-*.dmg \
  --apple-id "you@example.com" \
  --team-id "TEAMID" \
  --password "app-specific-password" \
  --wait

xcrun stapler staple build/macos/TimeYourself-2.0.0-*.dmg
```

> 不做签名/公证也能在自己和同学的 Mac 上通过“右键打开”运行；只是第一次会多一步 Gatekeeper 提示。

---

## 7. 常见问题

| 现象 | 原因 / 处理 |
|---|---|
| `jpackage: command not found` | `JAVA_HOME` 没指向 JDK 26；执行 `export JAVA_HOME=$(/usr/libexec/java_home -v 26)` |
| `release version 26 not supported` | 用了旧 JDK；换 JDK 26 |
| `invalid target release: 26` | 同上 |
| `jpackage` 报 JavaFX 相关缺类 | 确认是在 Mac 上构建，Maven 会自动拉 `javafx-*-mac` / `-mac-aarch64`；不要手动指定 Windows classifier |
| 启动后白屏 | 先用 Terminal 运行 `.../TimeYourself.app/Contents/MacOS/TimeYourself` 看日志；检查 `timeyourself.log` |
| 双击后立刻闪退、Terminal 显示 `Killed: 9` | 签名带了 hardened runtime，而 JavaFX 原生库是运行时从 jar 解压的，被 macOS library validation 杀掉。打包脚本已去掉 `--options runtime`；不要给 ad-hoc 签名加这个参数 |
| `dlopen(.../libjli.dylib) failed ... different Team IDs` | 主启动器缺少 `com.apple.security.cs.disable-library-validation`。这是 jpackage 的 [JDK-8358723](https://bugs.openjdk.org/browse/JDK-8358723)；`scripts/TimeYourself.entitlements` 已包含该键，重新打包即可 |
| `Failed to create unpack directory` | `java.io.tmpdir` 不可写；打包脚本已固定为 `/tmp`，源码运行可加 `-Djava.io.tmpdir=/tmp` |
| `Bad CPU type in executable` | DMG 架构和机器不匹配；Apple Silicon 用 arm64 包，Intel 用 x86_64 包 |
| 端口被占用 | 打包版脚本传了 `-Dserver.port=0`，会自动选空闲端口；源码运行固定 8081，先关掉占用进程 |
| 中文仍是日文字形 | 字体没进 jar 或 CSS 没加载；按 4.3 检查 |
| 双击 app 闪退 | 用 Terminal 启动看真实错误；确认 `Contents/runtime`、`Contents/app` 完整 |
| 数据“消失” | 检查 `~/Library/Application Support/TimeYourself/timeyourself.db`，不是 `.app` 目录 |

从 Terminal 启动已安装的 app（最直观的排错方式）：

```bash
/Applications/TimeYourself.app/Contents/MacOS/TimeYourself
```

---

## 8. 改动清单（Mac 适配相关）

| 文件 | 作用 |
|---|---|
| `TimeYourselfApplication.java` | macOS 数据目录、日志、动态端口、窗口尺寸适配、关闭钩子 |
| `config/Utf8ContentTypeFilter.java` | 给 HTML/JS/CSS/JSON 明确加 `charset=UTF-8` |
| `application.yml` | UTF-8、仅监听 127.0.0.1、数据目录占位符、日志文件 |
| `pom.xml` | 编译/资源 UTF-8、`spring-boot:run` JVM 参数 |
| `shared.css` | 内嵌 Noto Sans SC + 全局字体 |
| `static/fonts/*` | 中文字体、OFL 许可证、来源说明 |
| `scripts/run-macos.sh` | Mac 源码运行 |
| `scripts/package-macos.sh` | Mac `.app` + `.dmg` 打包 |
| `appLogo.icns` / `static/images/appLogo.icns` | Mac 图标（脚本也会从 PNG 重新生成） |
| `.github/workflows/macos.yml` | 在 GitHub Actions 上同时构建 arm64 / x86_64 DMG |
| `.editorconfig` / `.gitattributes` | 统一 UTF-8、LF，保证 `.sh` 在 Mac 上可直接执行 |

Windows 的现有安装包流程不受影响：`mvn clean package` + `jpackage --type exe` 仍然可用，数据库仍在程序目录的 `data/`。
