package ua.foxminded.university;

import org.springframework.boot.SpringApplication;

public class TestUniversityWebAppApplication {

	public static void main(String[] args) {
		SpringApplication.from(UniversityWebAppApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
