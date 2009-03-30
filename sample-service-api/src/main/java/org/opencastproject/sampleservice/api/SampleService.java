package org.opencastproject.sampleservice.api;

public interface SampleService {
  public String getSomething(String path);

  public void setSomething(String path, String content);
}
