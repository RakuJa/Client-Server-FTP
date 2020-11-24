package Server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Class used to handle receiving file from clients
 * @author Giachetto Daniele
 *
 */
public class UploadHandler extends Thread{
	
	private Socket socketFTP = null;
	private Socket commandSocket = null;
	private final String PATH;
	private static final String logFile = ServerDFTP.getLogFile();

	
	
	/**
	 * Method used to receive files from a client
	 * @param path directory in which to store the file received
	 * @param socketFTP object socket used to get objects from stream
	 * @param commandSocket object socket used to get commands from stream
	 */
	public UploadHandler (String path,Socket socketFTP,Socket commandSocket) {
		writeString("OPERATION | Upload requested from : " + socketFTP.getInetAddress().getHostAddress() , logFile);
		this.socketFTP = socketFTP;
		this.commandSocket = commandSocket;
		this.PATH = path+"/";
		File file = new File(PATH);
		if(!file.isDirectory()) {
			file.mkdirs();
		}
		start();
	}
	

	@Override
	public void run() {

		ObjectInputStream inputS = null;
		try {
			inputS = new ObjectInputStream(socketFTP.getInputStream());
		} catch (IOException e) {
			writeString("EXCEPTION | Error setting up input stream from : " + socketFTP.getInetAddress().getHostAddress() , logFile);	
		}
		
		File inputFile = null;
		ZipFile zip = null;
		try {
			inputFile = (File) inputS.readObject();
			if (inputFile != null && inputFile.canRead()) {
				try {
					zip = new ZipFile(inputFile);
					extractFolder(zip);
				} catch (ZipException e) {
					writeString("EXCEPTION | Error casting file to zip, illegal file and client from : " + socketFTP.getInetAddress().getHostAddress() , logFile);
					new SenderHandler(commandSocket, "/NO");
				}
			}else {
				new SenderHandler(commandSocket, "/NO");
			}
		} catch (ClassNotFoundException e) {
			writeString("EXCEPTION | Error casting | Corrupted file from : " + socketFTP.getInetAddress().getHostAddress() , logFile);
			new SenderHandler(commandSocket, "/NO");
		} catch (IOException e) {
			writeString("EXCEPTION | Error receiving file from : " + socketFTP.getInetAddress().getHostAddress() , logFile);
			new SenderHandler(commandSocket, "/NO");
		} catch (NullPointerException e) {
			writeString("EXCEPTION | Error receiving file | Null Pointer from : " + socketFTP.getInetAddress().getHostAddress() , logFile);
			new SenderHandler(commandSocket, "/NO");
		}
		

	}
	
	/**
	 * Method used to copy file from an inputStream (Because it's a zipFile and not a file) to a file
	 * @param from original file stream that will be copied
	 * @param to destination file 
	 * @throws IOException Exception throw if file already exists or does not have permission to copy
	 */
	private void copyFile( InputStream from, File to ) throws IOException {
	    Files.copy( from, to.toPath() );
	}

	
	
	/**
	 * Method used to extract files from a zipFile
	 * @param zip zipFile from which will be extracted files
	 * @return true if folder is extracted successfully
	 */
	private boolean extractFolder(ZipFile zip) {

		boolean extractedSuccessfully = true;
		// Composes into entries
		Enumeration<?> zipFileEntries = zip.entries();
		BufferedInputStream is;
		// Process each entry
		while (zipFileEntries.hasMoreElements()) {
			// grab a zip file entry
			ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
			String currentEntry = entry.getName();
			File destFile = new File(PATH, currentEntry);
			if (!entry.isDirectory() && !destFile.exists() && extractedSuccessfully) {
				try {
					is = new BufferedInputStream(zip.getInputStream(entry));
					destFile.getParentFile().mkdirs();
					copyFile(is, destFile);
				} catch (IOException e) {
					extractedSuccessfully = false;
					writeString("EXCEPTION | Error unzipping file :" + destFile.getName()+ " from : " + socketFTP.getInetAddress().getHostAddress() , logFile);
				}
			}

			/**
			 * chiamata ricorsiva per unzippare un file,tralasciamola if
			 * (currentEntry.endsWith(".zip")) {
			 * System.out.println(currentEntry); extractFolder(new
			 * ZipFile(destFile)); }
			 **/
		}
		try { //Closing zipfile,if you don't do that it will lock the resource making it impossible to delete from local client
			zip.close();					
		} catch (IOException e) {
			writeString("EXCEPTION | Error closing file : " + zip.getName() + " from : " + socketFTP.getInetAddress().getHostAddress() , logFile);
		}
		if (extractedSuccessfully) {
			new SenderHandler(commandSocket, "/OK");
			writeString("SUCCESS | Successfully extraced file from : " + socketFTP.getInetAddress().getHostAddress() , logFile);
			return true;
		}else {
			new SenderHandler(commandSocket, "/NO");
			writeString("EXCEPTION | Error extracting file from : " + socketFTP.getInetAddress().getHostAddress() , logFile);
			return false;
		}
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
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileOutputStream(new File(fileName), true /* append = true */));
			// viene salvato il messaggio nel file dopo aver aperto lo stream dati
			pw.println(data);
		} catch (FileNotFoundException e) {
			System.out.println("Errore nella manipolazione del file richiesto");
		} finally {
			pw.close();
		}
		pw.flush();
		pw.close();
	}
	

}




