package com.woven.app.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


public class PasswordGenerator {

    public static void main(String[] args) {

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String rawPassword = "T0p5ecr3t!";

        String hashed = encoder.encode(rawPassword);

        System.out.println("Raw password: " + rawPassword);
        System.out.println("BCrypt hash: " + hashed);
    }

}
