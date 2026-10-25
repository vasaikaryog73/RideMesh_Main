package com.project.ridemesh.config;

import com.project.ridemesh.entities.User;
import com.project.ridemesh.entities.enums.Roles;
import com.project.ridemesh.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByEmail("admin@ridemesh.com").isEmpty()) {
            User admin = new User();
            admin.setName("System Admin");
            admin.setEmail("admin@ridemesh.com");
            admin.setPassword(passwordEncoder.encode("AdminPass123"));
            admin.setUsername("admin#00001");
            admin.setProfilePictureUrl("https://res.cloudinary.com/domckasfk/image/upload/v1774786542/mediscan/reports/ygnjdwtvacvw6rrzmumc.jpg");
            admin.setAuthProvider(com.project.ridemesh.entities.enums.AuthProvider.EMAIL);
            admin.setRoles(Set.of(Roles.ADMIN));
            admin.setEmailVerified(true);
            userRepository.save(admin);
            System.out.println("Seeded System Admin: admin@ridemesh.com / AdminPass123");
        }
    }
}
