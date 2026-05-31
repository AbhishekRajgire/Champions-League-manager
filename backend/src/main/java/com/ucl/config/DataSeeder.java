package com.ucl.config;

import com.ucl.model.Role;
import com.ucl.model.User;
import com.ucl.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-username}")
    private String adminUsername;
    @Value("${app.seed.admin-password}")
    private String adminPassword;
    @Value("${app.seed.user-username}")
    private String userUsername;
    @Value("${app.seed.user-password}")
    private String userPassword;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername(adminUsername)) {
            userRepository.save(new User(adminUsername, passwordEncoder.encode(adminPassword), Role.ADMIN));
            log.info("Seeded ADMIN account '{}'", adminUsername);
        }
        if (!userRepository.existsByUsername(userUsername)) {
            userRepository.save(new User(userUsername, passwordEncoder.encode(userPassword), Role.USER));
            log.info("Seeded USER account '{}'", userUsername);
        }
    }
}
