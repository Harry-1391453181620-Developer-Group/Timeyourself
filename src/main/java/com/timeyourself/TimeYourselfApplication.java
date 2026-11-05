package com.timeyourself;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class TimeYourselfApplication {

    static void main() {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("data"));
        } catch (java.io.IOException error) {
            System.err.println("无法创建 data 目录：" + error);
        }

        ConfigurableApplicationContext context =
                SpringApplication.run(TimeYourselfApplication.class);

        String port = context.getEnvironment().getProperty("server.port", "8081");
        openWindow(context, "http://localhost:" + port + "/");
    }

    private static void openWindow(ConfigurableApplicationContext context, String url) {
        Runnable show = () -> {
            WebView browser = new WebView();
            browser.setContextMenuEnabled(false);
            browser.getEngine().load(url);

            Stage stage = new Stage();
            stage.setTitle("TimeYourself");
            stage.setScene(new Scene(browser, 1500, 950));
            stage.setOnCloseRequest(event -> {
                context.close();
                Platform.exit();
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