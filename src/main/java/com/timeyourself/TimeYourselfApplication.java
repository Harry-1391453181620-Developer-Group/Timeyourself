package com.timeyourself;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Locale;

@SpringBootApplication
public class TimeYourselfApplication {

    static void main() {
        Path dataDir = prepareDataDir(resolveDataDir());
        System.setProperty("timeyourself.data-dir", dataDir.toString());
        System.out.println("TimeYourself data directory: " + dataDir);

        ConfigurableApplicationContext context =
                SpringApplication.run(TimeYourselfApplication.class);

        /* SQLite 在第一次连接时创建库文件；启动完成后收紧为仅当前用户可读写 */
        restrictToOwner(dataDir.resolve("timeyourself.db"));

        /* macOS 的 Cmd+Q 不一定会触发窗口关闭事件，用关闭钩子保证 Tomcat 被停掉 */
        Runtime.getRuntime().addShutdownHook(
                new Thread(context::close, "timeyourself-shutdown"));

        int port = resolvePort(context);
        String url = "http://127.0.0.1:" + port + "/";

        if (isHeadless()) {
            System.out.println("TimeYourself UI disabled (headless); listening on " + url);
            return;
        }

        openWindow(context, url);
    }

    /**
     * 数据目录：
     * - 显式 -Dtimeyourself.data-dir=... 或 TIMEYOURSELF_DATA_DIR 优先；
     * - macOS 用 ~/Library/Application Support/TimeYourself（.app 所在目录通常不可写）；
     * - Ubuntu 用 XDG 数据目录（默认 ~/.local/share/timeyourself）；
     *   .deb 安装到 /opt 后普通用户不可写，因此不能沿用 ./data；
     * - Windows 仍用当前目录下的 data/，保持现有安装包行为。
     */
    private static Path resolveDataDir() {
        String override = System.getProperty("timeyourself.data-dir");
        if (override == null || override.isBlank()) {
            override = System.getenv("TIMEYOURSELF_DATA_DIR");
        }
        if (override != null && !override.isBlank()) {
            return Path.of(override).toAbsolutePath().normalize();
        }
        if (isMac()) {
            return Path.of(System.getProperty("user.home"),
                    "Library", "Application Support", "TimeYourself");
        }
        if (isLinux()) {
            return ubuntuXdgDataHome().resolve("timeyourself");
        }
        return Path.of("data").toAbsolutePath().normalize();
    }

    /**
     * 按 XDG Base Directory 规范解析数据目录。
     * XDG_DATA_HOME 必须是绝对路径；相对路径按规范视为未设置。
     */
    private static Path ubuntuXdgDataHome() {
        String configured = System.getenv("XDG_DATA_HOME");
        if (configured != null && !configured.isBlank()) {
            Path candidate = Path.of(configured);
            if (candidate.isAbsolute()) {
                return candidate.normalize();
            }
        }
        return Path.of(System.getProperty("user.home"), ".local", "share");
    }

    private static Path prepareDataDir(Path dataDir) {
        try {
            Files.createDirectories(dataDir);
            restrictToOwner(dataDir);
            return dataDir;
        } catch (IOException error) {
            /* 极端情况下（目录被占用/无权限）退到用户主目录，至少让程序能起来 */
            System.err.println("无法创建数据目录 " + dataDir + "：" + error);
            Path fallback = Path.of(System.getProperty("user.home"), ".timeyourself");
            try {
                Files.createDirectories(fallback);
                restrictToOwner(fallback);
                return fallback;
            } catch (IOException ignored) {
                System.err.println("备用数据目录也无法创建：" + fallback);
                return dataDir;
            }
        }
    }

    /**
     * 任务数据属于个人隐私：在 macOS/Ubuntu 上尽量收紧为仅当前用户可访问。
     * Windows 没有 POSIX 权限，跳过。
     */
    private static void restrictToOwner(Path path) {
        if (isWindows() || !Files.exists(path)) {
            return;
        }
        try {
            String permissions = Files.isDirectory(path) ? "rwx------" : "rw-------";
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions));
        } catch (IOException | UnsupportedOperationException | SecurityException ignored) {
            /* 文件系统不支持 POSIX 权限（例如挂载的 FAT/exFAT）时保持默认行为 */
        }
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("mac");
    }

    private static boolean isLinux() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("linux");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("windows");
    }

    /** CI/服务器冒烟测试用：-Dtimeyourself.headless=true 或 TIMEYOURSELF_HEADLESS=true */
    private static boolean isHeadless() {
        String override = System.getProperty("timeyourself.headless");
        if (override == null || override.isBlank()) {
            override = System.getenv("TIMEYOURSELF_HEADLESS");
        }
        return override != null && Boolean.parseBoolean(override.trim());
    }

    private static int resolvePort(ConfigurableApplicationContext context) {
        Integer actual = context.getEnvironment()
                .getProperty("local.server.port", Integer.class);
        if (actual != null && actual > 0) {
            return actual;
        }
        return context.getEnvironment().getProperty("server.port", Integer.class, 8081);
    }

    private static void openWindow(ConfigurableApplicationContext context, String url) {
        Runnable show = () -> {
            WebView browser = new WebView();
            browser.setContextMenuEnabled(false);
            browser.getEngine().load(url);

            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            double maxWidth = Math.max(800, bounds.getWidth() * 0.92);
            double maxHeight = Math.max(600, bounds.getHeight() * 0.92);
            double width = Math.min(1500, maxWidth);
            double height = Math.min(950, maxHeight);

            Stage stage = new Stage();
            stage.setTitle("TimeYourself");
            stage.setScene(new Scene(browser, width, height));
            stage.setMinWidth(Math.min(1024, width));
            stage.setMinHeight(Math.min(700, height));
            stage.centerOnScreen();

            stage.setOnCloseRequest(event -> {
                Platform.exit();
                context.close();
                System.exit(0);
            });

            for (int size : new int[]{16, 32, 48, 64, 128, 256}) {
                try (var stream = TimeYourselfApplication.class
                        .getResourceAsStream("/static/images/appLogo-" + size + ".png")) {
                    if (stream != null) {
                        stage.getIcons().add(new javafx.scene.image.Image(stream));
                    }
                } catch (Exception ignored) {
                    /* 某一档图标缺失不影响运行 */
                }
            }
            stage.show();
        };

        try {
            Platform.startup(show);
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(show);
        }
    }
}
