package com.igarciamen.users.config;

import com.igarciamen.users.enums.ERole;
import com.igarciamen.users.model.Role;
import com.igarciamen.users.model.User;
import com.igarciamen.users.repository.RoleRepository;
import com.igarciamen.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("!test")
public class DataLoader {

    @Bean
    public CommandLineRunner initData(
            RoleRepository roleRepo,
            UserRepository userRepo,
            PasswordEncoder encoder,
            @Value("${app.admin.username:}") String adminUser,
            @Value("${app.admin.password:}") String adminPass
    ) {
        return args -> {

            if (roleRepo.count() == 0) {
                roleRepo.save(new Role(ERole.ROLE_ADMIN));
                roleRepo.save(new Role(ERole.ROLE_PROMOTER));
                roleRepo.save(new Role(ERole.ROLE_INVESTOR));
            }

            if (adminUser != null && !adminUser.isBlank()) {
                if (!userRepo.existsByUsername(adminUser)) {
                    String adminEmail = adminUser + "@admin.local";
                    User admin = new User(adminUser, adminEmail, encoder.encode(adminPass));
                    Role adminRole = roleRepo.findByName(ERole.ROLE_ADMIN)
                            .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN does not exist"));
                    admin.getRoles().add(adminRole);
                    userRepo.save(admin);
                }
            }
        };
    }
}
