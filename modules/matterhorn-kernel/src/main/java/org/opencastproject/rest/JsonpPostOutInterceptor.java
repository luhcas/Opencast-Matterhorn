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
import javax.ws.rs.core.MediaType;

/**
 * Appends json padding if requested
 */
public class JsonpPostOutInterceptor extends AbstractPhaseInterceptor<Message> {

  /** the logger */
  private static final Logger logger = LoggerFactory.getLogger(JsonpPostOutInterceptor.class);

  public JsonpPostOutInterceptor() {
    super(Phase.POST_STREAM);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.cxf.interceptor.Interceptor#handleMessage(org.apache.cxf.message.Message)
   */
  @Override
  public void handleMessage(Message message) throws Fault {
    String contentType = (String) message.get(Message.CONTENT_TYPE);
    if (MediaType.APPLICATION_JSON.equals(contentType)) {
      Exchange exchange = message.getExchange();
      String callbackValue = (String) exchange.get(JsonpInInterceptor.CALLBACK_KEY);
      if (!StringUtils.isEmpty(callbackValue)) {
        logger.debug("Finishing callback padding to message {}", callbackValue, message);
        HttpServletResponse response = (HttpServletResponse) message.get("HTTP.RESPONSE");
        try {
          response.getOutputStream().write((")").getBytes("UTF-8"));
        } catch (IOException e) {
          throw new Fault(e);
        }
      }
    }
  }

}
