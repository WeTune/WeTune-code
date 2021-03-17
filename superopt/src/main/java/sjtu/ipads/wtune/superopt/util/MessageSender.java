package sjtu.ipads.wtune.superopt.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;

public class MessageSender {
  private final Inet4Address dest;
  private final int port;
  private final String appName;
  private final DatagramSocket sock;

  public MessageSender(Inet4Address dest, int port, String appName, DatagramSocket sock) {
    this.dest = dest;
    this.port = port;
    this.appName = appName;
    this.sock = sock;
  }

  public static MessageSender make(Inet4Address dest, int port, String appName) {
    try {
      final DatagramSocket sock = new DatagramSocket();
      Runtime.getRuntime().addShutdownHook(new Thread(sock::close));
      return new MessageSender(dest, port, appName, sock);
    } catch (Exception ex) {
      return null;
    }
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
}
