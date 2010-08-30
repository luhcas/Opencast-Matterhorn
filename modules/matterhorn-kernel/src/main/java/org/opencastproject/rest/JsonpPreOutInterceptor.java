/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.rest;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * Appends json padding if requested
 */
public class JsonpPreOutInterceptor extends AbstractPhaseInterceptor<Message> {

  /** the logger */
  private static final Logger logger = LoggerFactory.getLogger(JsonpPreOutInterceptor.class);

  public JsonpPreOutInterceptor() {
    super(Phase.PRE_STREAM);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.cxf.interceptor.Interceptor#handleMessage(org.apache.cxf.message.Message)
   */
  @Override
  public void handleMessage(Message message) throws Fault {
    Exchange exchange = message.getExchange();
    String callbackValue = (String) exchange.get(JsonpInInterceptor.CALLBACK_KEY);
    if (!StringUtils.isEmpty(callbackValue)) {
      logger.debug("Appending callback padding '{}' to message {}", callbackValue, message);
      HttpServletResponse response = (HttpServletResponse) message.get("HTTP.RESPONSE");
      try {
        response.getOutputStream().write((callbackValue + "(").getBytes("UTF-8"));
      } catch (IOException e) {
        throw new Fault(e);
      }
    }
  }

}
