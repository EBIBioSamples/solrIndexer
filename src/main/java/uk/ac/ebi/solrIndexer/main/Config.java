package uk.ac.ebi.solrIndexer.main;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import uk.ac.ebi.solrIndexer.threads.GroupRepoCallable;



@SpringBootApplication
//need to check for hibernate model classes in various packages
@EntityScan(basePackages = { "uk.ac.ebi.fg.biosd.model", "uk.ac.ebi.fg.core_model" })
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

	@Bean
	public static GroupRepoCallable getGroupRepoCallable() {
		return new GroupRepoCallable();		
	}

	@Bean
	public static GroupRepoCallable getGroupRepoCallable(int pageStart, int pageSize, ConcurrentUpdateSolrClient client) {
		return new GroupRepoCallable(pageStart, pageSize, client);		
	}
	

	public static void main(String[] args) {
		SpringApplication.run(Config.class, args);
	}

}

