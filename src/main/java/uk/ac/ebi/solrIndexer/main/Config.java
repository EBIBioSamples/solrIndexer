package uk.ac.ebi.solrIndexer.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import uk.ac.ebi.solrIndexer.threads.GroupRepoCallable;
import uk.ac.ebi.solrIndexer.threads.SampleRepoCallable;



@SpringBootApplication
//check the packages here for components
@ComponentScan(basePackages = { "uk.ac.ebi.solrIndexer" })
//turn on advanced proxy object creation so the multi-threading and repositories work
@EnableAspectJAutoProxy(proxyTargetClass = true) 
public class Config {
	
	//this is needed to read nonstrings from propertis files
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertyConfigIn() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	//needed to inject callables
	@Bean
	public static GroupRepoCallable getGroupRepoCallable() {
		return new GroupRepoCallable();		
	}

	//needed to inject callables
	@Bean
	public static SampleRepoCallable getSampleRepoCallable() {
		return new SampleRepoCallable();		
	}
	

	public static void main(String[] args) {
		SpringApplication.run(Config.class, args);
	}

}

