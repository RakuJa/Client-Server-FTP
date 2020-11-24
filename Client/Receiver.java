package Client;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Class used to listen for input from command stream
 * @author Giachetto Daniele
 *
 */
public class Receiver extends Thread{

	private Socket s;
	private JTextField checkField;
	private JTextArea textArea;
	private static boolean socketClosed;
	
	/**
	 * Constructor of the Receiver
	 * @param s command socket used to listen
	 * @param textArea textArea in which to put download list
	 * @param checkField field in which to put error or result
	 */
	public Receiver(Socket s,JTextArea textArea,JTextField checkField) {
		this.textArea = textArea;
		this.checkField = checkField;
		this.s = s;
		socketClosed = false;
		start();
	}
	
	@Override
	public void run() {
		
		while (!isSocketClosed()) {
			String outputClient = null;
			BufferedReader br;
			try {
				br = new BufferedReader(new InputStreamReader(s.getInputStream()));
				outputClient = br.readLine();
				//Check is output read is equals to exit command
				if (outputClient != null && !outputClient.equals("/EXIT")) {
					//If command is "/ok" it means operation requested was successful
					if (outputClient.equalsIgnoreCase("/OK")){
						checkField.setText("Operation was successful!");
						checkField.setBackground(Color.GREEN);
						//If command is "/NO" it means something went wrong
					}else if (outputClient.equalsIgnoreCase("/NO")) {
						checkField.setText("Something went wrong!Thinking emoji");
						System.out.println(outputClient);
						checkField.setBackground(Color.RED);
					} else {
						//reset download list
						textArea.setText("");
						//Print again download list into textArea
						String[] downloadList = outputClient.split("\\|");
						for (String downloadItem : downloadList) {
							System.out.println(downloadItem);
							textArea.append(downloadItem + "\n");
						}
						//Set result field to success
						checkField.setText("Operation was successful!");
						checkField.setBackground(Color.GREEN);
					}
					ClientFTP.setAllButtonsOn();
				}else {
					//If server accepted or told to close connection stop listening
					setSocketStatusAsClosed();
				}
			} catch (IOException e) {
				//Field for result or errors
				checkField.setText("Fatal Error detected!");
				checkField.setBackground(Color.RED);
				textArea.append("Error fetching download list or input from server! Try reloading");
				System.out.println("Errori rilevati nella lettura dell'input");
				//Error with connection,stop listening
				setSocketStatusAsClosed();
			}
		}	
	}
	
	/**
	 * Method used to set client as not listening anymore
	 */
	public static synchronized void setSocketStatusAsClosed() {
		socketClosed = true;
	}
	
	/**
	 * Method used to check if client is still listening for input
	 * @return true if client is listening
	 */
	public static synchronized boolean isSocketClosed() {
		return socketClosed;
	}
	
	
}
