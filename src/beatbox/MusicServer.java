package beatbox;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;

public class MusicServer {
	
	ArrayList<ObjectOutputStream> clientOutputStreams;
	
	public static void main (String[] args) {
		new MusicServer().serverGUI();
		new MusicServer().go();
	}
	public  void serverGUI(){
		JFrame servFrame = new JFrame("Artem's BeatBox Server");
		JPanel servPanel = new JPanel(new BorderLayout());
		JLabel servLabel = new JLabel("            Server is Running        ");
		servLabel.setFont(new Font("", Font.BOLD,20));
		servPanel.add(servLabel,BorderLayout.CENTER);
		servFrame.getContentPane().add(servPanel);
		servFrame.setBounds(900,400,50,50);
		servFrame.pack();
		servFrame.setVisible(true);
		servFrame.setResizable(false);
		servFrame.setIconImage(new ImageIcon(new MusicServer().getClass().getResource("/serv.png")).getImage());
		servFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	public class ClientHandler implements Runnable {
		
		ObjectInputStream in;
		Socket clientSocket;
		
		public ClientHandler(Socket socket) {
			try {
				clientSocket = socket;
				in = new ObjectInputStream(clientSocket.getInputStream());
				
			} catch(Exception ex) {ex.printStackTrace();}
		}
		
		public void run() {
			Object o2 = null;
			Object o1 = null;
			try {
				while ((o1 = in.readObject()) != null) {
					
					o2 = in.readObject();
					
					System.out.println("read two objects");
					tellEveryone(o1, o2);
				}
			} catch(Exception ex) {ex.printStackTrace();}
		}
	}
	public void go() {
		clientOutputStreams = new ArrayList<ObjectOutputStream>();
		
		try {
			ServerSocket serverSock = new ServerSocket(4242);
			
			while(true) {
				Socket clientSocket = serverSock.accept();
				ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
				clientOutputStreams.add(out);
				
				Thread t = new Thread(new ClientHandler(clientSocket));
				t.start();
				
				System.out.println("got a connection");
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void tellEveryone(Object one, Object two) {
		Iterator it = clientOutputStreams.iterator();
		while(it.hasNext()) {
			try {
				ObjectOutputStream out = (ObjectOutputStream) it.next();
				out.writeObject(one);
				out.writeObject(two);
			} catch(Exception ex) {ex.printStackTrace();}
		}
	}

}