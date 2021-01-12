import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;

public class Client {
    public int PORT              = 6060; 
    private DataOutputStream out = null;
    private DataInputStream in   = null;
    private Socket socket        = null;

    Client() throws IOException {
        System.out.println("Client is ready.\nConnecting to the server........");
        try {
            socket = new Socket("localhost", PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (ConnectException ce) {
            System.out.println("Server not found.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        String s = in.readUTF();
        if (s.equals("connected")) System.out.print("Connected.\n\n> ");
    }

    public void start() {
        new clientInput();
        new clientOutput();
    }
    
    public static void main(String[] args) throws IOException {
        try {
            new Client().start();
        } catch (NullPointerException npe) {
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class clientInput extends Thread {
        clientInput() {
            this.start();
        }

        public void run() {
            try {
                while (true) {
                    String input = in.readUTF();
                    int len = input.length();
                    input = input.substring(0, len - 1);
                    System.out.print("Server: " + input + "\n> ");
                }
            } catch (EOFException eofe) {
                try {
                    in.close();
                    out.close();
                    return;
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
            } catch (SocketException se) {
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    } 

    private class clientOutput extends Thread {
        private BufferedReader bufReader = null;
        
        clientOutput() {
            bufReader = new BufferedReader(new InputStreamReader(System.in));
            this.start();
        }

        public void run() {
            String input;
            try {
                while (true) {
                    input = bufReader.readLine().trim();
                    if (input.equals("exit")) {
                        System.out.println("\nClient closed.");
                        bufReader.close();
                        in.close();
                        out.close();
                        socket.close();
                        break;
                    } else {
                        out.writeUTF(input);
                        System.out.print("> ");
                    }
                }
            } catch (IOException ioe) {
                System.out.println("\nConnection lost.\nServer not found.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}