import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.net.ServerSocket;


public class Server {
	private static ServerSocket serverSocket = null; // Defining the server socket.
	private static Socket clientSocket = null; 	// Defining the client socket.
	private static Map<String, String> dictionary = new HashMap<String, String>(); // this HashMap will stores the keys as usernames and value as passwords from credentials.txt
	private static Map<String, Long> authenticationHistory = new HashMap<String, Long>(); // this HashMap stores keys as usernames and IPs and values as the timestamp when it was blocked for 3 wrong entries.
	private static ArrayList <String> loginHistory = new ArrayList<String>(); // Stores (name +" " + login time stamp). This is used for whoelsesince.
	private static ArrayList <String> offlineMessages = new ArrayList<String>(); // this array list stores (name + " " + message) if the recipient is offline.
	private static ArrayList <String> onlineUsers = new ArrayList<String>(); // stores the online users to prevent a user from signing in from a different machine when it already online before.
	private static final int maxClientsCount = 30; // defines the maximum number of threads made for clients. I used this variable to iterate through client threads.
	private static final clientThread[] threads = new clientThread[maxClientsCount]; // this is the instance of client thread.

	public static void main(String args[]) throws IOException {
		int portNumber = Integer.parseInt(args[0]);
		int block_duration = Integer.parseInt(args[1]);
		int timeout = Integer.parseInt(args[2]);



		/* Read credentials.txt from current directory and put usernames and passwords from file to glabal HashMap "dictionary".
		 */
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(new BufferedReader(new FileReader("credentials.txt")));
		while (scanner.hasNextLine()) {
			String line[];
			String str=scanner.nextLine();
			line = str.split(" ");
			dictionary.put(line[0].trim(), line[1].trim());
		}


		/* Open a server socket on the portNumber given in the command line argument.
		 */
		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.out.println(e);
		}


		/* Create a client socket for each connection and pass it to a new client thread.
		 */
		while (true) {
			clientSocket = serverSocket.accept();
			int i = 0;
			for (i = 0; i < maxClientsCount; i++) {
				clientSocket.setSoTimeout(timeout*1000); // closes the connection from the client after the inactivity timeout.
				if (threads[i] == null) {
					(threads[i] = new clientThread(clientSocket, threads, dictionary, offlineMessages, 
							loginHistory, authenticationHistory, block_duration, onlineUsers)).start();
					break;
				}
			}
		}
	} // main
} // class server





/* 
 * 
 * 
 * 
 * This client thread opens the input and the output streams for a particular client 
 * 
 * 
 * 
 * 
 */
class clientThread extends Thread {
	private String clientName = null;
	private DataInputStream is = null;
	private PrintStream os = null;
	private Socket clientSocket = null;
	private final clientThread[] threads;
	private Map<String, String> dictionary;
	private Map<String, Long> authenticationHistory;
	private ArrayList<String> loginHistory;
	private int block_duration;
	private ArrayList <String> offlineMessages;
	private static ArrayList <String> onlineUsers;
	private int maxClientsCount;
	private ArrayList <String> blockedUsers = new ArrayList<String>(); // all online users


	// constructor
	public clientThread(Socket clientSocket, clientThread[] threads, Map<String, String> dictionary, 
			ArrayList <String> offlineMessages, ArrayList<String> loginHistory, Map<String, Long> authenticationHistory, 
			int block_duration, ArrayList <String> onlineUsers) {
		this.clientSocket = clientSocket;
		this.threads = threads;
		this.dictionary = dictionary;
		this.loginHistory = loginHistory;
		this.offlineMessages = offlineMessages;
		this.onlineUsers = onlineUsers;
		this.authenticationHistory = authenticationHistory;
		this.block_duration = block_duration;
		maxClientsCount = threads.length;
	} // end constructor clientThread


	@SuppressWarnings("deprecation")
	public void run() {
		int maxClientsCount = this.maxClientsCount;
		clientThread[] threads = this.threads;

		try {
			/*
			 * Create input and output streams for this client.
			 */
			is = new DataInputStream(clientSocket.getInputStream());
			os = new PrintStream(clientSocket.getOutputStream());

			/*validate username
			 */
			String name = "";
			int attempt1 = 0;
			String socketAddress = clientSocket.getRemoteSocketAddress().toString();
			String ipSplit[] = socketAddress.split(":");
			String ipAddress = ipSplit[0].trim();
			while (true) {
				os.println("Username: ");
				name = is.readLine().trim();
				// check if the username is already logged in from an other machine
				if (onlineUsers.contains(name)) {
					os.println("Login attempt is denied because this username is already logged in from another machine.");
					is.close();
					os.close();
					clientSocket.close();
				}

				// check if the username is being blocked for for block_duration due to wrong attempts before
				else if (authenticationHistory.containsKey(name) && (authenticationHistory.get(name) + block_duration*1000) >= System.currentTimeMillis()) {
					os.println("Your account is blocked due to multiple login failures. Please try again later");
					is.close();
					os.close();
					clientSocket.close();
				}
				// check if the IP is being blocked for for block_duration due to wrong attempts before
				else if (authenticationHistory.containsKey(ipAddress) && (authenticationHistory.get(ipAddress) + block_duration*1000) >= System.currentTimeMillis()) {
					os.println("Your IP is blocked due to multiple login failures. Please try again later");
					is.close();
					os.close();
					clientSocket.close();
				}

				// if username is right, break the while loop and go to password
				else if (dictionary.containsKey(name)) {
					break;
				} 

				// if username is being wrong for 2 consecutive times.
				else if(attempt1 == 2){
					os.println("Invalid Username. Your account has been blocked. Please try again later");
					authenticationHistory.put(ipAddress, System.currentTimeMillis()); // putting the current time and the name of client in the list to block it for block_duration
					is.close();
					os.close();
					clientSocket.close();
					break;
				}

				// if username is wrong for first or second time
				else {
					os.println("Invalid Username. Please try again");
					attempt1++;
				}
			} // end username authentication


			/*validate password
			 */
			String password;
			int attempt2 = 0;
			while (true) {
				os.println("Password: ");
				password = is.readLine().trim();

				// if password is right
				if (dictionary.get(name).equals(password)) {
					onlineUsers.add(name);
					loginHistory.add(name +" " + System.currentTimeMillis()); // this is for whoelsesince
					break;
				} 
				// if user make three consecutive wrong password attempts
				else if(attempt2 == 2){
					os.println("Invalid Password. Your account has been blocked. Please try again later");
					authenticationHistory.put(name, System.currentTimeMillis()); // putting the current time and the name of client in the list to block it for block_duration
					is.close();
					os.close();
					clientSocket.close();
					break;
				}
				// if user make first and second wrong attempt
				else {
					os.println("Invalid Password. Please try again");
					attempt2++;
				}
			} // end password authentication



			os.println("Welcome to the greatest messaging application ever!");

			/* checking and printing offline messages
			 */
			for (int i = 0; i < offlineMessages.size(); i++) { 
				String[] words = offlineMessages.get(i).split(" ", 3);
				if (name.equals(words[1])) {
					os.println(words[0]+": " + words[2]);
					//offlineMessages.remove(i);
				}
			} 

			// removing offline message being shown to client from database
			for (int i = 0; i < offlineMessages.size(); i++) { 
				String[] words = offlineMessages.get(i).split(" ", 3);
				if (name.equals(words[1])) {
					//os.println(words[0]+": " + words[2]);
					offlineMessages.remove(i);
				}
			}



			// running synchronized this
			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] == this) {
						clientName = name;
						blockedUsers = blockedUsers;
						break;
					}
				}
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] != this && (!threads[i].blockedUsers.contains(this.clientName))) {
						threads[i].os.println(name + " logged in");
					}
				}
			} // synchronized (this) ends


			/* CONVERSATION STARTING IN THE BELOW WHILE TRUE LOOP */
			while (true) {
				String line = is.readLine();
				/*logout the client if he types logoout*/
				if (line.startsWith("logout")) {
					onlineUsers.remove(this.clientName);
					this.os.println("logout");

					break;
				}

				/* blocking users by adding their names in blocked User list */
				else if (line.startsWith("block")) { 
					String[] words = line.split(" ", 2);

					synchronized (this) {
						if (this.clientName.equals(words[1])) {
							this.os.println("You can't block yourself");
						}
						else if (words[1].contains(" ")){
							this.os.println("Invalid command. Type 'block <username>'");

						}
						else {
							if (blockedUsers.contains(words[1])) {
								this.os.println(words[1] + " is already blocked");
							} else {
								blockedUsers.add(words[1]);
								this.os.println(words[1] + " is blocked");
								//this.os.println(blockedUsers.size());
							}
						}
					}
				}


				/* unblocking users by removing their names from blocked User list */
				else if (line.startsWith("unblock")) { 
					String[] words = line.split(" ", 2);

					synchronized (this) {
						if (this.clientName.equals(words[1])) {
							this.os.println("You can't  block/unblock yourself");
						} 
						else if (words[1].contains(" ")){
							this.os.println("Invalid command. Type 'unblock <username>'");

						}
						else {
							if (blockedUsers.contains(words[1])) {
								blockedUsers.remove(words[1]);
								this.os.println(words[1] + " is unblocked");
							} else {
								this.os.println(words[1] + " is not been blocked before");
							}
						}
					}
				}


				/* to send a private message to some one */
				else if (line.startsWith("message")) {
					String[] words = line.split(" ", 3);
					if (words.length > 2 && words[1] != null) {
						words[1] = words[1].trim();
						if (!words[1].isEmpty()) {
							synchronized (this) {
								// if receiver is not in dictionary OR receiver is client
								if (!(dictionary.containsKey(words[1])) || this.clientName.equals(words[1])){ 
									this.os.println("Error. Invalid user");
								}

								//online message
								else {
									for (int i = 0; i < maxClientsCount; i++) {
										if (threads[i] != null && threads[i] != this && threads[i].clientName != null && threads[i].clientName.equals(words[1])) {
											//System.out.println(Arrays.toString(threads[i].blockedUsers.toArray()));
											//flag = 1;
											if (threads[i].blockedUsers.contains(this.clientName)) {
												this.os.println("Your message could not be delivered as the recipient has blocked you");
											}
											else {
												threads[i].os.println(name + ": " + words[2]);
											}

											//flag = 1;
											break;
										}


									}
								}

								// offline message
								int flag = 0;
								if ((dictionary.containsKey(words[1])) && !(this.clientName.equals(words[1]))) { 
									for (int i = 0; i < maxClientsCount; i++) {
										if (threads[i] != null && threads[i].clientName != null && threads[i] != this) {
											if (threads[i].clientName.equals(words[1])) {
												flag = 1; // user is online and it name is same as word[0]

											}
										}
									}

								}
								if (flag == 0){
									offlineMessages.add(this.clientName.trim() + " " + words[1].trim() + " " + words[2].trim());
								}
							}
						}
					}
				}



				/* broadcast message */
				else if (line.startsWith("broadcast")) { 
					String[] words = line.split(" ", 2);
					synchronized (this) {
						for (int i = 0; i < maxClientsCount; i++) {
							if (threads[i] != null && threads[i].clientName != null && threads[i].clientName != name) {
								if (threads[i].blockedUsers.contains(this.clientName)) {
									this.os.println("Your message could not be delivered to some recipients");
								} else {
									threads[i].os.println(name + ": " + words[1]);
								}
							}
						}
					}
				}


				/* whoelsesince is online */
				else if (line.startsWith("whoelsesince")) { 
					String words[];
					words = line.split(" ");
					String time1 = words[1];
					int time = Integer.parseInt(time1);
					synchronized (this) {
						for (int i = 0; i < loginHistory.size(); i ++) {
							String split[] = loginHistory.get(i).split(" ");
							String name1 = split[0];
							String time2 = split[1];
							long loginTime = Long.parseLong(time2);
							System.out.println("loginTime" + loginTime);
							System.out.println("time" + time);
							System.out.println("current time" + System.currentTimeMillis());
							if (loginTime + time*1000 >= System.currentTimeMillis() && !(this.clientName.equals(name1))){
								this.os.println(name1);

							}
						}
					}
				}


				/* whoelse is online */
				else if (line.startsWith("whoelse")) { 
					synchronized (this) {
						for (int i = 0; i < maxClientsCount; i++) {
							if (threads[i] != null && threads[i].clientName != null && threads[i] != this) {
								this.os.println(threads[i].clientName);
							}
						}
					}
				} 

				
				/* if non of the above if's and else if's statements work, print "Error. Invalid command" */
				else {
					this.os.println("Error. Invalid command");
				}

			} // end of while true



			/* when user enter logout, while(true) loop broke (very first if statement) and the rest of the run() 
			 * executes and eventually client socket close*/
			//broadcasting the logout of a client
			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] != this && threads[i].clientName != null && (!threads[i].blockedUsers.contains(this.clientName))) {
						threads[i].os.println(name + " logged out");
					}
				}
			}

			/* point the thread of the this (current) client to null because its going to close*/
			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] == this) {
						threads[i] = null;
					}
				}
			}

			is.close();
			os.close();
			clientSocket.close();
		} catch (IOException e) {
		}
	} // end run()
} // clientThread