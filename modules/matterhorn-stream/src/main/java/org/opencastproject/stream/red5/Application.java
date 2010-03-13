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
package org.opencastproject.stream.red5;

import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.IBandwidthConfigure;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.stream.IServerStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.support.SimpleConnectionBWConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The opencast streaming application.
 */
public class Application extends ApplicationAdapter {
  private static final Logger logger = LoggerFactory.getLogger(Application.class);
  private IScope appScope;
  private IServerStream serverStream;

  public void init() {
    logger.info(this + ".init()");
  }
  
  /** {@inheritDoc} */
  @Override
  public synchronized boolean connect(IConnection conn, IScope scope, Object[] params) {
    logger.info("red5 application: connect");
    return super.connect(conn, scope, params);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void disconnect(IConnection conn, IScope scope) {
    logger.info("red5 application: disconnect");
    super.disconnect(conn, scope);
  }
  
  /** {@inheritDoc} */
  @Override
  public synchronized boolean start(IScope scope) {
    logger.info("red5 application: start");
    return super.start(scope);
  }
  
  /** {@inheritDoc} */
  @Override
  public synchronized void stop(IScope scope) {
    logger.info("red5 application: stop");
    super.stop(scope);
  }
  
  /** {@inheritDoc} */
  @Override
  public synchronized boolean join(IClient client, IScope scope) {
    logger.info("red5 application: join");
    return super.join(client, scope);
  }
  
  /** {@inheritDoc} */
  @Override
  public synchronized void leave(IClient client, IScope scope) {
    logger.info("red5 application: leave");
    super.leave(client, scope);
  }

  /** {@inheritDoc} */
  @Override
  public boolean appStart(IScope app) {
    log.info("oflaDemo appStart");
    System.out.println("oflaDemo appStart");      
    appScope = app;
    return true;
  }

/** {@inheritDoc} */
  @Override
  public boolean appConnect(IConnection conn, Object[] params) {
    log.info("oflaDemo appConnect");
    // Trigger calling of "onBWDone", required for some FLV players
    measureBandwidth(conn);
    if (conn instanceof IStreamCapableConnection) {
      IStreamCapableConnection streamConn = (IStreamCapableConnection) conn;
      SimpleConnectionBWConfig bwConfig = new SimpleConnectionBWConfig();
      bwConfig.getChannelBandwidth()[IBandwidthConfigure.OVERALL_CHANNEL] =
        1024 * 1024;
      bwConfig.getChannelInitialBurst()[IBandwidthConfigure.OVERALL_CHANNEL] =
        128 * 1024;
      streamConn.setBandwidthConfigure(bwConfig);
    }
    return super.appConnect(conn, params);
  }
  /** {@inheritDoc} */
  @Override
  public void appDisconnect(IConnection conn) {
    log.info("oflaDemo appDisconnect");
    if (appScope == conn.getScope() && serverStream != null) {
      serverStream.close();
    }
    super.appDisconnect(conn);
  }
}
