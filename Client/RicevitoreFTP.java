package Client;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JTextField;

/**
 * Class used to receive object from object stream
 * @author Giachetto Daniele
 *
 */
public class RicevitoreFTP extends Thread{
	
	
	private Socket s;
	private JTextField checkField;
	private final static String PATH = "H:/5B/FTP/FTPClient/";
	private static boolean isListening;
	private static ObjectInputStream inputS = null;
	
	/**
	 * Constructor used to set a listener for file sent by the server
	 * @param s object socket 
	 * @param checkField field used to display result or error
	 */
	public RicevitoreFTP(Socket s,JTextField checkField) {
		this.s = s;
		this.checkField = checkField;
		isListening = true;
		start();
	}
	
	@Override
	public void run() {
		try {
			while (isListening) {
				
				//Reinstance of ObjectInputStream to avoid infinite loop (Object saved into buffer stream will always skip readObject)
				inputS = new ObjectInputStream(s.getInputStream());

				File inputFile = null;
				try {
					System.out.println("In attesa del file");
					
					//Try to cast object to file,server only sends files
					inputFile = (File) inputS.readObject();
					System.out.println("File ricevuto");
				} catch (ClassNotFoundException e) {
					//Couldn't cast,strange object was sent
					checkField.setText("Error detected! You are trying to download a corrupted or malicious file!");
					checkField.setBackground(Color.RED);
					System.out.println("Errore nel casting | File corrotto(?)");
					continue;
				} catch (IOException e) {
					//Error during receive operation,probably file deleted from server while sending
					checkField.setText("Error downloading file!Check internet connection");
					checkField.setBackground(Color.RED);
					System.out.println("Problema rilevato nella ricezione del file");
					continue;
				}
				if (inputFile != null && inputFile.isFile()) {
					if (inputFile.getName().endsWith(".zip")) {
						ZipFile zip = new ZipFile(inputFile);
						if (extractFolder(zip)) {
						}else {
							try {
								Files.delete(Paths.get(inputFile.getAbsolutePath()));
							}catch (IOException e) {
								System.out.println("Error deleting corrupted file");
							}
						}
					} else if (inputFile.isFile()) {
						try {
							// Copy file received from buffer to memory
							copyFile(inputFile, new File(PATH + inputFile.getName()));
							// Set success
							checkField.setText("Operation was successful!");
							checkField.setBackground(Color.GREEN);
						} catch (IOException e) {
							// Set error
							checkField.setText("Error!File already downloaded?");
							checkField.setBackground(Color.RED);
							System.out.println("Errore nella manipolazione del file");
						}
					}
				}
				ClientFTP.setAllButtonsOn();
			}
			//Client is not listening anymore to server,fatal error somehow
			System.out.println("Object Receiver shutting down...");
			ClientFTP.setAllButtonsOn();
			close();
		} catch (IOException e) {
			checkField.setText("Stream error!");
			checkField.setBackground(Color.RED);
			System.out.println("Errore nella manipolazione del file");
			System.out.println("Problema rilevato con gli stream");
			System.out.println("Object Receiver shutting down...");
			ClientFTP.setAllButtonsOn();
			close();
		}
	}
	
	/**
	 * Method used to copy file from one to another
	 * @param from original file,file sent from server that will be copied
	 * @param to destination file
	 * @throws IOException In case file already exists or Does not have permissions
	 */
	private void copyFile( File from, File to ) throws IOException {
	    Files.copy( from.toPath(), to.toPath() );
	    System.out.println("File copiato!");
	}

	/**
	 * Method used to stop listening for file from server
	 */
	public static synchronized void close() {
		isListening = false;
	}
	
	/**
	 * Method used to check if client is still listening for file from server
	 * @return true if client is listening
	 */
	public static synchronized boolean isListening() {
		return isListening;
	}
	
	/**
	 * Method used to extract files from a zipFile
	 * @param zip zipFile from which will be extracted files
	 * @return true if folder is extracted successfully
	 */
	private synchronized boolean extractFolder(ZipFile zip) {
		boolean extractedSuccessfully = true;

		// Composes into entries
		Enumeration<?> zipFileEntries = zip.entries();
		BufferedInputStream is;
		// Process each entry
		while (zipFileEntries.hasMoreElements() && extractedSuccessfully) {
			// grab a zip file entry
			ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
			String currentEntry = entry.getName();
			File destFile = new File(PATH, currentEntry);
			if (!entry.isDirectory() && !destFile.exists()) {
				try {
					is = new BufferedInputStream(zip.getInputStream(entry));
					destFile.getParentFile().mkdirs();
					copyFile(is, destFile);
				} catch (IOException e) {
					extractedSuccessfully = false;
					System.out.println("Exception unzipping file");
				}
			}
		}
		try { //Closing zipfile,if you don't do that it will lock the resource making it impossible to delete
			if (extractedSuccessfully) {
				checkField.setText("Operation was successful!");
				checkField.setBackground(Color.GREEN);
				System.out.println("File copied successfully");
			}else {
				checkField.setText("Error downloading and extracting folder!");
				checkField.setBackground(Color.RED);
				System.out.println("File copied unsuccessfully");
			}
			zip.close();
		} catch (IOException e) {
			System.out.println("Error closing zipFile");
		}
		return extractedSuccessfully;
	}
	
	/**
	 * Method used to copy file from an inputStream (Because it's a zipFile and not a file) to a file
	 * @param from original file stream that will be copied
	 * @param to destination file 
	 * @throws IOException Exception throw if file already exists or does not have permission to copy
	 */
	private synchronized void copyFile( InputStream from, File to ) throws IOException {
	    Files.copy( from, to.toPath() );
	}
}
