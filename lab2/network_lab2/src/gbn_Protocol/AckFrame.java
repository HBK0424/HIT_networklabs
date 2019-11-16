package gbn_Protocol;
public class AckFrame {
    protected int ACK;
    protected String ack;
    protected byte[] ackByte;

    public AckFrame(int ACK) {
        this.ACK=ACK;
        ack=String.valueOf(ACK);
        ackByte=ack.getBytes();
    }
}
