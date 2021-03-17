package sjtu.ipads.wtune.superopt.daemon;

public interface PacketHandler {
  void handle(byte[] packet);
}
