package sjtu.ipads.wtune.support;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MessageSender {
  public static final String CONFIG_MSG_DEST = "wetune_address";
  public static final String CONFIG_MSG_PORT = "wetune_port";
  public static final String CONFIG_CONTEXT_NAME = "wetune_context";

  private final InetAddress dest;
  private final int port;
  private final String appName;
  private final DatagramSocket sock;
  private final BlockingQueue<String> queue;

  private static MessageSender instance;
  private static int failedInitialization;
  private static final int MAX_ATTEMPTS = 3;

  public MessageSender(InetAddress dest, int port, String appName, DatagramSocket sock) {
    this.dest = dest;
    this.port = port;
    this.appName = appName;
    this.sock = sock;
    this.queue = new ArrayBlockingQueue<>(1024);
  }

  public static MessageSender instance() {
    return instance;
  }

  public static MessageSender init(String dest, int port, String appName) {
    if (instance == null) {
      if (failedInitialization >= MAX_ATTEMPTS) return null;

      synchronized (MessageSender.class) {
        if (instance == null) {
          try {
            final DatagramSocket sock = new DatagramSocket();
            Runtime.getRuntime().addShutdownHook(new Thread(sock::close));

            final MessageSender sender =
                new MessageSender(Inet4Address.getByName(dest), port, appName, sock);

            final Thread thread = new Thread(sender::run);
            thread.setDaemon(true);
            thread.start();

            instance = sender;
          } catch (IOException ex) {
            failedInitialization++;
          }
        }
      }
    }

    return instance;
  }

  private byte[] makeMessage(String sql) {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream(sql.length());
    final DataOutputStream stream = new DataOutputStream(bytes);
    try {
      stream.writeByte(0x19);
      stream.writeByte(0x19);
      stream.writeInt(0);
      stream.writeUTF(appName);
      stream.writeUTF(sql);
    } catch (IOException ignored) {
      // should not throw exception
    }

    final byte[] byteArray = bytes.toByteArray();
    final int length = byteArray.length - 2 - 4;
    byteArray[2] = (byte) ((length >> 24) & 0xFF);
    byteArray[3] = (byte) ((length >> 16) & 0xFF);
    byteArray[4] = (byte) ((length >> 8) & 0xFF);
    byteArray[5] = (byte) ((length >> 0) & 0xFF);

    return byteArray;
  }

  private void run() {
    while (true) {
      try {
        sendMessage(queue.take());
      } catch (InterruptedException ex) {
        break;
      }
    }
  }

  public void sendMessage(String sql) {
    final byte[] msg = makeMessage(sql);
    final DatagramPacket packet = new DatagramPacket(msg, msg.length);
    packet.setAddress(dest);
    packet.setPort(port);
    try {
      sock.send(packet);
    } catch (IOException ignored) {
    }
  }

  public void enqueue(String sql) {
    queue.offer(sql);
  }
}
