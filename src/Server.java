import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.HashSet;

public class Server {
    public int PORT                               = 6060;
    private static int clientCount                = 0;
    private ServerSocket serverSocket             = null;
    private Socket socket                         = null;
    private SimpleDateFormat formatter            = null;
    private static File file                      = null;
    private static FileWriter log                 = null;
    private static String serverIP                = null;
    private static String serverName              = null;
    private static HashSet<Server.client> clients = null;
    private static boolean running                = false;

    private class clientHandler {
        clientHandler() throws IOException {
            System.out.println("Server is starting........");
            formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            String fileName = "logs" + formatter.format(date).replaceAll("\\s", "").replaceAll("/", "").replaceAll(":", "") + ".txt";
            Server.file = new File(fileName);
            Server.file.createNewFile();
            Server.log = new FileWriter(Server.file);
            serverSocket = new ServerSocket(PORT);
            Server.running = true;
            clients = new HashSet<>();
            Server.serverIP = InetAddress.getLocalHost().getHostAddress();
            Server.serverName = InetAddress.getLocalHost().getHostName();
            System.out.print("Server started.\nServer: " + serverIP + ":6060/" + serverName + "\nServer logs: " + fileName + "\n\n> ");
            Server.log.write(formatter.format(date) + ": " + "Server started. Server: " + serverIP + ":6060/" + serverName + "\n");
        }

        private void start() {
            try {
                new serverRunning(clients);
                while (Server.running) {
                    socket = serverSocket.accept();
                    String clientIP = socket.getInetAddress().getHostAddress(), clientName = socket.getInetAddress().getHostName();
                    System.out.print("Client connected.\nClient ID: " + Server.clientCount + "." + " Client Name: " + clientName + "\n> ");
                    Server.log.write(formatter.format(new Date()) + ": " + "Client connected. Client ID: " + clientCount + "." + " Client IP: " + clientIP + "." + " Client Name: " + clientName + ".\n");
                    clients.add(new client(clientCount++, socket, clientIP, clientName));
                }
            } catch (Exception e) {
                try {
                    for (Server.client cli: clients)
                        cli.socket.close();
                    clients.clear();
                    try {
                        Server.log.close();
                    } catch(IOException ioe) {}
                    return;
                } catch (NullPointerException npe) {
                    return;
                } catch (Exception ec) {
                    System.out.println("Exception socket closing: ");
                    ec.printStackTrace();
                }
            }
        }
    }
    public static void main(String[] args) throws IOException {
        Server server = new Server();
        clientHandler clientHandler = server.new clientHandler();
        clientHandler.start();
    }

    private class client extends Thread {
        public int clientID         = 0;
        public String clientIP      = null;
        public String clientName    = null;
        public Socket socket        = null;
        private DataInputStream in  = null;
        public DataOutputStream out = null;

        client(int clientID, Socket socket, String clientIP, String clientName) throws IOException {
            this.clientID = clientID;
            this.clientIP = clientIP;
            this.clientName = clientName;
            this.socket = socket;
            this.in = new DataInputStream(this.socket.getInputStream());
            this.out = new DataOutputStream(this.socket.getOutputStream());
            this.start();
        }

        public void run() {
            try {
                out.writeUTF("connected");
                while (Server.running) {
                    String input = in.readUTF();
                    System.out.print("Client " + clientID + ": " + input + "\n> ");
                    Server.log.write(formatter.format(new Date()) + ": " + "Client " + clientID + " sent: " + input + "\n");
                }
            } catch (SocketException se) {
                return;
            } catch (EOFException eofe) {
                try {
                    clients.remove(this);
                    this.socket.close();
                    System.out.print("Client " + clientID + " closed connection.\n> ");
                    Server.log.write(formatter.format(new Date()) + ": " + "Client " + clientID + " closed connection.\n");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class serverRunning extends Thread {
        private BufferedReader bufReader       = null;
        private HashSet<Server.client> clients = null;
        
        serverRunning(HashSet<Server.client> clients) {
            bufReader = new BufferedReader(new InputStreamReader(System.in));
            this.clients = clients;
            this.start();
        }

        public void run() {
            try {
                while (Server.running) {
                    String input = bufReader.readLine().trim();
                    if (input.equals("exit")) {
                        Server.running = false;
                        System.out.println("\nServer closed.");
                        Server.log.write(formatter.format(new Date()) + ": Server closed.");
                        bufReader.close();
                        serverSocket.close();
                        return;
                    } else {
                        for (Server.client c: clients) c.out.writeUTF(input + "\n");
                        System.out.print("> ");
                    }
                }
            } catch (NullPointerException npe) {
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}