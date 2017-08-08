import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client implements Runnable {
	private static Socket clientSocket = null; // making client socket
	private static PrintStream os = null; // making output stream of client
	private static DataInputStream is = null; // making input for client
	private static BufferedReader inputLine = null; // making a buffer reader for to get input from user keyboard
	private static boolean closed = false;

	public static void main(String[] args) {
		String host = args[0];
		int portNumber = Integer.parseInt(args[1]);

		/*Open a socket on the given host and port number.
		 */
		try {
			clientSocket = new Socket(host, portNumber);
			inputLine = new BufferedReader(new InputStreamReader(System.in));
			os = new PrintStream(clientSocket.getOutputStream());
			is = new DataInputStream(clientSocket.getInputStream());
		} catch (UnknownHostException e) {} catch (IOException e) {}

		/*If client socket, input stream and output stream is being established, open connection on port.
		 */
		if (clientSocket != null && os != null && is != null) { 
			try {

				/* Create a thread to read from the server. */
				new Thread(new Client()).start();
				while (!closed) {
					os.println(inputLine.readLine().trim()); // while not closed, send the keyboard input to server
				}

				/* The program will come to this point when above while loop has already broken (which means
				 * the program is going to close the output stream, close the input stream, close the socket.
				 */
				os.close();
				is.close();
				clientSocket.close();
			} catch (IOException e) {}
		} //end  opening connection
	} // end main method


	/* Creating a thread to read from the server.
	 */
	@SuppressWarnings("deprecation")
	public void run() {
		/*reading from the socket in while the response from the server is not null and not equal to "logout".
		 */
		String responseLine;
		try {
			while ((responseLine = is.readLine()) != null ) {
				if (responseLine.equals("logout")) { break;} // when server replies "logout", break the close the client input from keyboard
				System.out.println(responseLine);
			}
			closed = true;
		} catch (IOException e) {}
	} // end run()
} // end client
