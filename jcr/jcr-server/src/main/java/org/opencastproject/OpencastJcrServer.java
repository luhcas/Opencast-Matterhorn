package org.opencastproject;

import java.io.InputStream;

import javax.jcr.Repository;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

public class OpencastJcrServer {
	JackrabbitRepository repo;
	public OpencastJcrServer(InputStream config, String repoHome) {
		try {
			RepositoryConfig rc = RepositoryConfig.create(config, repoHome);
			repo = RepositoryImpl.create(rc);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void closeRepository() {
		repo.shutdown();
	}
	
	public Repository getRepository() {
		return repo;
	}

}
