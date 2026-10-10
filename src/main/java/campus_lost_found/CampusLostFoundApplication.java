package campus_lost_found;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import campus_lost_found.model.Admin;
import campus_lost_found.repository.AdminRepository;

@SpringBootApplication
public class CampusLostFoundApplication {

	public static void main(String[] args) {
		SpringApplication.run(CampusLostFoundApplication.class, args);
	}

	@Bean
	public CommandLineRunner initAdmin(AdminRepository adminRepository) {
		return args -> {
			if (adminRepository.count() == 0) {
				adminRepository.save(new Admin("admin", "admin123"));
				System.out.println("Default admin user created in MySQL: username=admin, password=admin123");
			}
		};
	}

}
