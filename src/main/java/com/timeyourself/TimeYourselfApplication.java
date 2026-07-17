package com.timeyourself;

import com.formdev.flatlaf.FlatLightLaf;
import com.timeyourself.view.MainFrame;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class TimeYourselfApplication {
    static void main() {
        FlatLightLaf.setup();
        ConfigurableApplicationContext context =
                SpringApplication.run(
                        TimeYourselfApplication.class
                );
        MainFrame mainFrame =
                context.getBean(MainFrame.class);
        mainFrame.show();
    }
}
