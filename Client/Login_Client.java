package Client;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * main class Client side, used to connect to the server 
 * @author Giachetto Daniele
 *
 */
public class Login_Client {

	public static void main(String argv[]) {

		//Impostazione di un interfaccia utente
		JFrame frame = new JFrame("Login | Inserire IP Server");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(true);
		JPanel inputpanel = new JPanel();
		inputpanel.setLayout(new FlowLayout());
		//textField usato per ricevere l'ip desiderato dall'utente
		JTextField input = new JTextField(30);
		input.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String inputIp = input.getText();
				//Creazione del nuovo oggetto che instaurera' la connessione
				new ClientFTP(inputIp);
				//chiusura di questa parte del programma
				frame.dispose();
			}
		});
		
		//Bottone usato per prendere in input l'ip inserito nel textField
		JButton button = new JButton("Enter IP");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String inputIp = input.getText();
				//Creazione del nuovo oggetto che instaurera' la connessione
				new ClientFTP(inputIp);
				//chiusura di questa parte del programma
				frame.dispose();
			}
		});
		//Inserimento vari oggetti nel pannello e del pannello nel frame
		inputpanel.add(input);
		inputpanel.add(button);
		panel.add(inputpanel);
		frame.getContentPane().add(BorderLayout.CENTER, panel);
		frame.pack();
		frame.setLocationByPlatform(true);
		frame.setVisible(true);
		frame.setResizable(false);
		input.requestFocus();

	}

}
