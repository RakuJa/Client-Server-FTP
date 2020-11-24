package Client;



import java.awt.BorderLayout;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.DefaultCaret;

/**
 * Class client side that creates interface
 * @author Giachetto Daniele
 *
 */
public class ClientFTP extends JPanel{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	
	private static final String STANDARDNAME = "H:/";
	
	//Public Keys
	private static BigInteger coPrime;
	private static BigInteger n;
	
	private String archiveName;
	private ObjectOutputStream outS;
	private String serverIp;
	private Socket s = null;
	private Socket sftp = null;
	private JTextField checkField;
	private JTextArea textArea;
	private JTextField fieldName;
	private JPasswordField fieldPass;
	private JPasswordField fieldConfirmPass;
	
	//Buttons 
	
	private static JButton helperButton;
	private static JButton fetchButton;
	private static JButton downButton;
	private static JButton upButton;
	private static JButton registerButton;
	private static JButton loginButton;
	private static JButton logoutButton;
	
	public ClientFTP(String serverIp) {
		createFrame();
	}
	
	/**
	 * Method that contains creates an interface and handle basic feature like disconnecting connecting and sending messages
	 */
	public void createFrame() {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				//Creazione dell'interfaccia e assegnazione di vari attributi ad essa
				checkField = new JTextField("Results will be displayed here!          ");
				checkField.setEditable(false);
				JFrame frame = new JFrame("Benvenuto nel client FTP di Daniele Giachetto!");
				JPanel panel = new JPanel();
				panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
				panel.setOpaque(true);
				//Creazione textArea dove verranno visualizzati messaggi
				textArea = new JTextArea(15, 50);
				textArea.setWrapStyleWord(true);
				textArea.setLineWrap(true);
				textArea.setEditable(false);
				textArea.setFont(Font.getFont(Font.SANS_SERIF));
				JScrollPane scroller = new JScrollPane(textArea);
				scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
				scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
				
				//JPanel FOR USER INTERFACE
				
				JPanel helpPanel = new JPanel();
				helpPanel.setLayout(new FlowLayout());
				
				//JPANEL FOR BUTTONS
				JPanel inputPanel = new JPanel();
				inputPanel.setLayout(new FlowLayout());
				
				//BUTTONS
				helperButton = new JButton("Press for help!");
				fetchButton = new JButton("Fetch download list");
				downButton = new JButton("Download");
				upButton = new JButton("Upload");
				registerButton = new JButton("Register");
				loginButton = new JButton("Login");
				logoutButton = new JButton("Logout");
				//Action listener per gestire l'invio utilizzando il tasto sulla tastiera
				
				//Prova a stabilire la connessione,se non viene stabilita viene chiuso il programma
				if (!establishConnection()) {
					//custom title, error icon
					JOptionPane.showMessageDialog(frame,
					    "Connection with the server lost,try connecting again", //description
					    "Connection error", //title
					    JOptionPane.ERROR_MESSAGE);
					System.exit(1);
				}
				
				
				
				/**
				 * Action listener used to request an helper
				 */
				helperButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {		
						checkField.setText("Results will be displayed here!");
						checkField.setBackground(Color.WHITE);
						textArea.append("");
						int choice = JOptionPane.showOptionDialog(null, 
								"Do you want the guide to be in italian? "
								+ "\n"
								+ "Vuoi che la guida sia in italiano?", 
								"Choose helper language", 
							      JOptionPane.YES_NO_OPTION, 
							      JOptionPane.QUESTION_MESSAGE, 
							      null, null, null);

							  // interpret the user's choice
						if (choice == JOptionPane.YES_OPTION) {
							textArea.setText("");
							textArea.append("Benvenuto nel client FTP realizzato da Giachetto Daniele." + "\n" + "\n");
							textArea.append("Ti stai forse chiedendo quale sia l'utilita' di questo programma?Questa guida e' per te allora!" + "\n "
									+ "Questo programma e' stato creato per poter fornire la possibilita' di caricare file in un server remoto" + "\n"
							    	+ "qualsiasi file si desideri e potere poi in un secondo o anche in nessun momento," + "\n"
							    	+ "scaricarlo o scaricare file messi a disposizione da altri client." + "\n");
							textArea.append("\n" + "\n");
							textArea.append("L'interfaccia utente e' composta 6 pulsanti con diverse funzionalita',partendo da sinistra :" + "\n");
							textArea.append("Il pulsante FETCH :  serve per richiedere al server la lista dei file disponibili per il download");
							textArea.append("\n" + "\n");
							textArea.append("Il pulsante DOWNLOAD :  serve per poter scaricare file messi a disposizione dal server, per poterlo fare basta"
							    	+ " solamente digitare il nome del file desiderato (contenente anche l'estensione) e premere il pulsante ok,in seguito"
							    	+ " e' consigliato aspettare che venga mostrato prima di fare altro se l'operazione e' stata effettuata con successo o meno");
							textArea.append("\n" + "\n");
							textArea.append("Il pulsante UPLOAD :  serve per poter mandare al server qualsiasi file si desidera,si possono mandare uno o pie' file"
							    	+ " a scelta dell'utente. Essi se vengono caricati mentre si e' loggati con un account specifico diventano privati e visibili" + 
							    	" solo a quell'account,mentre se si carica quando non si e' entrati con nessun account i file risultano" + 
							    	" pubblici e scaricabili da tutti i client");
							textArea.append("\n" + "\n");
							textArea.append("Il pulsante REGISTER :  serve per potersi creare un account privato,ogni account privato ha una sua directory privata");
							textArea.append("\n" + "\n");
							textArea.append("Il pulsante LOGIN :  serve per poter accedere con il proprio account creato precedentemente");
							textArea.append("\n" + "\n");
							textArea.append("Il pulsante LOGOUT :  serve per poter uscire dal proprio account e ritornare utente generico");
							textArea.append("\n" + "\n");
							textArea.append("Infine per terminare correttamente l'esecuzione del software bisogna solamente premere il pulsante di chisura "
							    	+ "rosso in alto a destra. Grazie mille per aver usato questo programma!");
						}else if (choice == JOptionPane.NO_OPTION) {
							
							textArea.setText("");
							textArea.append("Welcome to Giachetto Daniele's FTP Client!" + "\n" + "\n");
							textArea.append("You are asking yourself what you can do with this software?If this is the case then this guide is for you!" + "\n"
								    + "This software has been created to offer the possibility to upload files to a remote server" + "\n"
									+ "every kind of file you desire and to have the ability whenever and however you want," + "\n"
								    + "to download it or every file that has been put on the server by all the others client." + "\n");
							textArea.append("\n" + "\n");
							textArea.append("The user interface is made up by 6 buttons with different functionality,starting from the left :" + "\n");
							textArea.append("FETCH button :  used to request a list of file that can be downloaded from the server");
							textArea.append("\n" + "\n");
							textArea.append("DOWNLOAD button :  used to download files  up for download from the server, to do that you just need to"
								    + " type the file's name (even the extension like .txt) and press 'ok' button,after this operation"
								    + " it's suggested to wait that the result of the operation is been shown before doing other actions");
							textArea.append("\n" + "\n");
							textArea.append("UPLOAD button :  used to send to the server every file desired,you can even send more than one file at a time."
								    + " Files uploaded will be stored in a specific and private directory if you are logged in and they will be visible and up for download"
								    + " only from the user that uploaded ,otherwise if files are uploaded while you are not logged in files will be public and downloaded from everyone");
							textArea.append("\n" + "\n");
							textArea.append("REGISTER button :  used to create new private accounts, each account has a private directory for storage");
							textArea.append("\n" + "\n");
							textArea.append("LOGIN button :  used to login in an account created prior to this");
							textArea.append("\n" + "\n");
							textArea.append("LOGOUT button :  used to exit the account you are logged in and return a generic user");
							textArea.append("\n" + "\n");
							textArea.append("If you want to close this fantastic software you only need to press the big and red closing button on the upper right corner. "
								    + "Thank you very much for downloading and using this software!");
						}
					}
				});
				
				/**
				 * Action listener used to request a list of file
				 */
				fetchButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						//Se la connessione �ｿｽ ancora attiva
						if (!Receiver.isSocketClosed()) {
							setAllButtonsOff();
							checkField.setText("Results will be displayed here!");
							checkField.setBackground(Color.WHITE);
							//Avvisa il server di volere la lista di file
							warnServer("/REFRESH");
						}else {
							//custom title, error icon
							JOptionPane.showMessageDialog(frame,
							    "Connection with the server lost,try connecting again", //description
							    "Connection error", //title
							    JOptionPane.ERROR_MESSAGE);
							System.exit(1);
						}
					}
				});
				
				/**
				 * Action listener used to request downlaod file
				 */
				downButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						
						//Controlla se la connessione �ｿｽ ancora attiva
						if (!Receiver.isSocketClosed()) {
							checkField.setText("Results will be displayed here!");
							checkField.setBackground(Color.WHITE);
							String fileName = JOptionPane.showInputDialog(frame, "Type File's name here!");
							//check if file isn't equals at  null
							if (fileName!=null && !fileName.equals("")) {
								setAllButtonsOff();
								//Avvisa il server della richiesta e del nome del file desiderato
								warnServer("/DOWNLOAD " + fileName);
								//Se il ricevitore oggetto sta gia ascoltando bene,altrimenti rimetterlo in ascolto
								if (RicevitoreFTP.isListening()) {
									System.out.println("ReceiverFTP Correctly listening");
								}else {
									new RicevitoreFTP(sftp,checkField);
								}
							}else {
								//Mostra messaggio di errore,cancellata l'op
								checkField.setText("Action canceled!");
								checkField.setBackground(Color.ORANGE);
							}
						}else {
							//custom title, error icon
							JOptionPane.showMessageDialog(frame,
							    "Connection with the server lost,try connecting again", //description
							    "Connection error", //title
							    JOptionPane.ERROR_MESSAGE);
							System.exit(1);
						}
					}

				});
				
				/**
				 * Action listener for upload button
				 */
				upButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						//Se il ricevitore non ﾃｨ chiuso e quindi la connessione ﾃｨ attiva
						if (!Receiver.isSocketClosed()) {
							
							//textField that display error or results
							checkField.setText("Results will be displayed here!");
							checkField.setBackground(Color.WHITE);
							boolean wantDirectory = false;
							int choice = JOptionPane.showOptionDialog(null, 
									"Do you want to upload directory?" +"\n" + "YES == Upload Directory" + "\n" + "NO == Upload files(everything exept directory)", 
									"Upload", 
								      JOptionPane.YES_NO_OPTION, 
								      JOptionPane.QUESTION_MESSAGE, 
								      null, null, null);

								  // interpret the user's choice
							if (choice == JOptionPane.YES_OPTION) {
								wantDirectory = true;
							}
							
							// Chiamata ad un metodo che ritorna un array di
							// file richiesti dall'utente
							File[] file = fileChooser(wantDirectory);
							// Se il vettore ed i file esistono e non sono nulli
							if (file != null && fileExists(file)) {
								setAllButtonsOff();
								// Avvisa il server che si vuole utilizzare
								// l'operazione di upload
								warnServer("/UPLOAD");
								try {
									Thread.sleep(500);
								} catch (InterruptedException e1) {
									System.out.println("Errore nella corretta gestione del thread");
								}
								// Create zip file
								File zipToSend = null;
								//Compress a file
								if (!wantDirectory) {
									zipToSend = compress(file);
								//Compress a directory
								} else {
									if (file != null && file[0] != null && file[0].exists()) {
										zipToSend = compressDirectory(file[0]);
										System.out.println("Directory");
									} else {
										checkField.setText("Problem detected with directory!");
										checkField.setBackground(Color.RED);
									}
								}if (zipToSend != null) {
									inviaFile(zipToSend);
								}else {
									checkField.setText("Error compressing!");
									checkField.setBackground(Color.RED);
								}
							} else {
								checkField.setText("Action canceled!");
								checkField.setBackground(Color.ORANGE);
							}
						}else {
							//title, error icon
							JOptionPane.showMessageDialog(frame,
							    "Connection with the server lost,try connecting again", //description
							    "Connection error", //title
							    JOptionPane.ERROR_MESSAGE);
							System.exit(1);
						}
					}

				});
				
				
				/**
				 * Action listener for register button
				 */
				registerButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						//If receiver is up and connection working
						if (!Receiver.isSocketClosed()) {
							//textField that display error or results
							checkField.setText("Results will be displayed here!");
							checkField.setBackground(Color.WHITE);
							//name,password and confirm password field
							fieldName = new JTextField();
							fieldPass = new JPasswordField();
							fieldConfirmPass = new JPasswordField();

							
							Object[] message = { "Username:", fieldName, "Password:", fieldPass, "Confirm Password",
									fieldConfirmPass };
							int option = JOptionPane.showConfirmDialog(null, message, "Register",JOptionPane.OK_CANCEL_OPTION);
							
							//If the user pressed "okay"
							if (option == JOptionPane.OK_OPTION) {
								String confirmPass = new String(fieldConfirmPass.getPassword());
								String password = new String(fieldPass.getPassword());
								String userName = fieldName.getText();
								
								// If the user did not left any field empty
								if (userName != null && confirmPass != null && password != null
										&& !confirmPass.equals("") && !password.equals("") && !userName.equals("")
										&& !userName.endsWith(" ") && !password.endsWith(" ") && !confirmPass.endsWith(" ")
										&& password.equals(confirmPass)) {
									if (!password.contains(";") && !confirmPass.contains(";")) {
										setAllButtonsOff();
										password = encrypt(password, coPrime, n);
										confirmPass = encrypt(confirmPass, coPrime, n);
										// Request register to server
										warnServer("/register " + userName + " " + password + " " + confirmPass);
									} else {
										// textField that display error
										checkField.setText("Prohibited char -> ; !");
										checkField.setBackground(Color.RED);
									}
								} else {
									// textField that display error or results
									checkField.setText("Fill every field correctly!");
									checkField.setBackground(Color.RED);
								}
							} else {
								//User did not press "okay"
								checkField.setText("Action canceled!");
								checkField.setBackground(Color.ORANGE);
							}
						} else {
							//title, error icon
							JOptionPane.showMessageDialog(frame, "Connection with the server lost,try connecting again", // description
									"Connection error", // title
									JOptionPane.ERROR_MESSAGE);
							System.exit(1);
						}
					}
				});

				/**
				 * Action listener for login button
				 */
				loginButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						//If receiver is up and connection working
						if (!Receiver.isSocketClosed()) {
							//textField that display error or results
							checkField.setText("Results will be displayed here!");
							checkField.setBackground(Color.WHITE);
							
							fieldName = new JTextField();
							fieldPass = new JPasswordField();
							Object[] message = { "Username:", fieldName, "Password:", fieldPass};
							int option = JOptionPane.showConfirmDialog(null, message, "Login",JOptionPane.OK_CANCEL_OPTION);
							
							//If user pressed "okay"
							if (option == JOptionPane.OK_OPTION) {
								String password = new String(fieldPass.getPassword());
								String userName = fieldName.getText();
								//If user did not left any field empty
								if (userName != null && password != null && !userName.equals("")&& !password.equals("")
										&& !userName.endsWith(" ") && !password.endsWith(" ")) {
									//Checks if password does not contain prohibited char and is not too long
									if (!password.contains(";") && password.length()<=18) {
										setAllButtonsOff();
										// encrypt
										password = encrypt(password, coPrime, n);
										// Request login to server
										warnServer("/login " + userName + " " + password);
									}else {
										//textField that display error
										checkField.setText("Prohibited char -> ; !");
										checkField.setBackground(Color.RED);
									}
								}else {
									//textField that display error or results
									checkField.setText("Fill all the fields!");
									checkField.setBackground(Color.RED);
								}
							}else {
								checkField.setText("Action canceled!");
								checkField.setBackground(Color.ORANGE);
								System.out.println("User did not press okay");
							}
						}else {
							//title, error icon
							JOptionPane.showMessageDialog(frame,
							    "Connection with the server lost,try connecting again", //description
							    "Connection error", //title
							    JOptionPane.ERROR_MESSAGE);
							System.exit(1);
						}
					}
				});
				
				/**
				 * Action listener used to request a logout from the account
				 */
				logoutButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						//Se la connessione e' ancora attiva
						if (!Receiver.isSocketClosed()) {
							checkField.setText("Results will be displayed here!");
							checkField.setBackground(Color.WHITE);
							//Avvisa il server di volere la lista di file
							int choice = JOptionPane.showOptionDialog(null, 
									"Are you sure you want to logout?", 
									"Logout", 
								      JOptionPane.YES_NO_OPTION, 
								      JOptionPane.QUESTION_MESSAGE, 
								      null, null, null);

								  // interpret the user's choice
							if (choice == JOptionPane.YES_OPTION) {
								setAllButtonsOff();
								warnServer("/LOGOUT");
							}else {
								
							}
						}else {
							//custom title, error icon
							JOptionPane.showMessageDialog(frame,
							    "Connection with the server lost,try connecting again", //description
							    "Connection error", //title
							    JOptionPane.ERROR_MESSAGE);
							System.exit(1);
						}
					}
				});
				
				//Definizione della modalita' in cui si potra' chiudere la finestra
				frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				frame.addWindowListener(new java.awt.event.WindowAdapter() {
					@Override
					public void windowClosing(java.awt.event.WindowEvent windowEvent) {
						// Mostrare finestra di dialogo in cui chiedere se si �ｾ�ｽｨ sicuri di voler chiudere la finestra
						if (JOptionPane.showConfirmDialog(frame, "Are you sure you want to close this software?",
								"Warning", JOptionPane.YES_NO_OPTION,
								// se l'utente conferma di voler chiudere la finestra essa verra' chiusa definitivamente
								JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
							setAllButtonsOff();
							//Keep on trying to close the connection for 5 time,each time waiting for a longer time for server response
							int i = 500;
							while (!Receiver.isSocketClosed() && i<5000) {
								//Warn server that disconnection is requested
								warnServer("/EXIT");
								try {
									Thread.sleep(i);
								}catch (InterruptedException e ) {
									System.out.println("Error while waiting");
								}
								i*=2;
							}
							//Closing stream and object socket
							try {
								if (outS != null) {
									outS.flush();
									outS.reset();
								}
								RicevitoreFTP.close();
								sftp.close();
							} catch (IOException e) {
								System.out.println("Problems detected closing object stream and socket");
							}
							//Delete zip archive
							File toDelete = new File(archiveName);
							try {
								Files.delete(toDelete.toPath());
							} catch (IOException e) {
								System.out.println("File does not exists || Error deleting file");
							}
							System.out.println(toDelete.delete());
							if (Receiver.isSocketClosed()) {
								System.out.println("Closed without errors");
								System.exit(0);
							}else {
								System.out.println("Closed with errors");
								System.exit(1);
							}
						}
					}
				});
				
				
			
				
				//Ultimi dettagli sugli attributi dell'interfaccia grafica
				DefaultCaret caret = (DefaultCaret) textArea.getCaret();
				caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
				helpPanel.add(checkField);
				helpPanel.add(helperButton);
				panel.add(helpPanel);
				panel.add(scroller);
				inputPanel.add(fetchButton);
				inputPanel.add(downButton);
				inputPanel.add(upButton);
				inputPanel.add(registerButton);
				inputPanel.add(loginButton);
				inputPanel.add(logoutButton);
				panel.add(inputPanel);
				frame.getContentPane().add(BorderLayout.CENTER, panel);
				frame.pack();
				frame.setLocationByPlatform(true);
				frame.setVisible(true);
				frame.setResizable(false);
			}
		});
	}
	
	/**
	 * Method used to prepare stream for compression
	 * @param file directory to compress
	 * @return File compressed
	 */
	private File compressDirectory(File file) {
		try {
			File outputFile = new File(archiveName);
			outputFile.createNewFile();
			compressDirectory(file.getAbsolutePath(), outputFile.getAbsolutePath());
			return outputFile;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}
	

	/**
	 * Method used to prepare streams for directory compression
	 * @param srcFolder Absolute path of directory to compress
	 * @param destZipFile Absolute path of destination zip file
	 * @throws IOException if problems with streams are encountered
	 */
	private void compressDirectory(String srcFolder, String destZipFile) throws Exception {
		ZipOutputStream zip = null;
		FileOutputStream fileWriter = null;

		fileWriter = new FileOutputStream(destZipFile);
		zip = new ZipOutputStream(fileWriter);

		addFolderToZip("", srcFolder, zip);
		zip.flush();
		zip.close();
	}

	/**
	 * Method used recoursively to add single file to destination zipfile
	 * @param path path used to know in which directory we are in
	 * @param srcFile Absolute path of directory to compress
	 * @param zip Stream in which to write file
	 * @throws IOException if problems with streams are encountered
	 */
	private void addFileToZip(String path, String srcFile, ZipOutputStream zip) throws Exception {

		File folder = new File(srcFile);
		if (folder.isDirectory()) {
			addFolderToZip(path, srcFile, zip);
		} else {
			byte[] buf = new byte[1024];
			int len;
			FileInputStream in = new FileInputStream(srcFile);
			zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
			while ((len = in.read(buf)) > 0) {
				zip.write(buf, 0, len);
			}
			in.close();
		}
	}

	
	/**
	 * Method used to add a folder to zip file,it takes every file from the folder and write them
	 * @param path used to know in which directory the method is,to handle recoursivity
	 * @param srcFolder Absolute path of directory to compress
	 * @param zip Stream in which to write
	 * @throws IOException if there are problems with the stream
	 */
	private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) throws Exception {
		File folder = new File(srcFolder);

		for (String fileName : folder.list()) {
			if (path.equals("")) {
				addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
			} else {
				addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
			}
		}
	}
	
	
	
	/**
	 * Method that checks if every file in the array is not null and exists
	 * @param files array of files
	 * @return true if every file in the array exists
	 */
	private boolean fileExists(File[] files) {
		if (files != null) {
			for (File file : files) {
				if (file != null) {
					//Checks if file exists, doesn't matter if it's a folder or a file
					if (file.exists()) {
						System.out.println(file.getName() + " | Is a file");
					} else {
						return false;
					}
				} else {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Method that compress a given array of file into one zip file
	 * @param srcFiles array of file to compress
	 * @return File containing every array entry
	 */
	private File compress(File[] srcFiles) {
		try {
			// create byte buffer
			byte[] buffer = new byte[1024];
			
			//Create zip file
			new File(archiveName).createNewFile();
			
			FileOutputStream fos = new FileOutputStream(archiveName);
			ZipOutputStream zos = new ZipOutputStream(fos);
			
			for (File srcFile : srcFiles) {
				
				FileInputStream fis = new FileInputStream(srcFile);
				// begin writing a new ZIP entry, positions the stream to the
				// start of the entry data
				zos.putNextEntry(new ZipEntry(srcFile.getName()));
				int length;
				while ((length = fis.read(buffer)) > 0) { //cannot read folders
					zos.write(buffer, 0, length);
				}

				
				zos.closeEntry();

				// close the InputStream
				fis.close();
				
			}
			// close the ZipOutputStream
			zos.close();
			fos.close();
		} catch (IOException e) {
			System.out.println("Error creating zip file: " + e);
			return null;
		}
		
		//Return zip file containing every entry
		return new File(archiveName);
	}
	
	/**
	 * Method used to send message to the server (Using command socket)
	 * @param output command to send
	 */
	private void warnServer(String output) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
			pw.println(output);
			pw.flush();
			System.out.println("Message sent");
		} catch (IOException e) {
			System.out.println("Generic error contacting server");
		}
	}

	/**
	 * Method used to establish connection with the server,it tries to do that with inputed ip,local host and set ip
	 * @return true if connection is correctly established
	 */
	public boolean establishConnection() {
		try {
			//Prova a connettersi all'ip inserito dall'utente
			s = new Socket(serverIp, 55555);
			sftp = new Socket(serverIp, 55556);
			warnServer("/KEY");
			if (getKeys()) {
				System.out.println("Key got");
				System.out.println(n);
				System.out.println(coPrime);
				warnServer("/KEYGOT");
				new RicevitoreFTP(sftp,checkField);
				new Receiver(s, textArea,checkField);
				archiveName = STANDARDNAME + s.getLocalPort()+ ".zip";
				return true;
			}else {
				//Contacs server telling him to close connection
				warnServer("/NO");
			}
		} catch (IOException e) {
			System.out.println("Connection failed with inputed ip");
			try {
				//Prova a connettersi a localhost
				s = new Socket("127.0.0.1", 55555);
				sftp = new Socket("127.0.0.1", 55556);
				warnServer("/KEY");
				if (getKeys()) {
					System.out.println("Key got");
					System.out.println(n);
					System.out.println(coPrime);
					warnServer("/KEYGOT");
					System.out.println("Server warned");
					new RicevitoreFTP(sftp, checkField);
					new Receiver(s, textArea,checkField);
					archiveName = STANDARDNAME + s.getLocalPort()+ ".zip";
					serverIp = "127.0.0.1";
					return true;
				}else {
					//Contacts server telling him to close connection
					warnServer("/NO");
				}
			} catch (IOException e1) {
				System.out.println("Connection failed with local host");
				try {
					//Prova a connettersi con un ip di default
					s = new Socket("192.168.103.231", 55555);
					sftp = new Socket("192.168.103.231", 55556);
					warnServer("/KEY");
					if (getKeys()) {
						System.out.println("Key got");
						warnServer("/KEYGOT");
						serverIp = "192.168.103.231";
						new RicevitoreFTP(sftp,checkField);
						new Receiver(s, textArea,checkField);
						archiveName = STANDARDNAME + s.getLocalPort()+ ".zip";
						return true;
					}else {
						//Contacts server telling him to close connection
						warnServer("/NO");
					}
				} catch (IOException e2) {
					//If it cannot connect it closes the application 
					s=null;
				}
			}
		}
		return false;
	}
	
	/**
	 * Method used to get public keys from server
	 * @return true if keys are correctly fetched
	 * @throws IOException if there are problems with the stream
	 */
	private boolean getKeys() throws IOException {
		ObjectInputStream inputS = new ObjectInputStream(sftp.getInputStream());
		Vector<?> publicKeys = null;
		System.out.println("In attesa delle chiavi");
		try {
			publicKeys = (Vector<?>) inputS.readObject();
			coPrime = (BigInteger) publicKeys.get(0);
			n = (BigInteger) publicKeys.get(1);
			if (coPrime == null || n == null) {
				return false;
			}
		} catch (ClassNotFoundException except) {
			System.out.println("Error receiving keys");
			return false;
		}
		return true;
	}

	/**
	 * Method used to choose file from disk
	 * @param d used to check if directory is requested or file
	 * @return an array of file choosen by the user
	 */
	private File[] fileChooser(boolean d) {
		
		
		JFileChooser fileChooser = new JFileChooser();
		
		if (d) {
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		    fileChooser.setAcceptAllFileFilterUsed(false);
		}
		//Allow multiple file to be selected
		fileChooser.setMultiSelectionEnabled(true);
		//Show interface to user
		int n = fileChooser.showOpenDialog(this);
		//If user did not select a file
		if (n == JFileChooser.CANCEL_OPTION || n== JFileChooser.ERROR_OPTION) {
			return null;
		}
		return fileChooser.getSelectedFiles();
	}
	

	/**
	 * Method used to send file to the server (Uses File Socket)
	 * @param file zipFile to send
	 */
	private void inviaFile(File file) {

		if (file != null) {
			try {
				outS = new ObjectOutputStream(sftp.getOutputStream());
				if (outS != null) {
					outS.reset();
					outS.writeObject(file);
					outS.flush();
				}
			} catch (IOException e) {
				//textField that display error or results
				checkField.setText("Error detected!");
				checkField.setBackground(Color.RED);
				System.out.println("Problems detected sending file");
			}

		}
	}
	
	/**
	 * Method used to encrypt messages
	 * @param data message to encrypt
	 * @param e key
	 * @param n key
	 * @return Encrypted message
	 */
	private static String encrypt(String data,BigInteger e,BigInteger n) {	
		
		//Splits data string into char array,convert each char in a number and crypts it
		//In the end returns a string of number 
		
		Vector<BigInteger> intVector = new Vector<BigInteger>();

		StringBuilder intList = new StringBuilder();

		char[] charRawVector = new char[data.length()];

		data.getChars(0, data.length(), charRawVector, 0);		
		
		for (char character : charRawVector) {
			int i = (int) character;
			if (i != 13) {
				BigInteger c = new BigInteger(Integer.toString(i)).modPow(e,n);
				intVector.add(new BigInteger(Integer.toString(i)));
				intList.append(c);
				intList.append(";");
			} else {
				intList.append("\n");
			}
		}
			if (data.length()<10) {
				intList.append("0");
			}
			intList.append(data.length());
		System.out.println("ENCRYPT --> " + intVector);
		System.out.println(intList.toString());
		return intList.toString();
		
	}
	/**
	 * Method used to set all buttons off
	 */
	private static void setAllButtonsOff() {
		fetchButton.setEnabled(false);
		downButton.setEnabled(false);
		upButton.setEnabled(false);
		registerButton.setEnabled(false);
		loginButton.setEnabled(false);
		logoutButton.setEnabled(false);
	}
	
	/**
	 * Methdo used to set all buttons on
	 */
	public static void setAllButtonsOn() {
		fetchButton.setEnabled(true);
		downButton.setEnabled(true);
		upButton.setEnabled(true);
		registerButton.setEnabled(true);
		loginButton.setEnabled(true);
		logoutButton.setEnabled(true);
	}
	

} 