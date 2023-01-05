package lab4.controller;

import lab4.protobuf.SnakesProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;

public class Receiver extends Thread implements Observer {

    DatagramSocket socket;
    NodeController controller;

    protected byte[] buf = new byte[8192];

    public Receiver(NodeController controller, DatagramSocket socket) {
        this.controller = controller;
        this.socket = socket;
        controller.register(this);
    }

    @Override
    public void run() {

        DatagramPacket dp = new DatagramPacket(buf, buf.length);

        while(!interrupted()){


            try {
                socket.receive(dp);
                byte[] actualPacket = new byte[dp.getLength()];
                System.arraycopy(dp.getData(), dp.getOffset(), actualPacket, 0, dp.getLength());
                controller.handle(new HostInfo(dp.getAddress(),dp.getPort()), actualPacket);


            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    @Override
    public void update() {
        for(var player : controller.players.entrySet()){
            var message = SnakesProto.GameMessage.newBuilder()
                    .setState(SnakesProto.GameMessage.StateMsg.newBuilder().setState(controller.gameState))
                    .setMsgSeq(controller.newSeqNum())
                    .setReceiverId(player.getValue())
                    .setSenderId(controller.myId)
                    .build();

                    var msg = message.toByteArray();
            var packet = new DatagramPacket(msg, msg.length, player.getKey().ip,player.getKey().port);
            try {
                socket.send(packet);
                System.out.println("sending gameStateMessage to " + player.getKey());

                if(!controller.messages.containsKey(player.getKey()))
                    controller.messages.put(player.getKey(), new ConcurrentHashMap<>());
                var messagesmap = controller.messages.get(player.getKey());
                messagesmap.put(message.getMsgSeq(), new AbstractMap.SimpleEntry<>(message, System.currentTimeMillis()));
                controller.lastMessageSentTime.put(player.getKey(), System.currentTimeMillis());


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void finalUpdate() {
        this.interrupt();
    }
}
