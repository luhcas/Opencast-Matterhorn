package org.opencastproject;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;

public class OpencastJmsServer {
  protected ActiveMQConnectionFactory connectionFactory;
  protected BrokerService broker;

  protected final String classPathToConfigFile;
  protected final String connectionUri;

  public OpencastJmsServer(String classPathToConfigFile, String connectionUri) {
    this.classPathToConfigFile = classPathToConfigFile;
    this.connectionUri = connectionUri;
  }

  public void init() {
    try {
      broker = BrokerFactory.createBroker("xbean:"
          + classPathToConfigFile);
      broker.start();
      connectionFactory = new ActiveMQConnectionFactory(connectionUri);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void destroy() {
    if (broker != null) {
      try {
        broker.stop();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public Connection getConnection(String clientId) throws JMSException {
    Connection conn = connectionFactory.createConnection();
    conn.setClientID(clientId);
    return conn;
  }
}
