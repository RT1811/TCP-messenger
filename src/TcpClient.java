import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TcpClient {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 5000);

        System.out.println("Connected to server");

        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();

        Thread receiver = new Thread(() -> {
            try {
                while (true) {
                    String message = TcpServer.readFrame(in);
                    System.out.println("Received: " + message);
                }
            } catch (EOFException e) {
                System.out.println("Server disconnected");
            } catch (IOException e) {
                System.out.println("Connection error: " + e.getMessage());
            }
        });

        receiver.start();

        BufferedReader console =
                new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            String message = console.readLine();

            if (message == null) {
                break;
            }

            sendFrame(out, message);
        }

        socket.close();
    }

    public static void sendFrame(OutputStream out, String message) throws IOException {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);

        int length = payload.length;

        out.write((length >>> 24) & 0xFF);
        out.write((length >>> 16) & 0xFF);
        out.write((length >>> 8) & 0xFF);
        out.write(length & 0xFF);

        out.write(payload);
    }
}
