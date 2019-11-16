package gbn_Protocol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
public class GBN_client implements Runnable {
    private static int seqnum = 15;

    private static int N = 8;
    private int choice;

    public GBN_client(int choice) {
        super();
        this.choice = choice;
    }

    // 发送数据部分
    private static int SendDataPort = 10241;
    private static int ReceiveAckPort = 10240;
    private static DatagramSocket SenderSocket;
    private static DatagramPacket SendDataPacket;
    private static DatagramSocket ReceiveAckSocket;
    private static DatagramPacket ReceiverAckPacket;
    private static int send_base = 0;
    private static int nextseqnum = 0;
    private static boolean flag = false;
    private static int timeout = 2;
    private static String filestring = new String();
    private static byte[] B;
    private static int team;

    private ScheduledExecutorService executor;

    /**
     * 开始计时或者重新计时，超时时间为2s
     */
    private void timerBegin() {
        TimerTask task = new TimerTask() {
            @Override public void run() {
                if (send_base >= Math.ceil(B.length / 1478) - 1) {
                    return;
                }
                try {
                    for (int i = send_base; i < nextseqnum; i++) {
                        byte[] tempb = getByteArray(i);
                        String temp = new String(tempb);
                        String s = new String(i % seqnum + ":" + temp);
                        byte[] data = s.getBytes();
                        DatagramPacket SenderPacket =
                                new DatagramPacket(data, data.length,
                                                   InetAddress.getLocalHost(),
                                                   SendDataPort);
                        SenderSocket.send(SenderPacket);
                        System.out.println(
                                "***重发分组:" + i % seqnum + "*** 重发的是第" + i + "个包***");
                        timerBegin();
                    }
                } catch (Exception e) {
                }
            }
        };
        if (!flag) {
            flag = true;
        } else {
            executor.shutdown();
            //timer.cancel();
            //timer.purge();
        }
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(task, timeout, timeout,
                                        TimeUnit.SECONDS);
    }

    /**
     * 结束计时
     */
    private void timerEnd() {
        if (flag) {
            //timer.purge();
            //timer.cancel();
            executor.shutdown();
            flag = false;
        }
    }

    /**
     * 读取文件，存入filestring和字节数组B中
     *
     * @param fileName
     */
    public static void readFileByLines(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            while ((tempString = reader.readLine()) != null) {
                filestring = filestring + tempString + "\r\n";
            }
            reader.close();
            B = filestring.getBytes();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
    }

    /**
     * 根据nextseqnum获得要传输的字节
     *
     * @param nextseqnum 要传输的分组序号
     * @return 字节数组，要传输的字节
     */
    private byte[] getByteArray(int nextseqnum) {
        byte[] temp = new byte[1478];
        for (int i = 0; i < 1478; i++) {
            if (nextseqnum * 1478 + i >= B.length) {
                break;
            }
            temp[i] = B[nextseqnum * 1478 + i];
        }
        return temp;
    }

    /**
     * 发送数据
     * 调用readfilebyline()读取文件
     * 同时也调用sendtoreceiver()和receiverACK()
     */
    public void send() {
        readFileByLines("test.txt");
        team = (int) Math.ceil(B.length / 1478);
        try {
            SenderSocket = new DatagramSocket();
            ReceiveAckSocket = new DatagramSocket(ReceiveAckPort);
            while (true) {
                SendToReciver();
                ReceiveACK();
                if (send_base >= team) {
                    break;
                }
            }
            //打印信息提示发送结束
            System.out.println("***********发送结束***********");
        } catch (Exception e) {
        }
    }

    /**
     * 发送数据给接收方
     * 要先给定序号，然后使用getByteArray()转回字节数组才能进行传输。并且要重新设定计时器
     * 在此函数中实现模拟数据包丢失功能
     */
    private void SendToReciver() {
        try {
            while (nextseqnum < send_base + N) {
                if (send_base >= team || nextseqnum >= team) {
                    break;
                }
                byte[] tempb = getByteArray(nextseqnum);
                String temp = new String(tempb);
                String s = new String(nextseqnum % seqnum + ":" + temp);
                byte[] data = s.getBytes();
                SendDataPacket = new DatagramPacket(data, data.length,
                                                    InetAddress.getLocalHost(),
                                                    SendDataPort);
                // 模拟数据包丢失
                if (nextseqnum % 5 != 0) {
                    SenderSocket.send(SendDataPacket);
                    System.out.println(
                            "***发送分组:" + nextseqnum % seqnum + "*** 发送的是第" +
                            nextseqnum + "个包***");
                } else {
                    System.out.println(
                            "***模拟分组" + nextseqnum % seqnum + "丢失***" + "*** 丢失的是第" +
                            nextseqnum + "个包***");
                }
                if (send_base == nextseqnum) {
                    timerBegin();
                }
                nextseqnum++;
                // System.out.println(nextseqnum);不打印
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收ACK
     *对send_base进行更新
     * 实现模拟ACK丢失功能
     * @throws InterruptedException
     */
    private void ReceiveACK() throws InterruptedException {
        try {
            if (send_base >= team) {
                return;
            }
            byte[] bytes = new byte[10];
            ReceiverAckPacket = new DatagramPacket(bytes, bytes.length);
            ReceiveAckSocket.receive(ReceiverAckPacket);
            String ackString = new String(bytes, 0, bytes.length);
            String acknum = new String();
            for (int i = 0; i < ackString.length(); i++) {
                if (ackString.charAt(i) >= '0' && ackString.charAt(i) <= '9') {
                    acknum += ackString.charAt(i);
                } else {
                    break;
                }
            }
            int ack = Integer.parseInt(acknum);
            // 模拟ACK丢包
            if (ack % 6 != 0) {
                System.out.println("***ACK" + ack);
                //send_base = Math.max(ack + 1, send_base);
                int m;
                //System.out.println(send_base);
                if (send_base % seqnum > ack &&
                    nextseqnum / seqnum > send_base / seqnum &&
                    ack <= (send_base + N) % N) {
                    m = send_base / seqnum * seqnum + ack + seqnum + 1;
                } else {
                    m = send_base / seqnum * seqnum + ack + 1;
                }
                send_base = Math.max(send_base, m);
            } else {
                System.out.println("***模拟ACK" + ack + "丢失***");
            }
            TimerReset();
            //System.out.println("base="+send_base);
            //System.out.println("next_seq="+nextseqnum);
        } catch (IOException e) {
        }
    }

    /**
     * 重置计时器
     */
    private void TimerReset() {
        if (send_base == nextseqnum) {
            timerEnd();
        } else {
            timerBegin();
        }
    }

    // 接收数据部分
    private static int SendAckPort = 10243;
    private static int ReceiveDataPort = 10242;
    private static DatagramSocket ReciverSocket;
    private static DatagramPacket SendAckPacket;
    private static int expectedSeqNum = 0;

    private static int last = -1;
    /**
     * 接收数据并发回ACK
     * 解析收到的ACK数据包并调用sendACKback()发回ACKseq
     */
    public void receive() {
        try {
            ReciverSocket = new DatagramSocket(ReceiveDataPort);
            while (true) {
                byte[] data = new byte[1472];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                ReciverSocket.receive(packet);
                byte[] d = packet.getData();
                String message = new String(d);
                String num = new String();
                for (int i = 0; i < message.length(); i++) {
                    if (message.charAt(i) <= '9' && message.charAt(i) >= '0') {
                        num = num + message.charAt(i);
                    } else {
                        break;
                    }
                }
                // 判断是否是顺序到达的
                if (expectedSeqNum == Integer.valueOf(num)) {
                    int ack = expectedSeqNum;
                    sendACKback(ack);
                    expectedSeqNum = (expectedSeqNum + 1) % seqnum;
                    last = ack;
                } else {
                    if (last >= 0) {
                        sendACKback(last);
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    /**
     * 发回ACK
     *
     * @param ack 发回的ACK序号，为0到N-1
     */
    private void sendACKback(int ack) {
        try {
            AckFrame ACK = new AckFrame(ack);
            SendAckPacket = new DatagramPacket(ACK.ackByte, ACK.ackByte.length,
                                               InetAddress.getLocalHost(),
                                               SendAckPort);
            ReciverSocket.send(SendAckPacket);
            System.out.println("***Send ACK" + ack + " Back***");
        } catch (Exception e) {
        }
    }

    @Override public void run() {
        if (choice == 0) {
            send();
        } else if (choice == 1) {
            receive();
        }
    }

    public static void main(String[] args) {
        new Thread(new GBN_client(0)).start();
        new Thread(new GBN_client(1)).start();
    }
}