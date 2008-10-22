package org.opencastproject;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

public class SampleJcrWriter {

	public void writeNodes() {
		OpencastJcrServer server = new OpencastJcrServer();
		Repository repo = server.getRepository();
		try {
			Session s = repo.login(new SimpleCredentials("username", "password".toCharArray()));
			Node currentParent = s.getRootNode();
			int i=0;
			while(i++ < 5) {
				Node currentNode = addChild(currentParent);
				currentNode.setProperty("count", new Integer(i).toString());
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
	
	protected Node addChild(Node node) throws Exception {
		return node.addNode("child");
	}
}
