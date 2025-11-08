package com.example.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Locale;

@SpringBootApplication
public class SecurityOracleJwtApplication {

    public static void main(String[] args) {
        Locale.setDefault(Locale.forLanguageTag("en-US-u-nu-latn"));
        System.setProperty("user.language", "en");
        System.setProperty("user.country", "US");
        SpringApplication.run(SecurityOracleJwtApplication.class, args);

        // TODO: نقطة دخول التطبيق
    }
}
