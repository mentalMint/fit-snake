package lab4.controller;

import com.google.protobuf.InvalidProtocolBufferException;
import lab4.protobuf.SnakesProto;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class GameFinder extends Thread{

    private MulticastSocket socket;
    private final byte[] buf = new byte[8192];
    public HashMap<HostInfo, SnakesProto.GameMessage.AnnouncementMsg> games = new HashMap<>();
    HashMap<HostInfo, Long> gamesttl = new HashMap<>();

    ArrayList<Observer> observers = new ArrayList<>();

    Timer ttltimer;

    @Override
    public void run() {
        ttltimer = new Timer();
        ttltimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (var game: gamesttl.entrySet()) {
                    if (game.setValue(game.getValue()-1) <= 0){
                        games.remove(game.getKey());
                        gamesttl.remove(game.getKey());
                        notifyObservers();
                    }
                }
            }
        }, 1000, 1000);

        try {

            this.socket = new MulticastSocket(9192);
            socket.joinGroup(InetAddress.getByName("239.192.0.4"));

            socket.setSoTimeout(5000);

        } catch (IOException e) {
            e.printStackTrace();
        }

        DatagramPacket dp = new DatagramPacket(buf, buf.length);
        while(!interrupted()){

            try {

                socket.receive(dp);
                handleMessage(dp);

            } catch (IOException e) {
            }

        }

        socket.close();


    }

    private void handleMessage(DatagramPacket dp) throws InvalidProtocolBufferException {
        byte[] actualPacket = new byte[dp.getLength()];
        System.arraycopy(dp.getData(), dp.getOffset(), actualPacket, 0, dp.getLength());

        SnakesProto.GameMessage message = SnakesProto.GameMessage.parseFrom(actualPacket);
        //System.out.println(message.toString());

        if (!message.hasAnnouncement()) return;

        var announcement = message.getAnnouncement();

        games.put(new HostInfo(dp.getAddress(),dp.getPort()),announcement);
        gamesttl.put(new HostInfo(dp.getAddress(),dp.getPort()), (long)5);
        notifyObservers();

    }

    public void register(Observer observer) {
        observers.add(observer);
    }

    private void notifyObservers(){
        for (Observer observer:observers){
            observer.update();
        }
    }

    public void sendDiscover(String host, String port) {
        var message = SnakesProto.GameMessage.newBuilder()
                .setDiscover(SnakesProto.GameMessage.DiscoverMsg.newBuilder().build())
                .setMsgSeq(0)
                .build();

        var msg = message.toByteArray();
        DatagramPacket packet = null;
        try {
            packet = new DatagramPacket(msg, msg.length, InetAddress.getByName(host), Integer.parseInt(port));
            socket.send(packet);
            System.out.println("sending discover message to " + host + ":" + port);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
