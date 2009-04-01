package org.opencastproject.sampleservice;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

//@RunWith(JUnit4TestRunner.class)
public class SampleServiceTest {
  public SampleServiceTest() {
  }

  @Test
  public void testServiceExists() {
    System.out
        .println("Running testServiceExists(), which doesn't test anything");
    assertTrue(true);
  }
}
