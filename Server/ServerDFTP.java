package Server;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Main class server side
 * @author Giachetto Daniele
 *
 */
public class ServerDFTP {
	
	private final static int PORTA = 55555; 			//COMMAND PORT
	private static final int PORTAFTP = 55556;			//FTP PORT
	private final static String PATH = "H:/5B/FTP/FTPServer/";
	private final static String PATH2 = "H:/5B/FTP/FTPServer/User/";
	
	private static final String LOGFILE = "MessageLog.txt";
	private static final String USERFILE = "Credentials.txt";

	
	/**
	 * Main method used to accept connection
	 * @param arg used to pass extra information to main method
	 * @throws IOException in case the server has problems closing itself
	 */
	public static void main(String arg[]) throws IOException {
		File file = new File(PATH2);
		if(file.isDirectory()) {
			file.mkdir();
		}
		ServerSocket server = null;
		ServerSocket serverFTP = null;
		// Viene creato se non esiste il file contenente la lista degli ip bannati
		//PrintWriter pw = new PrintWriter(new FileOutputStream(new File("ipBanned.txt"), true /* append = true */));
		try {
			// Apertura del socket destinato ad accettare connessioni
			server = new ServerSocket(PORTA);
			serverFTP = new ServerSocket(PORTAFTP);
			System.out.println("Opening port...");
			while (true) {
				try {
					// Viene attesa ed accettata una connessione
					Socket s = server.accept();

					Socket sftp = serverFTP.accept();

					new DecisionHandler(s, sftp);
					// Se accettata viene assegnata ad un thread
					writeLog("SUCCESS | " + s.getInetAddress().getHostAddress());

				} catch (IOException e) {
					writeLog("FAILED | CONNECTION FAILED");
				}
				
			}
		} catch (IOException e) {
			// Se l'apertura della porta fallisce viene mostrato un messaggio d'errore
			System.out.println("Problems encountered opening port,Server closing..");
		} finally {
			// Vengono chiusi il server e l'oggetto PrintWriter
			server.close();
		}
	}
	
	/**
	 * Method used by all the other server-side class to get logFile destination
	 * @return String logFile containing path to log file
	 */
	public static String getLogFile() {
		return LOGFILE;
	}
	
	/**
	 * Method used by all the other server-side thread to get FilePath destination
	 * @return String PATH containing path to server directory
	 */
	public static String getPath() {
		return PATH;
	}
	
	/**
	 * Method used to write data into entrylog.txt,it also creates logs file
	 * @param ip client ip
	 */
	private static void writeLog(String ip) {
		PrintWriter logWriter = null;
		try {
			logWriter = new PrintWriter(new FileOutputStream(new File(USERFILE),
					true /* append = true */));
			logWriter = new PrintWriter(new FileOutputStream(new File("EntryLog.txt"),
					true /* append = true */));
			//viene salvato il messaggio nel file dopo aver aperto lo stream dati
			logWriter.println(ip);
		} catch (FileNotFoundException e) {
			System.out.println("Errore con la manipolazione del file richiesto");
		} finally {
			logWriter.close();
		}
		logWriter.flush();
		logWriter.close();
	}

}
