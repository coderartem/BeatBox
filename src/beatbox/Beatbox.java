package beatbox;

import java.awt.*;
import javax.swing.*;
import java.io.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;
import java.net.*;
import javax.swing.event.*;

public class Beatbox {
	
	JFrame theFrame;
	JPanel mainPanel;
	JList incomingList;
	JTextField userMessage;
	ArrayList<JCheckBox> checkboxList;
	int nextNum;
	Vector<String> listVector = new Vector<String>();
	String userName = "Track";
	ObjectOutputStream out;
	ObjectInputStream in;
	HashMap<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();
	
	Sequencer sequencer;
	Sequence sequence;
	Sequence mySequence = null;
	Track track;
	
	String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga"};
	
	int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};
	
	public static void main(String args []) {
		new Beatbox().startUp();
	}
	
	public void startUp() {
		buildGUI();
		
		try {
			Socket sock = new Socket("127.0.0.1", 4242);
			out = new ObjectOutputStream(sock.getOutputStream());
			in = new ObjectInputStream(sock.getInputStream());
			Thread remote = new Thread(new RemoteReader());
			remote.start();
		} catch(Exception ex) {
			userMessage.setText("  If you start the Server, Restart this program");
			userMessage.setEnabled(false);
			JOptionPane.showConfirmDialog(null, "Couldn't connect to server - you'll have to play alone.","Server Connection Error",JOptionPane.PLAIN_MESSAGE);
		}
		setUpMidi();
		
	}
	
	public void buildGUI() {
		
		theFrame = new JFrame("Artem's BeatBox");
		BorderLayout layout = new BorderLayout();
		JPanel background = new JPanel(layout);
		background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		
		checkboxList = new ArrayList<JCheckBox>();
		
		Box buttonBox = new Box(BoxLayout.Y_AXIS);
		JButton start = new JButton("    Start    ");
		start.addActionListener(new MyStartListener());
		buttonBox.add(start);
		
		JButton stop = new JButton("    Stop     ");
		stop.addActionListener(new MyStopListener());
		buttonBox.add(stop);
		
		JButton upTempo = new JButton("Tempo Up     ");
		upTempo.addActionListener(new MyUpTempoListener());
		buttonBox.add(upTempo);
		
		JButton downTempo = new JButton("Tempo Down");
		downTempo.addActionListener(new MyDownTempoListener());
		buttonBox.add(downTempo);
		
		JButton sendIt = new JButton("Send to Server");
		sendIt.addActionListener(new MySendListener());
		buttonBox.add(sendIt);
		
		userMessage = new JTextField();
		buttonBox.add(userMessage);
		
		incomingList = new JList();
		incomingList.addListSelectionListener(new MyListSelectionListener());
		incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane theList = new JScrollPane(incomingList);
		buttonBox.add(theList);
		incomingList.setListData(listVector); // no data to start with
		
		Box nameBox = new Box(BoxLayout.Y_AXIS);
		for(int i = 0; i < 16; i++) {
			nameBox.add(new Label (instrumentNames[i]));
		}
		
		background.add(BorderLayout.EAST, buttonBox);
		background.add(BorderLayout.WEST, nameBox);
		
		theFrame.getContentPane().add(background);
		GridLayout grid = new GridLayout(16, 16);
		grid.setVgap(1);
		grid.setHgap(2);
		mainPanel = new JPanel(grid);
		background.add(BorderLayout.CENTER, mainPanel);
		
		for (int i = 0; i < 256; i++) {
			JCheckBox c = new JCheckBox();
			c.setSelected(false);
			checkboxList.add(c);
			mainPanel.add(c);
		}
		
		theFrame.setBounds(50, 50, 300, 300);
		theFrame.pack();
		theFrame.setVisible(true);
		theFrame.setIconImage(new ImageIcon(new Beatbox().getClass().getResource("/bbx.png")).getImage());
		theFrame.setResizable(false);
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
	}
	
	public void setUpMidi() {
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequence = new Sequence(Sequence.PPQ, 4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);
		} catch (Exception e) {e.printStackTrace();}
		
	}
	
	public void buildTrackAndStart() {
		ArrayList<Integer> trackList = null;
		sequence.deleteTrack(track);
		track = sequence.createTrack();
		
		for (int i = 0; i < 16; i++) {
			
			trackList = new ArrayList<Integer>();
			
			for (int j = 0; j < 16; j++) {
				JCheckBox jc = (JCheckBox) checkboxList.get(j + (16*i));
				if (jc.isSelected()) {
					int key = instruments[i];
					trackList.add(new Integer(key));
				} 
				else {
					trackList.add(null);
				}
			}
			makeTracks(trackList);
		}
		track.add(makeEvent(192,9,1,0,15));
		try {
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
			sequencer.start();
			sequencer.setTempoInBPM(120);
		} catch(Exception e) {e.printStackTrace();}
	}
	
	public class MyStartListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			buildTrackAndStart();
		}
	}
	
	public class MyStopListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			sequencer.stop();
		}
	}
	
	public class MyUpTempoListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor( (float) (tempoFactor * 1.03));
		}
	}
	
	public class MyDownTempoListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float) (tempoFactor * .97));
		}
	}
	
	public class MySendListener implements ActionListener {
		boolean servConected =true;
		public void actionPerformed(ActionEvent a) {
			boolean[] checkboxState = new boolean[256];
			for (int i = 0; i < 256; i++) {
				JCheckBox check = (JCheckBox) checkboxList.get(i);
				if (check.isSelected()) {
					checkboxState[i] = true;
				}
			}
		String messageToSend = null;
		try {
			out.writeObject(userName +" "+ nextNum++ + ":   " + userMessage.getText());
			out.writeObject(checkboxState);
			servConected=true;
		} catch(Exception ex) {
			JOptionPane.showConfirmDialog(null,"Sorry dude. Couldn't send it to the server. Check if server is running.","Server Connection Error",JOptionPane.PLAIN_MESSAGE);
			servConected=false;
		}
		if(!servConected){
			userMessage.setText("  If you start the Server, Restart this program");
			userMessage.setEnabled(false);
		}else{
			userMessage.setText("");	
		}
		}
	}
	
	public class MyListSelectionListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent le) {
			if (!le.getValueIsAdjusting()) {
				String selected = (String) incomingList.getSelectedValue();
				if (selected != null) {
					boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
					changeSequence(selectedState);
					sequencer.stop();
					buildTrackAndStart();
				}
			}
		}
	}
	public class RemoteReader implements Runnable {
		boolean[] checkboxState = null;
		String nameToShow = null;
		Object obj = null;
		public void run() {
			try {
				while ((obj=in.readObject()) != null) {
					System.out.println("got an object from server");
					System.out.println(obj.getClass());
					String nameToShow = (String) obj;
					checkboxState = (boolean[]) in.readObject();
					otherSeqsMap.put(nameToShow, checkboxState);
					listVector.add(nameToShow);
					incomingList.setListData(listVector);
				}
			} catch (Exception ex) {ex.printStackTrace();}
		}
	}
	public class MyPlayMineListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			if (mySequence != null) {
				sequence = mySequence;
			}
		}
	}
	public void changeSequence(boolean[] checkboxState) {
		for (int i = 0; i < 256; i++) {
			JCheckBox check = (JCheckBox) checkboxList.get(i);
			if (checkboxState[i]) {
				check.setSelected(true);
			} else {
				check.setSelected(false);
			}
		}
	}
	public void makeTracks(ArrayList list) {
		Iterator it = list.iterator();
		for (int i = 0; i < 16; i++) {
			Integer num = (Integer) it.next();
			if (num != null) {
				int numKey = num.intValue();
				track.add(makeEvent(144,9,numKey, 100, i));
				track.add(makeEvent(128,9,numKey,100, i + 1));
			}
		}
	}
	public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
		MidiEvent event = null;
		try {
			ShortMessage a = new ShortMessage();
			a.setMessage(comd, chan, one, two);
			event = new MidiEvent(a, tick);
		} catch (Exception e) { }
		return event;
	}
}