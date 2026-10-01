import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TcpServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(5000);

        System.out.println("Waiting for client 1...");
        Socket client1 = serverSocket.accept();
        System.out.println("Client 1 connected");

        System.out.println("Waiting for client 2...");
        Socket client2 = serverSocket.accept();
        System.out.println("Client 2 connected");

        System.out.println("Both clients connected");

        InputStream in1 = client1.getInputStream();
        OutputStream out1 = client1.getOutputStream();

        InputStream in2 = client2.getInputStream();
        OutputStream out2 = client2.getOutputStream();

        Thread client1ToClient2 = new Thread(() -> {
            forwardMessages(in1, out2, "Client 1");
        });

        client1ToClient2.start();

        try {
            forwardMessages(in2, out1, "Client 2");
        } finally {
            client1.close();
            client2.close();
            serverSocket.close();
        }
    }

    public static byte[] readExactly(InputStream in, int count) throws IOException {
        byte[] data = new byte[count];

        int totalRead = 0;

        while (totalRead < count) {
            int bytesRead = in.read(data, totalRead, count - totalRead);

            if (bytesRead == -1) {
                throw new EOFException("Stream ended before reading " + count + " bytes");
            }

            totalRead += bytesRead;
        }

        return data;
    }

    public static String readFrame(InputStream in) throws IOException {
        byte[] header = readExactly(in, 4);

        int length =
                ((header[0] & 0xFF) << 24)
                        | ((header[1] & 0xFF) << 16)
                        | ((header[2] & 0xFF) << 8)
                        |  (header[3] & 0xFF);

        if (length < 0 || length > 1024 * 1024) {
            throw new IOException("Invalid frame length: " + length);
        }

        byte[] payload = readExactly(in, length);

        return new String(payload, StandardCharsets.UTF_8);
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

    public static void forwardMessages(InputStream in, OutputStream out, String clientName) {
        try {
            while (true) {
                String message = readFrame(in);

                System.out.println(clientName + ": " + message);

                sendFrame(out, message);
            }
        } catch (EOFException e) {
            System.out.println(clientName + " disconnected");
        } catch (IOException e) {
            System.out.println(clientName + " connection error: " + e.getMessage());
        }
    }
}
