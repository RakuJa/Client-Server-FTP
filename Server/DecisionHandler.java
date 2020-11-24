package Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Class used to take commands from stream and process them
 * @author Giachetto Daniele
 *
 */
public class DecisionHandler extends Thread {

	private Socket s; //Socket for commands
	private Socket sftp; //Socket for file
	private static final String USERFILE = "Credentials.txt";
	private static final String LOGFILE = ServerDFTP.getLogFile();
	private PrintWriter pw;
	private String name = "User";
	private static final String PATH = ServerDFTP.getPath();
	private static String token = ";";
	private BigInteger d;
	private BigInteger n;

	
	/**
	 * Main constructor of DecisionHandler class
	 * @param s command socket
	 * @param sftp object socket
	 */
	public DecisionHandler(Socket s,Socket sftp) {
		this.s = s;
		this.sftp = sftp;
		start();
	}

	@Override
	public void run(){

		try {
			
			//Keep on trying to send key to the user untill user receives it
			while(!keyExchange());
			
			String inputMessage = null;

			BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			while (true) {
				// if there are errors with the stream or client asks to close connection server stop handling client.
				if (s == null || sftp == null || s.isClosed() || (inputMessage = br.readLine()) == null
						|| inputMessage.equals("") || inputMessage.equals("/EXIT")) {
					break;
				} else {

					// if upload is requested server let another thread handle it
					if (inputMessage.equalsIgnoreCase("/UPLOAD")) {
						new UploadHandler(PATH + name, sftp, s);
						continue;
						// If list of file is requested server let another thread handle it
					} else if (inputMessage.equalsIgnoreCase("/REFRESH")) {
						new SenderHandler(name, s, true);
						continue;
						// If logout is requested server let another thread handle it
					} else if (inputMessage.equalsIgnoreCase("/LOGOUT")) {
						String oldName = name;
						name = "User";
						writeString("SUCCESS | User :" + oldName + " Logged out from : "
								+ s.getInetAddress().getHostAddress(), LOGFILE);
						new SenderHandler(s, "/OK");
						continue;
					} else {
						// Divide complex command into an array
						String[] arrPhrase = inputMessage.split(" ");
						// Checks if there are null cells into the vector
						if (isNotNull(arrPhrase)) {
							// Checks into the first word to understand command

							// If register is requested
							if (arrPhrase[0].equalsIgnoreCase("/Register")) {
								registerFunction(arrPhrase);
								continue;
								// If login is requested
							} else if (arrPhrase[0].equalsIgnoreCase("/Login")) {
								loginFunction(arrPhrase);
								continue;
								// If download is requested let thread handle it
							} else if (arrPhrase[0].equalsIgnoreCase("/DOWNLOAD") && arrPhrase.length == 2) {
								new SenderHandler(name, sftp, arrPhrase[1]);
								continue;
							}
						}
					}
				}
				new SenderHandler(s, "/NO");
			}
		} catch (IOException e) {
			writeString("EXCEPTION | Error setting command socket input stream ", LOGFILE);
		} finally {
			if (s != null) {
				new SenderHandler(s, "/EXIT"); // Closing message to client
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					writeString("EXCEPTION | Error handling thread  ", LOGFILE);
				}

				try { // Chiusura socket
					if (sftp != null)
						sftp.close();
					s.close();
					System.out.println("Socket closed");
				} catch (IOException e) {
					writeString("EXCEPTION | Error closing correctly socket  ", LOGFILE);
				}
			}
		}
	}


	/**
	 * Method used to login user with their credentials
	 * 
	 * @param arrPhrase
	 *            String array containing useful information to analyze and utilize
	 */
	private void loginFunction(String[] arrPhrase) {
		// If the phrases is composed by 3 words
		if (arrPhrase.length == 3) {
			String userName = arrPhrase[1], password = arrPhrase[2];
			// Decrypt password sent
			password = decrypt(password, d, n);
			password = getSHA512(password);
			// If credentials exists && doesn't contain illegal char or words
			if (credentialsExist(userName, password, USERFILE) && !containsIllegalCharacter(userName)) {
				name = userName;
				writeString("SUCCESS | User : " + name + " Logged in from : " + s.getInetAddress().getHostAddress(), LOGFILE);
				new SenderHandler(s, "/OK");
			} else { // An error log is written
				writeString("ERROR | Error logging in from : " + s.getInetAddress().getHostAddress(), LOGFILE);
				new SenderHandler(s, "/NO");
			}
		} else { // An error log is written
			writeString("ERROR | Error logging in from : " + s.getInetAddress().getHostAddress(), LOGFILE);
			new SenderHandler(s, "/NO");
		}
	}

	/**
	 * Method used to check for string with character not accepted by server
	 * @param toCheck string that will be examined
	 * @return true if the string passed contains an unwanted char or it's a prohibited name
	 */
	private boolean containsIllegalCharacter(String toCheck) {
		// Basic check with char that cannot be used on folders
		boolean basicCheck = ((toCheck.contains("<") || toCheck.contains(">")) || toCheck.contains("/")
				|| toCheck.contains(":") || toCheck.contains(String.valueOf('"')) || toCheck.contains("\\")
				|| toCheck.contains("|") || toCheck.contains("?") || toCheck.contains("*") || toCheck.contains("!")
				|| toCheck.contains("�ｿｽ") || toCheck.equalsIgnoreCase("User") || toCheck.equalsIgnoreCase("FTPBin"));
		
		// check if string doesn't end with a prohibited char
		//char 32 equals space
		boolean mediumCheck = toCheck.endsWith(String.valueOf((char) 32)) || toCheck.endsWith(".");
		
		// Advanced check with first 32 ascii code
		boolean advCheck = false;
		try {
			for (int i = 0; i < 32 && !advCheck; ++i) {
				char c = (char) i;
				advCheck = toCheck.contains(String.valueOf(c));
			}
		} catch (Exception e) {
			advCheck = true;
		}
		
		//Check if name is a reserved windows name
		boolean finalCheck = (toCheck.equalsIgnoreCase("CON") || toCheck.equalsIgnoreCase("PRN") || toCheck.equalsIgnoreCase("AUX")
				|| toCheck.equalsIgnoreCase("NUL") || toCheck.equalsIgnoreCase("COM1") || toCheck.equalsIgnoreCase("COM2")  
				|| toCheck.equalsIgnoreCase("COM3") || toCheck.equalsIgnoreCase("COM4") || toCheck.equalsIgnoreCase("COM5") 
				|| toCheck.equalsIgnoreCase("COM6") || toCheck.equalsIgnoreCase("COM7") || toCheck.equalsIgnoreCase("COM8")
				|| toCheck.equalsIgnoreCase("COM9") || toCheck.equalsIgnoreCase("LPT1") || toCheck.equalsIgnoreCase("LPT2")
				|| toCheck.equalsIgnoreCase("LPT3") || toCheck.equalsIgnoreCase("LPT4") || toCheck.equalsIgnoreCase("LPT5")
				|| toCheck.equalsIgnoreCase("LPT6") || toCheck.equalsIgnoreCase("LPT7") || toCheck.equalsIgnoreCase("LPT8")
				|| toCheck.equalsIgnoreCase("LPT9"));
		return basicCheck || mediumCheck || advCheck || finalCheck;

	}

	/**
	 * Method to register user credentials only if parameters are respected
	 * 
	 * @param arrPhrase
	 *            String containing useful information to analyze and utilize
	 */
	private void registerFunction(String[] arrPhrase) {
		// Registrazione con 4 parole
		if (arrPhrase.length == 4) {
			String userName = arrPhrase[1], password = arrPhrase[2];
			if (!password.equals("") && password != null) {
				// If password and confirmpass are equals,user is not already registered &
				// doesn't contain illegal char User gets registered
				if (password.equals(arrPhrase[3]) && !credentialsExist(userName, USERFILE)
						&& !containsIllegalCharacter(userName)) {
					// Dectypt password before writing it
					password = decrypt(password, d, n);
					password = getSHA512(password);
					writeString(userName + " " + password, USERFILE);
					writeString("SUCCESS | User : " + userName + " successfully registered from : "
							+ s.getInetAddress().getHostAddress(), LOGFILE);
					File dir = new File(PATH + "/" + userName);
					if (dir.exists()) {
						purgeDirectory(dir);
					}
					dir.mkdir();
					new SenderHandler(s, "/OK");
				} else { // Write an error log
					new SenderHandler(s, "/NO");
					writeString("ERROR | Error Registering user from : " + s.getInetAddress().getHostAddress(),
							LOGFILE);
				}
			}
		}
	}

	/**
	 * 
	 * @param arrPhrase String array containing useful information to analyze and utilize
	 * @return true if every cell in the array was not null
	 */
	private boolean isNotNull(String[] arrPhrase) {
		// Controlla se tutte le celle del vettore in input sono diverse da nullo
		boolean check = true;
		for (int i = arrPhrase.length - 1; i >= 0 && check; --i) {
			if (check = arrPhrase[i] == null) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Method used to write a String to a given File
	 * 
	 * @param data
	 *            String containing userName and password that is going to be saved
	 *            into the file
	 * @param fileName
	 *            String representing the path that is going to be used
	 */
	private synchronized void writeString(String data, String fileName) {
		try {
			pw = new PrintWriter(new FileOutputStream(new File(fileName), true /* append = true */));
			//String Data gets saved into file
			pw.println(data);
		} catch (FileNotFoundException e) {
			System.out.println("Errore nella manipolazione del file richiesto");
		} finally {
			pw.close();
		}
		pw.flush();
		pw.close();
	}

	/**
	 * Method used to check if userName is already saved into the file
	 * 
	 * @param userName
	 *            String containing userName to check
	 * @param fileName
	 *            String representing the path that is going to be used
	 * @return true if the userName is already saved into the file
	 */
	private synchronized boolean credentialsExist(String userName, String fileName) {
		BufferedReader reader = null;
		try {
			writeString("OPERATION | Check credentials requested for registere from : " +s.getInetAddress().getHostAddress(), LOGFILE);
			reader = new BufferedReader(new FileReader(new File(fileName)));
			// Read a line from the file
			String fileLine = reader.readLine();
			String[] arrPhrase;
			while (fileLine != null) {
				arrPhrase = fileLine.split(" ");
				// If every word read from the file is not null and the first one is equals to input string
				// it return true
				if (isNotNull(arrPhrase) && arrPhrase[0].equalsIgnoreCase(userName)) {
					return true;
				}
				fileLine = reader.readLine();
			}
		} catch (IOException e) {
			writeString("EXCEPTION | Error reading from File ", LOGFILE);
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				writeString("EXCEPTION | Error closing File stream ", LOGFILE);
			}
		}
		return false;

	}

	/**
	 * Method used to check if the credentials are correct
	 * 
	 * @param userName
	 *            String that indicates user name
	 * @param password
	 *            String that indicates user password
	 * @param fileName
	 *            String representing the path that is going to be used
	 * @return true if parameters in input are equals to parameters into the file
	 */
	private synchronized boolean credentialsExist(String userName, String password, String fileName) {
		writeString("OPERATION | Check credentials requested for login from : " +s.getInetAddress().getHostAddress(), LOGFILE);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(new File(fileName)));
			// Read one line from file
			String fileLine = reader.readLine();
			String[] arrPhrase;
			while (fileLine != null && fileLine.contains(" ")) {
				arrPhrase = fileLine.split(" ");
				// if phrase is made up of 2 words and is not null
				if (arrPhrase.length == 2 && isNotNull(arrPhrase)) {
					// If inputed string userName is equals to the first word read
					if (arrPhrase[0].equalsIgnoreCase(userName)) {
						// If inputed string password is equals to the second word read
						if (arrPhrase[1].equals(password)) {
							return true;
						}
						return false;
					}
				}
				fileLine = reader.readLine();
			}
		} catch (IOException e) {
			writeString("EXCEPTION | Error setting File input stream ", LOGFILE);
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				writeString("EXCEPTION | Error closing File stream  ", LOGFILE);
			}
		}
		return false;
	}
	
	
	/**
	 * Method used to send public key to client when connection is established
	 * @return true if key is exchanged successfully
	 * @throws IOException if there are problems with the connection
	 */
	private boolean keyExchange () throws IOException{ 
		
		while(!listenForRequest("/KEY"));

		boolean keyGenCorr = false;
		
		KeyGenerator keyG = null;
		
		Vector<BigInteger> publicKeys = null;
		
		while (!keyGenCorr) {
			
			
			keyG = new KeyGenerator();
			try {
				keyG.join();
			} catch (InterruptedException e) {
				return false;
			}
			if (keyG!=null && (publicKeys=keyG.getPublicKeys())!=null) {
			
				keyGenCorr = !publicKeys.isEmpty() && publicKeys.size()==2 && publicKeys.get(0)!=null && publicKeys.get(1)!=null;
				
			}
			
		}
		try {
			sendKeys(publicKeys);
		} catch (IOException e) {
			return false;
		}

		while (!listenForRequest("/KEYGOT"));
		
		
		Vector <BigInteger> privateKeys = keyG.getPrivateKeys();
		d=privateKeys.get(0);
		n=privateKeys.get(1);
		
		return true;
	}
	
	/**
	 * method used to listen to a certain command from stream
	 * @param request String to listen to from stream
	 * @return true if expected request is received
	 * @throws IOException in case problems with streams are encountered
	 */
	private boolean listenForRequest(String request) throws IOException{
		String input = null;
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			input = br.readLine();
		}catch (IOException e) {
			return false;
		}
			if (input != null) {
				if (input.equalsIgnoreCase(request)) {
					return true;
					//If client wants to end key exhange exception is thrown and socket later closed
				}else if (input.equalsIgnoreCase("/NO")){
					throw new IOException();
				}

			}
		return false;
	}
	
	/**
	 * Method used to send on stream previously generated public keys
	 * @param publicKeys Vector that contains two keys
	 * @throws IOException if there are problems with the stream
	 */
	private void sendKeys(Vector<BigInteger> publicKeys) throws IOException{
		if (s!=null) {
			ObjectOutputStream outS = new ObjectOutputStream(sftp.getOutputStream());
			if (publicKeys != null) {
				outS.reset();
				outS.writeObject(publicKeys);
				outS.flush();
			} else {
				throw new IOException();
			}
		}else {
			throw new IOException();
		}	
	}
	
	/**
	 * Method used to decrypt input data using private keys
	 * @param data String to decrypt
	 * @param d private key
	 * @param n private key
	 * @return the message decrypted
	 */
	private String decrypt(String data, BigInteger d, BigInteger n) {

		// This method casts String into int,it converts it and cast into string
		if (data.length() >= 4) {
			String wordCount = data.substring(data.length() - 2);
			int count = data.length() - data.replace(";", "").length();
			if (Integer.parseInt(wordCount) == count) {
				
				data = data.substring(0, data.length() - 2);
				
				StringTokenizer dama = new StringTokenizer(data, token);

				Vector<BigInteger> intVector = new Vector<BigInteger>();

				StringBuilder stringList = new StringBuilder();

				while (dama.hasMoreTokens()) {
					data = dama.nextToken();
					if (!data.isEmpty()) {
						// Convert String into BigInteger
						BigInteger i = new BigInteger(data);
						BigInteger m = i.modPow(d, n);
						intVector.add(m);
						stringList.append((char) m.longValueExact());
					} else {
						stringList.append("\n");
					}
				}

				// System.out.println("DECRYPTED --> " + intVector);
				// System.out.println("CHAR : DECRYPTED --> " + stringList);

				return stringList.toString();
			}else {
				writeString("ERROR | Data not encrypted correctly!  ", LOGFILE);
			}
		}
		return "";
	}
	
	/**
	 * Method used to delete periodically files in directory
	 * @param dir directory to purge
	 * @return true if directory is correctly deleted
	 */
	private boolean purgeDirectory(File dir) {
		boolean deletedAll = true;
		for (File file : dir.listFiles()) {
			if (file.isDirectory())
				purgeDirectory(file);
			if (deletedAll) {
				deletedAll = file.delete();
			} else {
				file.delete();
			}
		}
		return deletedAll;
	}
	
	/**
	 * Method used to convert given data into an hashed string
	 * @param data String to hash
	 * @return hashed string
	 */
	private String getSHA512(String data) {
		String generatedPassword = null;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			// Update the digest with given byte array
			md.update(data.getBytes(StandardCharsets.UTF_8));
			// Digest data
			byte[] bytes = md.digest(data.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			// Build string
			for (int i = 0; i < bytes.length; i++) {
				sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			generatedPassword = sb.toString();
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Error implementing algorithm");
		}
		return generatedPassword;
	}
}
