package org.opencastproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CommandLineJmsInterface {
  private static final Log log = LogFactory
      .getLog(CommandLineJmsInterface.class);

  public static final String TOPIC_ID = "the_test_topic";
  public static final String SUBSCRIBER_ID = "test_subscriber";

  public static void main(String... args) {
    try {
      System.out.print("Enter a unique name for this JMS client: ");
      String nodeNumberInput;
      try {
        nodeNumberInput = new BufferedReader(new InputStreamReader(System.in))
            .readLine();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext(
          "classpath:opencast-jms-server.xml");
      OpencastJmsServer server = (OpencastJmsServer) ac.getBean("jmsServer");

      Connection conn = server.getConnection("CommandLineJmsInterface_"
          + nodeNumberInput);
      Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Topic topic = session.createTopic(TOPIC_ID);
      conn.start();

      MessageConsumer subscriber = null;

      try {
        System.out
            .println("Type 'send <message>' to send a message to a pre-defined topic.");
        System.out
            .println("Type 'subscribe' to display messages sent to the topic.");
        System.out
            .println("Type 'unsubscribe' to stop displaying messages sent to the topic.");
        System.out.println("Type 'quit' to exit.");
        while (true) {
          System.out.print("> ");
          BufferedReader br = new BufferedReader(new InputStreamReader(
              System.in));
          String input;
          try {
            input = br.readLine();
          } catch (IOException e) {
            e.printStackTrace();
            break;
          }
          if (input == null)
            continue;

          if ("quit".equals(input)) {
            break;
          } else if (input.startsWith("send")) {
            String messageText = input.substring(input.indexOf(" ") + 1);
            try {
              MessageProducer producer = session.createProducer(topic);
              TextMessage message = session.createTextMessage(messageText);
              message.setJMSDestination(topic);
              producer.send(message);
            } catch (Exception e) {
              System.out.println(e.getMessage());
            }
          } else if (input.startsWith("subscribe")) {
            if (subscriber == null) {
              subscriber = session
                  .createDurableSubscriber(topic, SUBSCRIBER_ID);
              subscriber.setMessageListener(new MessageListener() {
                public void onMessage(Message inboundMessage) {
                  System.out.println(inboundMessage);
                }
              });
            }
          } else if (input.startsWith("unsubscribe")) {
            if (subscriber != null) {
              subscriber.close();
              session.unsubscribe(SUBSCRIBER_ID);
              subscriber = null;
            }
          } else {
            System.out.println("Unknown command: " + input);
          }
        }
      } finally {
        session.close();
        conn.close();
        server.destroy();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
