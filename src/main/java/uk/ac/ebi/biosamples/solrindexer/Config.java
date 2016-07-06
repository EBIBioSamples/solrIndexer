package uk.ac.ebi.biosamples.solrindexer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;



@SpringBootApplication
//turn on advanced proxy object creation so the multi-threading and repositories work
@EnableAspectJAutoProxy(proxyTargetClass = true) 
public class Config {
		
	//this is needed to read nonstrings from properties files
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertyConfigIn() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	public static void main(String[] args) {
		SpringApplication.run(Config.class, args);
	}

}

