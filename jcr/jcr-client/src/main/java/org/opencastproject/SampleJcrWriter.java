package org.opencastproject;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleJcrWriter {
	private Logger logger = LoggerFactory.getLogger(SampleJcrWriter.class);
	public static void main(String... args) {
		SampleJcrWriter writer = new SampleJcrWriter();
		writer.writeNodes();
	}
	
	public void writeNodes() {
		OpencastJcrServer server = new OpencastJcrServer();
		Repository repo = server.getRepository();
		try {
			Session s = repo.login(new SimpleCredentials("username", "password".toCharArray()));
			Node currentParent = s.getRootNode();
			int i=0;
			while(i++ < 5) {
				String suffix = new Integer(i).toString();
				Node currentNode = addChild(currentParent, suffix);
				logger.info("added node " + currentNode.getName() + " to parent " + currentParent.getName());
				currentParent.save();
				s.save();
				currentParent = currentNode;
				Thread.sleep(5000);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			server.closeRepository();
		}
	}
	
	protected Node addChild(Node node, String suffix) throws Exception {
		return node.addNode("child_" + suffix);
	}
}
