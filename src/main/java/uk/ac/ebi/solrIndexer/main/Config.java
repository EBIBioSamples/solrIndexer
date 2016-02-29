package uk.ac.ebi.solrIndexer.main;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import uk.ac.ebi.solrIndexer.threads.GroupRepoCallable;


@SpringBootApplication
//@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
@EntityScan(basePackages = { "uk.ac.ebi.fg.biosd.model", "uk.ac.ebi.fg.core_model" })
@ComponentScan(basePackages = { "uk.ac.ebi.solrIndexer" })
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
