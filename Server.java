import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class Server {
	private static ServerSocket serverSocket = null;
	private static Socket clientSocket = null;

	static final int maxClientsCount = 20;
	private static final ClientInstance[] threads = new ClientInstance[maxClientsCount];
	static Object lock = new Object();

	public static void main(String args[]) {
		ClientInstance.chatRooms.put("chat", new ArrayList<String>());
		ClientInstance.chatRooms.put("java", new ArrayList<String>());
		int portNumber = 2222;
		if (args.length < 1) {
			
			System.out.println("Welcome to the XYZ chat server");
		} else {
			portNumber = Integer.valueOf(args[0]).intValue();
		}

		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.out.println(e);
		}

		while (true) {
			try {
				clientSocket = serverSocket.accept();
				int i = 0;
				for (i = 0; i < maxClientsCount; i++) {
					if (threads[i] == null) {
						(threads[i] = new ClientInstance(clientSocket, threads, "default")).start();
						break;
					}
				}
				if (i == maxClientsCount) {
					PrintStream os = new PrintStream(clientSocket.getOutputStream());
					os.println("Server too busy. Try later.");
					os.close();
					clientSocket.close();
				}
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	}
}

class ClientInstance extends Thread {

	private String clientName = null;
	private BufferedReader is = null;
	private PrintStream os = null;
	private Socket clientSocket = null;
	private final ClientInstance[] threads;
	private String room = null;

	public ClientInstance(Socket clientSocket, ClientInstance[] threads, String room) {
		this.clientSocket = clientSocket;
		this.threads = threads;
		this.room = room;
	}

	static HashSet<String> userNames = new HashSet<String>();
	static HashMap<String, ArrayList<String>> chatRooms = new HashMap<String, ArrayList<String>>();

	public void run() {
		final int maxClientsCount = Server.maxClientsCount;
		ClientInstance[] threads = this.threads;

		try {
			is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			os = new PrintStream(clientSocket.getOutputStream());
			String name;
			os.println("Login Name?");
			while (true) {
				name = is.readLine().trim();
				if (userNames.contains(name)) {
					os.println("Sorry, name taken");
					os.println("Login Name?");
				} else {
					synchronized (Server.lock) {
						userNames.add(name);
					}

					break;
				}
			}

			os.println("Welcome " + name);
			os.println("List of commands: ");
			os.println("/rooms to see all the chat rooms available");
			os.println("/join <name_chat_room> to enter a chat room");
			os.println("/create <name_chat_room> to create a chat room");
			os.println("/leave <name_chat_room> to leave the chat room");
			os.println("/@<username> to send a private message to the user");
			os.println("/quit to exit the chat application");
			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] == this) {
						clientName = name;
						break;
					}
				}
			}
			while (true) {
				String line = is.readLine();
				if (line.startsWith("/quit")) {
					synchronized (this) {
						String temp = this.room;
						if (!temp.equals("default")) {
							ArrayList<String> list = chatRooms.get(temp);
							list.remove(this.clientName);
							chatRooms.put(temp, list);
						}
						userNames.remove(name);
					}
					break;
				}
				if(line.startsWith("/commands")){
					synchronized (this) {
						this.os.println("List of commands: ");
						this.os.println("/rooms to see all the chat rooms available");
						this.os.println("/join <name_chat_room> to enter a chat room");
						this.os.println("/create <name_chat_room> to create a chat room");
						this.os.println("/leave <name_chat_room> to leave the chat room");
						this.os.println("/quit to exit the chat application");
					}
				}
				if (line.startsWith("/leave")) {
					synchronized (this) {
						if (!this.room.equals("default")) {
							for (int i = 0; i < maxClientsCount; i++) {
								if (threads[i] != null && threads[i] != this && this.room.equals(threads[i].room) && threads[i].clientName != null) {
									threads[i].os.println("*** User has left chat: " + name);
								}
							}
							String temp = this.room;
							ArrayList<String> list = chatRooms.get(this.room);
							list.remove(this.clientName);
							chatRooms.put(this.room, list);
							this.room = "default";
							this.os.println("You have left chat room: " + temp);
						} else {
							this.os.println("You are not in any chat room. This command does not make a difference.");
						}

					}

				}
				if (line.startsWith("/rooms")) {
					synchronized (this) {
					for (int i = 0; i < maxClientsCount; i++) {
						if (threads[i] != null && threads[i] == this) {
							threads[i].os.println("Active rooms are:");
							Iterator it = chatRooms.entrySet().iterator();
							while (it.hasNext()) {
								Map.Entry pair = (Map.Entry) it.next();
								ArrayList v = (ArrayList) pair.getValue();
								threads[i].os.println(pair.getKey() + "(" + v.size() + ")");
							}
						}
					
					}}
				}
				if (line.startsWith("/create")) {
					String[] input = line.split("\\s");
					synchronized (this) {
						if (input.length > 2) {
							this.os.println("Chat room name should not have spaces ");
						} else {
							if (this.room.equals("default")) {
								this.room = input[1];
								ArrayList<String> list = new ArrayList<String>();
								list.add(this.clientName);
								chatRooms.put(input[1], list);
								this.os.println("Chat room " + input[1] + " created and you were added successfully");
							} else {
								for (int i = 0; i < maxClientsCount; i++) {
									if (threads[i] != null && threads[i] != this && this.room.equals(threads[i].room) && threads[i].clientName != null) {
										threads[i].os.println("*** User has left chat: " + name);
									}
								}
								String temp = this.room;
								ArrayList<String> list = chatRooms.get(this.room);
								list.remove(this.clientName);
								chatRooms.put(this.room, list);
								this.room = input[1];
								ArrayList<String> list1 = new ArrayList<String>();
								list1.add(this.clientName);
								chatRooms.put(input[1], list1);
								this.os.println("Chat room " + input[1] + " created and you were added successfully");
							}

						}
					}
				}
				if (line.startsWith("/delete")) {
					String[] input = line.split("\\s");
					synchronized (this) {
						ArrayList<String> list = chatRooms.get(input[1]);
						if (this.room.equals(input[1])) {
							if (list == null || list.size() == 0) {
								this.os.println("No such chat room exist");
							} else {
								for (int i = 0; i < maxClientsCount; i++) {
									if (threads[i] != null && threads[i].room.equals(input[1]) && threads[i].clientName != null && threads[i] != this) {
										threads[i].os.println("This chat room is deleted");
										threads[i].room = "default";
									}
								}
								chatRooms.remove(input[1]);
								this.os.println("Chat room deleted");
							}
						} else {
							this.os.println("Only members of this chat room can delete it");
						}

					}
				}
				if (line.startsWith("@")) {

					String[] words = line.split("\\s", 2);
					if (words.length > 1 && words[1] != null) {
						words[1] = words[1].trim();
						if (!words[1].isEmpty()) {
							synchronized (this) {
								String clientName = words[0].substring(1);
								for (int i = 0; i < maxClientsCount; i++) {
									if (threads[i] != null && threads[i] != this && threads[i].clientName != null && threads[i].clientName.equals(clientName)) {
										threads[i].os.println("< Private message from" + name + "> " + words[1]);
										this.os.println("> Private message sent to " + clientName + "> " + words[1]);
										break;
									}
								}
							}
						}
					}
				} else if (line.startsWith("/join")) {
					String[] input = line.split("\\s");
					int flag = 0;
					synchronized (Server.lock) {
						if (chatRooms.containsKey(input[1])) {
							for (int i = 0; i < maxClientsCount; i++) {
								if (threads[i] != null && threads[i] == this && this.room.equals("default")) {
									threads[i].room = input[1];
									if (flag < 1) {
										this.os.println("entering room: " + input[1]);
										flag += 1;
									}
									ArrayList<String> temp = chatRooms.get(input[1]);

									temp.add(this.clientName);
									chatRooms.put(input[1], temp);

									for (int j = 0; j < temp.size(); j++) {
										this.os.println(temp.get(j));
									}
									this.os.println("end of list");
								} else {
									if (threads[i] == this) {
										this.os.println("You can only join in one chat room. Please use /leave to exit this chat room and join another");
									}

								}

							}
							for (int i = 0; i < maxClientsCount; i++) {
								if (threads[i] != null && this.room.equals(threads[i].room) && !threads[i].clientName.equals(this.clientName)) {
									threads[i].os.println("* new user joined chat: " + this.clientName);
								}
							}
						} else {
							this.os.println("No such chat room exists. Create it to enter");
						}

					}
				} else {
					synchronized (this) {
						if (!line.startsWith("/join") && !line.startsWith("/delete") && !line.startsWith("/create") && !line.startsWith("/rooms") && !line.startsWith("/leave")
								&& !line.startsWith("/quit") && !line.startsWith(" ")) {
							for (int i = 0; i < maxClientsCount; i++) {
								if (threads[i] != null && this.room.equals(threads[i].room) && threads[i] != this && threads[i].clientName != null) {
									threads[i].os.println("<" + name + "> " + line);
								}
							}
						}

					}
				}
			}

			os.println(" BYE " + name);

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
	}

}
