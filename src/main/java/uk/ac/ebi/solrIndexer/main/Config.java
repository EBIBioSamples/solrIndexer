package uk.ac.ebi.solrIndexer.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;


@SpringBootApplication
//@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
@EntityScan(basePackages = { "uk.ac.ebi.fg.biosd.model", "uk.ac.ebi.fg.core_model" })
public class Config {
	
	//this is needed to read nonstrings from propertis files
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertyConfigIn() {
		return new PropertySourcesPlaceholderConfigurer();
	}
	
	

	public static void main(String[] args) {
		SpringApplication.run(Config.class, args);
	}

}
