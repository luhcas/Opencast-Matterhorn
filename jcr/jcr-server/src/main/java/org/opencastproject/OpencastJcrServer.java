package org.opencastproject;

import javax.jcr.Repository;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class OpencastJcrServer {
	private ClassPathXmlApplicationContext appContext;
	
	public OpencastJcrServer() {
		appContext = new ClassPathXmlApplicationContext("classpath:/spring-cluster-repository.xml");
	}
	
	public void closeRepository() {
		appContext.destroy();
	}
	
	public Repository getRepository() {
		return (Repository) appContext.getBean("jcrRepository");
	}

}
