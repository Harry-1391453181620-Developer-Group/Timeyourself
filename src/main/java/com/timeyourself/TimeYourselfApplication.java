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

/**
 * Spring Boot entry point that prepares the private data directory, starts the embedded web server and, unless
 * headless mode is requested, presents the user interface inside a JavaFX window.
 */
@SpringBootApplication
public class TimeYourselfApplication {

    /**
     * Boots the application: resolves and prepares the directory that stores the SQLite database, publishes it
     * through the {@code timeyourself.data-dir} system property, starts the Spring context and restricts the
     * database file to the current user. A JVM shutdown hook closes the context afterwards, so the server is
     * stopped even when the platform does not report a window close. The desktop window is opened last; in
     * headless mode the method only reports the listening address and leaves the embedded server running for
     * smoke tests. Data directory failures are absorbed by {@link #prepareDataDir(Path)}, and an already
     * running JavaFX platform is handled through {@link Platform#runLater(Runnable)}.
     */
    static void main() {
        Path dataDir = prepareDataDir(resolveDataDir());
        System.setProperty("timeyourself.data-dir", dataDir.toString());
        System.out.println("TimeYourself data directory: " + dataDir);

        ConfigurableApplicationContext context =
                SpringApplication.run(TimeYourselfApplication.class);

        restrictToOwner(dataDir.resolve("timeyourself.db"));

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
     * Resolves the directory that stores the SQLite database. An explicit {@code -Dtimeyourself.data-dir}
     * system property takes precedence over the {@code TIMEYOURSELF_DATA_DIR} environment variable, and both
     * are converted into absolute normalized paths. Without an override, macOS uses the application support
     * directory inside the user home, Linux uses the XDG data home below {@code ~/.local/share}, and every other
     * platform keeps the {@code data} directory of the working directory.
     *
     * @return absolute normalized path of the data directory, which is not created by this method
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
     * Resolves the XDG data home directory and ignores a relative {@code XDG_DATA_HOME} value, which the
     * specification treats as unset.
     *
     * @return normalized absolute XDG data home, or {@code ~/.local/share} when the variable is unset, blank or
     *         relative
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

    /**
     * Creates the data directory together with any missing parents and restricts it to the current user, which
     * makes the returned directory usable for the database. When creation fails, for example because the path
     * is occupied or not writable, the method retries with the {@code .timeyourself} directory inside the user
     * home; when both attempts fail it reports the problem on standard error and returns the requested path so
     * that startup can continue.
     *
     * @param dataDir directory that should hold the database and application data
     * @return the created directory, or the requested path when no directory could be created and secured
     */
    private static Path prepareDataDir(Path dataDir) {
        try {
            Files.createDirectories(dataDir);
            restrictToOwner(dataDir);
            return dataDir;
        } catch (IOException error) {
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
     * Restricts a file or directory to the current user by applying POSIX permissions, using {@code rwx------}
     * for directories and {@code rw-------} for files. The method is a no-op on Windows, for paths that do not
     * exist and for file systems without POSIX support, where the failure is intentionally swallowed so that
     * the application still runs from media such as FAT or exFAT mounts.
     *
     * @param path file or directory whose permissions should be tightened
     */
    private static void restrictToOwner(Path path) {
        if (isWindows() || !Files.exists(path)) {
            return;
        }
        try {
            String permissions = Files.isDirectory(path) ? "rwx------" : "rw-------";
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions));
        } catch (IOException | UnsupportedOperationException | SecurityException ignored) {
        }
    }

    /**
     * Tests whether the application runs on macOS.
     *
     * @return {@code true} when the {@code os.name} system property contains {@code mac}, ignoring case
     */
    private static boolean isMac() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("mac");
    }

    /**
     * Tests whether the application runs on Linux.
     *
     * @return {@code true} when the {@code os.name} system property contains {@code linux}, ignoring case
     */
    private static boolean isLinux() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("linux");
    }

    /**
     * Tests whether the application runs on Windows.
     *
     * @return {@code true} when the {@code os.name} system property contains {@code windows}, ignoring case
     */
    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("windows");
    }

    /**
     * Reads the headless switch that disables the desktop window for CI runs and server smoke tests.
     *
     * @return {@code true} when the {@code timeyourself.headless} system property or the
     *         {@code TIMEYOURSELF_HEADLESS} environment variable parses as {@code true}
     */
    private static boolean isHeadless() {
        String override = System.getProperty("timeyourself.headless");
        if (override == null || override.isBlank()) {
            override = System.getenv("TIMEYOURSELF_HEADLESS");
        }
        return override != null && Boolean.parseBoolean(override.trim());
    }

    /**
     * Determines the port the embedded server is actually listening on, falling back to the configured
     * {@code server.port} and finally to {@code 8081}.
     *
     * @param context running application context exposing the server environment
     * @return positive port number used to build the local user interface address
     */
    private static int resolvePort(ConfigurableApplicationContext context) {
        Integer actual = context.getEnvironment()
                .getProperty("local.server.port", Integer.class);
        if (actual != null && actual > 0) {
            return actual;
        }
        return context.getEnvironment().getProperty("server.port", Integer.class, 8081);
    }

    /**
     * Opens the main window on the JavaFX application thread and loads the given address into an embedded
     * {@link WebView}. The window is sized relative to the visible bounds of the primary screen, centred,
     * decorated with every bundled application icon that can be read and exits the JVM when the user closes it,
     * closing the application context first.
     *
     * @param context application context closed together with the window
     * @param url     address of the locally running user interface
     */
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
