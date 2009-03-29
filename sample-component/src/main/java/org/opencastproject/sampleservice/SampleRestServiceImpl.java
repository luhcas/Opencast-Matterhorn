package org.opencastproject.sampleservice;

import javax.ws.rs.QueryParam;

public class SampleRestServiceImpl implements SampleRestService {
	public String getSomething(@QueryParam("path") String path) {
		return "sample";
	}
}
