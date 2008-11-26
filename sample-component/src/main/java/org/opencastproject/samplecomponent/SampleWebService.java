package org.opencastproject.samplecomponent;

import javax.jws.WebParam;
import javax.jws.WebService;

@WebService
public interface SampleWebService {
	public String getSomething(@WebParam(name="path") String path);
	public void setSomething(@WebParam(name="path") String path, @WebParam(name="content") String content);
}
