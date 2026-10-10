package vn.yain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BadmintonManagementProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(BadmintonManagementProjectApplication.class, args);
	}

}
