package org.opencastproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

public class CommandLineJcrInterface {
	
	public static void main(String... args) {
		try {
			System.out.print("Enter a unique name for this node: ");
			String nodeNumberInput;
			try {
				nodeNumberInput = new BufferedReader(new InputStreamReader(System.in)).readLine();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			OpencastJcrServer server = new OpencastJcrServer(nodeNumberInput, "./target/cluster_repo_" + nodeNumberInput + "_home/");
			try {
				Repository repo = server.getRepository();
				Session s = repo.login(new SimpleCredentials("username", "password".toCharArray()));
				System.out.println("Type 'write <node path>' to add a node.");
				System.out.println("Type 'read <node path>' to display a node.");
				System.out.println("Type 'quit' to exit.");
				while(true) {
					System.out.print("> ");
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
					String input;
					try {
						input = br.readLine();
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
					if(input == null) continue;
					
					if("quit".equals(input)) {
						break;
					} else if(input.startsWith("write")) {
						// Strip off the write and whitespace
						int whiteSpaceIndex = input.indexOf(" ");
						String nodeName = input.substring(whiteSpaceIndex + 1);
						try {
							s.getRootNode().addNode(nodeName, "nt:folder");
							s.save();
						} catch (Exception e) {
							System.out.println(e.getMessage());
						}
					} else if(input.startsWith("read")) {
						int whiteSpaceIndex = input.indexOf(" ");
						String nodeName = input.substring(whiteSpaceIndex + 1);
						try {
							Node node = s.getRootNode().getNode(nodeName);
							System.out.println(node.getPath());
						} catch (Exception e) {
							System.out.println(e.getMessage());
						}
					} else {
						System.out.println("Unknown command: " + input);
					}
				}
			} finally {
				server.closeRepository();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
