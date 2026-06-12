package pl.jakubmikolajczyk.monitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TransactionMonitoringApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransactionMonitoringApplication.class, args);
	}

}
