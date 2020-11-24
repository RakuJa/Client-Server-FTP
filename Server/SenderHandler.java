package Server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Class used to send file and data to clients
 * @author Giachetto Daniele
 *
 */
public class SenderHandler extends Thread{

	private Socket s;
	private String name;
	private String fileName;
	private static final String LOGFILE = ServerDFTP.getLogFile();
	private static final String PATH = ServerDFTP.getPath();
	private static final String FTPBIN = PATH + "/FTPBin";
	private int operation;
	private String output;
	

	/**
	 * Download service constructor
	 * @param name client name
	 * @param s object socket used to send the file
	 * @param fileName File to search and send
	 */
	public SenderHandler(String name,Socket s,String fileName) {
		this.operation = 0;
		//Salvataggio di variabili chiave
		this.name = name;
		this.s = s;
		this.fileName = fileName;
		if (s!=null) {
			writeString("OPERATION | Download requested from : " + s.getInetAddress().getHostAddress(), LOGFILE);
			start();
		}else {
			writeString("ERROR | Connection not established ", LOGFILE);
		}
		
		
	}
	

	/**
	 * Standard communication constructor
	 * @param name client Name
	 * @param s command socket used to send file list or disconnect
	 * @param listCheck boolean used to check if disconnection is requested or file list
	 */
	public SenderHandler(String name,Socket s,boolean listCheck) {
		
		//Salvataggio variabili chiave
		this.name = name;
		this.s = s;
		if (s != null) {

			//Se la variabile e' true allora e' stata richiesta dal client la lista dei file scaricabili
			if (listCheck) {
				this.operation = 1;
				writeString("OPERATION | Download list requested from : " + s.getInetAddress().getHostAddress(), LOGFILE);
				start();
				
			} else { //Se invece e' false allora e' stata richiesta la disconnessione
				this.operation = 2;
				writeString("OPERATION | Disconnection requested from : " + s.getInetAddress().getHostAddress(), LOGFILE);
				start();
			}
		} else {
			writeString("ERROR | Error connection not established ", LOGFILE);
		}
		
	}
	
	/**
	 * Faster client command server
	 * @param s command socket
	 * @param output string rapresenting data to send
	 */
	public SenderHandler(Socket s,String output) {
		
		writeString("OPERATION | YES/NO Comunication constructor called ", LOGFILE);

		this.name = "User";
		this.s = s;
		this.operation = 3;
		this.output = output;
		if (s != null) {
			start();
		} else {
			writeString("ERROR | Error connection not established ", LOGFILE);
		}
		
	}


	@Override
	public void run() {

		switch (operation) {
			
		case 0:
			output = null;
			// Create a file object of the file requested
			File file = new File(PATH + name + "/" + fileName + "/");
			// if file requested does not exist on the private account
			// folder,try on the generic folder
			if (!file.exists()) {
				file = new File(PATH + "User" + "/" + fileName + "/");
			}
			// check If files exists
			if (file.exists()) {
				// check if file requested is a file
				if (file.isFile()) {
					try {
						sendFile(file);
					} catch (IOException e) {
						writeString("EXCEPTION | Error sending message to user stream, from : "
								+ s.getInetAddress().getHostName(), LOGFILE);
					}
					// if requested file a directory
				} else {
					try {
						sendFile(compressDirectory(file));
					} catch (IOException e) {
						writeString("EXCEPTION | Error sending message to user stream, from : "
								+ s.getInetAddress().getHostName(), LOGFILE);
					}
				}
			} else {
				new SenderHandler(s, "/NO");
				writeString("ERROR | File requested not found ", LOGFILE);
			}
			break;
		case 1:
			output = getListFromFolder();
			break;
		case 2:
			output = "/EXIT";
			break;
		}
		if (output != null) {
			sendOutput(output);
		}

	}
	
	/**
	 * Method used to send file to output stream
	 * @param file file to send
	 * @throws IOException if problems with connection emerges
	 */
	private void sendFile(File file) throws IOException {
		if (s != null) {
			ObjectOutputStream outS = new ObjectOutputStream(s.getOutputStream());
			if (file != null) {
				outS.reset();
				outS.writeObject(file);
				outS.flush();
			} else {
				throw new IOException();
			}
		}else {
			throw new IOException();
		}
	}
	
	/**
	 * Method used to send strings to output stream
	 * @param out string to send
	 * @return true if successful
	 */
	private boolean sendOutput(String out) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
			pw.println(out);
			pw.flush();
		} catch (IOException e) {
			writeString("EXCEPTION | Error contacting client , request from : " + s.getInetAddress().getHostAddress(), LOGFILE);
			return false;
		}
		return true;
	}
	
	
	/**
	 * Method used to fetch from standard and user private directory a list of files
	 * @return a string containing the name of every file stored
	 */
	private String getListFromFolder() {
		StringBuilder output = new StringBuilder();
		//If user is logged in with a private account gets file from his directory
		if (!name.equalsIgnoreCase("User")) {
			File file = new File(PATH+name);
			File[] files = file.listFiles();
			if (files!=null && files.length>0 &&  files[0] != null) {
				output.append(new String("             List for user : " + name + ":"+"|").toUpperCase());
				for (File f : files) {
					output.append(f.getName()+"|");
				}
			}
		}
		//Takes file list from public directory
		File file = new File(PATH+"User");
		File[] files = file.listFiles();
		if (files!=null && files.length>0 &&  files[0] != null) {
			output.append(new String ("             List for generic user :" +"|").toUpperCase());
			for (File f : files) {
				output.append(f.getName()+"|");
			}
		}
		
		return output.toString();
		
	}
	
	/**
	 * Method used to delete periodicaly files in directory
	 * @param dir directory to purge
	 * @return true if directory is completely deleted
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
	 * Method used to prepare file to be compressed,it cleans
	 * the Bin directory that contains all the temp zipped file to send and then zips it
	 * @param file represent the directory to compress
	 * @return zipped file
	 */
	private File compressDirectory(File file) {
		try {
			//purge directory from all the files it contains
			purgeDirectory(new File(FTPBIN));
			//Create a zipped and temp file to hold directory to send
			File outputFile = new File(FTPBIN+"/" +s.getPort()+".zip");
			outputFile.createNewFile();
			
			//Compress directory
			compressDirectory(file.getAbsolutePath(), outputFile.getAbsolutePath());
			return outputFile;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}
	

	/**
	 * Method used to prepare streams for directory compression
	 * @param srcFolder Absolute path of directory to compress
	 * @param destZipFile Absolute path of destination zip file
	 * @throws IOException if problems with stream are encountered
	 */
	private void compressDirectory(String srcFolder, String destZipFile) throws IOException {
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
	private void addFileToZip(String path, String srcFile, ZipOutputStream zip) throws IOException {

		File folder = new File(srcFile);
		//If file is a directory it calls another method to handle the case
		if (folder.isDirectory()) {
			addFolderToZip(path, srcFile, zip);
			//Otherwise write the file into the destination path
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
	private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) throws IOException {
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
