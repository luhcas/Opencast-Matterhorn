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
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * Sets the "jsonp" query string parameter, if present, to into the message exchange so the JsonOutInterceptor can apply
 * the padding to the response.
 */
public class JsonpInInterceptor extends AbstractPhaseInterceptor<Message> {
  public static final String CALLBACK_PARAM = "jsonp";
  public static final String CALLBACK_KEY = "org.opencastproject.rest.jsonp.callback";

  /** the logger */
  private static final Logger logger = LoggerFactory.getLogger(JsonpInInterceptor.class);

  public JsonpInInterceptor() {
    super(Phase.INVOKE);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.cxf.interceptor.Interceptor#handleMessage(org.apache.cxf.message.Message)
   */
  @Override
  public void handleMessage(Message message) throws Fault {
    HttpServletRequest request = (HttpServletRequest) message.get("HTTP.REQUEST");
    String callbackValue = request.getParameter(CALLBACK_PARAM);
    if (!StringUtils.isEmpty(callbackValue)) {
      logger.debug("Message {} contains a callback value of '{}'", message, callbackValue);
      message.getExchange().put(CALLBACK_KEY, callbackValue);
    }
  }

}
