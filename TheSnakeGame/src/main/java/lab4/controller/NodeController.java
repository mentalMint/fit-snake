package lab4.controller;

import com.google.protobuf.InvalidProtocolBufferException;
import lab4.protobuf.SnakesProto;
import lab4.model.GameModel;
import lab4.model.PlayerView;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NodeController extends Thread{

    public ConcurrentHashMap<HostInfo, ConcurrentHashMap<Long, Map.Entry<SnakesProto.GameMessage, Long>>> messages = new ConcurrentHashMap<>();
    public ConcurrentHashMap<HostInfo, Long> lastMessageSentTime = new ConcurrentHashMap<>();
    public ConcurrentHashMap<HostInfo, Long> lastMessageReceivedTime = new ConcurrentHashMap<>();
    public ConcurrentHashMap<HostInfo, Integer> players = new ConcurrentHashMap<>();
    public ConcurrentHashMap<Integer, HostInfo> playerIds = new ConcurrentHashMap<>();

    public SnakesProto.NodeRole role;
    public int myId;

    private GameModel game;
    public SnakesProto.GameState gameState;
    private int lastState;
    private String gameName;
    private String playerName;

    private DatagramSocket socket;
    private Receiver receiver;

    private Timer frameTimer;
    private Timer announceTimer;
    private Timer pingTimer;
    InetAddress group;
    private HostInfo master = null;
    private int masterId = -1;
    private HostInfo deputy = null;
    private int deputyId = -1;

    private SnakesProto.GameConfig config;

    private int stateDelay;
    private int pingDelay;
    private int pingTimeout;

    ArrayList<Observer> observers = new ArrayList<>();

    public NodeController(SnakesProto.GameConfig config, String gameName, String playerName){
        this.config = config;
        this.gameName = gameName;
        stateDelay = config.getStateDelayMs();
        pingDelay = (int)(0.1*stateDelay);
        pingTimeout = (int)(0.8*stateDelay);
        this.game = new GameModel(config);
        this.role = SnakesProto.NodeRole.MASTER;
        myId = 0;
        this.playerName = playerName;
        game.addPlayer(SnakesProto.GamePlayer.newBuilder().setName(playerName).setId(myId).setScore(0).setType(SnakesProto.PlayerType.HUMAN).setRole(role).build());
        try {
            socket = new DatagramSocket();
            group = InetAddress.getByName("239.192.0.4");
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        setGameState(-1);
        startAnnounce(1000);

    }

    public NodeController(HostInfo address, SnakesProto.GameAnnouncement gameAnnouncement, String playerName, SnakesProto.NodeRole role) {
        this.config = gameAnnouncement.getConfig();
        this.gameName = gameAnnouncement.getGameName();
        stateDelay = config.getStateDelayMs();
        pingDelay = (int)(0.1*stateDelay);
        pingTimeout = (int)(0.8*stateDelay);
        this.game = null;
        this.role = null;
        myId = -1;
        try {
            socket = new DatagramSocket();
            group = InetAddress.getByName("239.192.0.4");
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.playerName = playerName;
        master = address;
        sendJoin(role);
    }


    private void startAnnounce(long del) {
        if (announceTimer == null){
            announceTimer = new Timer();
        }
        announceTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                var announcementMsg = SnakesProto.GameMessage.AnnouncementMsg.newBuilder()
                        .addGames(SnakesProto.GameAnnouncement.newBuilder()
                                .setCanJoin(true)
                                .setConfig(config)
                                .setGameName(gameName)
                                .setPlayers(gameState.getPlayers())
                                .build())
                        .build();
                var message = SnakesProto.GameMessage.newBuilder().setAnnouncement(announcementMsg).setMsgSeq(newSeqNum()).build();
                var msg = message.toByteArray();
                var packet = new DatagramPacket(msg, msg.length, group, 9192);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        },
                del, del);
    }

    @Override
    public void run() {

        receiver = new Receiver(this, socket);
        receiver.start();

        pingTimer = new Timer();
        pingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (var connection: lastMessageReceivedTime.entrySet()){
                    if ( (System.currentTimeMillis() - connection.getValue()) > pingTimeout){
                        if (role == SnakesProto.NodeRole.MASTER){
                            var pid = players.get(connection.getKey());
                            if (pid != null) {
                                game.makeZombie(pid);
                            }
                            if (deputyId == pid){
                                deputy = null;
                                deputyId = -1;
                            }
                        }
                        else if (role == SnakesProto.NodeRole.DEPUTY && connection.getKey().equals(master)){
                            System.out.println("Becoming a master");
                            role = SnakesProto.NodeRole.MASTER;
                            var lastmaster = master;
                            master = null;
                            var lastmasterid = masterId;
                            masterId = myId;
                            deputyId = -1;
                            deputy = null;

                            game = new GameModel(config, gameState, lastmasterid, myId);

                            for (var player : gameState.getPlayers().getPlayersList()){
                                try {
                                    if (player.getId() == lastmasterid || myId == player.getId()) continue;
                                    playerIds.put(player.getId(), new HostInfo(InetAddress.getByName(player.getIpAddress().substring(1)), player.getPort()));
                                    players.put(new HostInfo(InetAddress.getByName(player.getIpAddress().substring(1)), player.getPort()), player.getId());
                                } catch (UnknownHostException e) {
                                    e.printStackTrace();
                                }

                                var messagesmap = messages.get(lastmaster);
                                if(messagesmap != null){
                                    for (var message : messagesmap.entrySet()){
                                        if (message.getValue().getKey().hasSteer()){
                                            changeDirection(message.getValue().getKey().getSteer().getDirection());
                                        }
                                    }
                                    messages.remove(lastmaster);
                                }

                            }
                            startAnnounce(1000);
                            startFrameTimer();
                            chooseDeputy();

                        }
                        else if (master.equals(connection.getKey())){
                            if (deputy!=null){
                                var lastmaster = master;
                                master = deputy;
                                masterId = deputyId;
                                deputy = null;
                                deputyId = -1;
                                var messagesmap = messages.get(lastmaster);
                                if(messagesmap != null){
                                    for (var message : messagesmap.entrySet()){
                                        var msg = message.getValue().getKey().toByteArray();
                                        var packet = new DatagramPacket(msg, msg.length, master.ip, master.port);

                                        try {
                                            socket.send(packet);
                                            System.out.println("resending message to new master" + master);

                                            lastMessageSentTime.put(master, System.currentTimeMillis());
                                            var map = messages.put(master,new ConcurrentHashMap<>());
                                            map.put(message.getKey(), message.getValue());

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    messages.remove(lastmaster);
                                }

                            }
                            else{
                                notifyObserversFinal();
                                terminate();
                            }
                        }
                        players.remove(connection.getKey());
                        var messagesmap = messages.get(connection.getKey());
                        if (messagesmap != null) {
                            messagesmap.clear();
                        }
                        messages.remove(connection.getKey());
                        lastMessageSentTime.remove(connection.getKey());
                        lastMessageReceivedTime.remove(connection.getKey());
                        System.out.println(connection.getKey() + " disconnected");
                        if (role == SnakesProto.NodeRole.MASTER && deputy == null){
                            chooseDeputy();
                        }
                    }
                }

                for(var record: messages.entrySet()){
                    if (!players.containsKey(record.getKey())){
                        messages.remove(record.getKey());
                    }
                    for(var message: record.getValue().entrySet()){
                        if ((System.currentTimeMillis() - message.getValue().getValue()) < pingDelay){
                            continue;
                        }
                        var msg = message.getValue().getKey().toByteArray();
                        var packet = new DatagramPacket(msg, msg.length, record.getKey().ip, record.getKey().port);

                        try {
                            socket.send(packet);
                            System.out.println("resending " + message.getValue().getKey().getMsgSeq()+ " message to " + record.getKey());

                            lastMessageSentTime.put(record.getKey(), System.currentTimeMillis());
                            message.getValue().setValue(System.currentTimeMillis());

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (System.currentTimeMillis() - lastMessageSentTime.get(record.getKey()) >= pingDelay){
                        sendPing(record.getKey());
                    }
                }

            }
        }, pingDelay, pingDelay);




        if (role == SnakesProto.NodeRole.MASTER) {
            startFrameTimer();
        }
    }

    private void chooseDeputy() {
        System.out.println("Choosing a deputy");
        if (deputy!=null) return;
        for(var player : players.entrySet()){
            if (player.getValue() > 0){
                deputy = player.getKey();
                deputyId = player.getValue();
                game.snakes.get(deputyId).player = game.snakes.get(deputyId).player.toBuilder().setRole(SnakesProto.NodeRole.DEPUTY).build();
                return;
            }
        }
    }

    private void startFrameTimer() {
        if (frameTimer != null) frameTimer.cancel();
        frameTimer = new Timer();
        frameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int stateorder = game.newFrame();
                setGameState(stateorder);
            }
        }, config.getStateDelayMs(), config.getStateDelayMs());
    }

    private void setGameState(int stateorder) {
        var actualplayersBuilder = SnakesProto.GamePlayers.newBuilder();


        var gameStateBuilder = SnakesProto.GameState.newBuilder()
                .setStateOrder(stateorder);

        for (var i : game.food) {
            var food = SnakesProto.GameState.Coord.newBuilder().setX(i.x).setY(i.y).build();
            gameStateBuilder.addFoods(food);
        }
        for (var i : game.snakes.values()) {
            if (i.isDead) {
                if (i.isZombie) continue;
                sendRoleChange(i.playerId, SnakesProto.NodeRole.VIEWER);
                continue;
            }

            var snakebuilder = SnakesProto.GameState.Snake.newBuilder()
                    .setPlayerId(i.playerId)
                    .setHeadDirection(i.direction).setState(i.isZombie ? SnakesProto.GameState.Snake.SnakeState.ZOMBIE : SnakesProto.GameState.Snake.SnakeState.ALIVE)
                    .addPoints(SnakesProto.GameState.Coord.newBuilder().setX(i.getHead().x).setY(i.getHead().y));
            for (var item : i.getCoordinates()) {
                snakebuilder.addPoints(SnakesProto.GameState.Coord.newBuilder().setX(item.x).setY(item.y));
            }
            gameStateBuilder.addSnakes(snakebuilder);
            if (i.player != null) {
                actualplayersBuilder.addPlayers(SnakesProto.GamePlayer.newBuilder()
                        .setName(i.player.getName())
                        .setRole(i.player.getRole())
                        .setId(i.player.getId())
                        .setPort(i.player.getPort())
                        .setIpAddress(i.player.getIpAddress())
                        .setScore(i.score)
                        .build());
            }
        }

        gameState = gameStateBuilder.setPlayers(actualplayersBuilder).build();
        notifyObservers();
    }

    public void terminate() {
        if (pingTimer != null) pingTimer.cancel();
        if (announceTimer != null) announceTimer.cancel();
        if (frameTimer!=null) frameTimer.cancel();
        receiver.interrupt();
    }


    public void handle(HostInfo senderAddress, byte[] dp) {
        try {
            var message = SnakesProto.GameMessage.parseFrom(dp);

            if (lastMessageReceivedTime.get(senderAddress) != null) {
                lastMessageReceivedTime.put(senderAddress, System.currentTimeMillis());
            }

            if (message.hasAck()){

                if (messages.get(senderAddress) == null) {
                    System.out.println("Client not found");
                    return;
                }

                if (messages.get(senderAddress).get(message.getMsgSeq()) == null) {
                    System.out.println("Base message not found");
                    return;
                }
                var baseMessage = messages.get(senderAddress).get(message.getMsgSeq()).getKey();
                if (baseMessage == null){
                    System.out.println("Base message not found");
                    return;
                }
                messages.get(senderAddress).remove(message.getMsgSeq());
                if (baseMessage.hasJoin()){
                    lastMessageReceivedTime.put(senderAddress, System.currentTimeMillis());
                    System.out.println("received ack for join");
                    master = senderAddress;
                    role = baseMessage.getJoin().getRequestedRole();
                    masterId = message.getSenderId();
                    myId = message.getReceiverId();
                    //notifyObservers();
                }
                else if (baseMessage.hasSteer()){
                    System.out.println("received ack for steer");
                }
                else if (baseMessage.hasRoleChange()){
                    System.out.println("received ack for role change");
                    var roleChange = baseMessage.getRoleChange();
                    if (role == SnakesProto.NodeRole.VIEWER || role == SnakesProto.NodeRole.NORMAL){
                        role = roleChange.getSenderRole();
                        notifyObservers();
                    }
                }
                else if (baseMessage.hasState()) {
                    System.out.println("received ack for state message");
                }
                else if (baseMessage.hasPing()){
                    System.out.println("received ack for ping");
                }

            }
            else if (message.hasPing()){
                System.out.println("received ping from " + senderAddress);
                sendAck(message, senderAddress);
            }
            else if (message.hasState()){
                if (role == SnakesProto.NodeRole.MASTER || !senderAddress.equals(master)) {
                    System.out.println(senderAddress + " sent state message but we don't need it"); return;
                }
                if (message.getState().getState().getStateOrder() > lastState){
                    System.out.println("received state message");
                    this.gameState = message.getState().getState();
                    this.lastState = gameState.getStateOrder();
                    notifyObservers();
                    for(var player : message.getState().getState().getPlayers().getPlayersList()){
                        if (player.getRole() == SnakesProto.NodeRole.DEPUTY && player.hasIpAddress() && player.hasPort()){
                            deputy = new HostInfo(InetAddress.getByName(player.getIpAddress().substring(1)), player.getPort());
                            deputyId = player.getId();
                            if (deputyId == myId) {
                                System.out.println("I am a deputy");
                                role = SnakesProto.NodeRole.DEPUTY;
                            }
                        }
                    }
                }
                else {
                    System.out.println("received outdated state");
                }
                sendAck(message,senderAddress);

            }
            else if (message.hasSteer()){
                if (master == null) {
                    game.changeSnakeDirection(players.get(senderAddress), message.getSteer().getDirection());
                    sendAck(message, senderAddress);
                }
            }
            else if (message.hasJoin()){
                System.out.println("received join message");
                if (!players.contains(senderAddress)){
                    var joinMessage = message.getJoin();

                    if (joinMessage.getRequestedRole() == SnakesProto.NodeRole.VIEWER){
                        players.put(senderAddress,-1);
                        sendAck(message, senderAddress);
                    }
                    else {
                        var newPlayer = SnakesProto.GamePlayer.newBuilder()
                                .setName(joinMessage.getPlayerName())
                                .setIpAddress(senderAddress.ip.toString())
                                .setPort(senderAddress.port)
                                .setRole(joinMessage.getRequestedRole())
                                .setType(joinMessage.getPlayerType())
                                .setScore(0);
                        int id = game.addPlayer(newPlayer);
                        if (id < 0){
                            sendError(message,senderAddress);
                            return;
                        }
                        else{
                            players.put(senderAddress,id);
                            playerIds.put(id, senderAddress);
                            chooseDeputy();
                        }
                    }
                }
                lastMessageReceivedTime.put(senderAddress, System.currentTimeMillis());
                sendAck(message,senderAddress);
            }
            else if (message.hasRoleChange()){
                var roleMessage = message.getRoleChange();
                if (master == null){
                    if (roleMessage.getSenderRole() == SnakesProto.NodeRole.VIEWER){
                        var pid = players.get(senderAddress);
                        if (pid == null) return;
                        if (deputyId == pid){
                            deputy = null;
                            deputyId = -1;
                        }
                        game.makeZombie(pid);
                        players.put(senderAddress, -1);
                        sendAck(message, senderAddress);
                        if (deputy == null){
                            chooseDeputy();
                        }
                    }
                }
                else if (senderAddress.equals(master)){

                    if (role != roleMessage.getReceiverRole()){
                        role = roleMessage.getReceiverRole();
                    }
                    sendAck(message, senderAddress);
                }
                else{
                    sendError(message, senderAddress);
                }
            }
            else if (message.hasError()){
                if (myId == -1){
                    notifyObserversFinal();
                }
            }

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }








      //////////////////////
     // SENDING MESSAGES //
    //////////////////////


    private void sendPing(HostInfo address) {
        var message = SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(newSeqNum())
                .setPing(SnakesProto.GameMessage.PingMsg.newBuilder().build())
                .build();
        var msg = message.toByteArray();
        var packet = new DatagramPacket(msg, msg.length, address.ip, address.port);
        try {

            if (!messages.containsKey(address))
                messages.put(address,new ConcurrentHashMap<>());
            var messagesmap = messages.get(address);
            messagesmap.put(message.getMsgSeq(), new AbstractMap.SimpleEntry(message,System.currentTimeMillis()));
            socket.send(packet);
            System.out.println("Ping sent to " + address);
            lastMessageSentTime.put(address, System.currentTimeMillis());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void sendJoin(SnakesProto.NodeRole role){
        var joinMessage = SnakesProto.GameMessage.JoinMsg.newBuilder()
                .setGameName(gameName)
                .setRequestedRole(role)
                .setPlayerName(playerName)
                .setPlayerType(SnakesProto.PlayerType.HUMAN)
                .build();
        var message = SnakesProto.GameMessage.newBuilder().setMsgSeq(newSeqNum()).setJoin(joinMessage).build();
        var msg = message.toByteArray();
        var packet = new DatagramPacket(msg, msg.length, master.ip, master.port);
        try {

            if (!messages.containsKey(master))
                messages.put(master,new ConcurrentHashMap<>());
            var messagesmap = messages.get(master);
            messagesmap.put(message.getMsgSeq(), new AbstractMap.SimpleEntry(message,System.currentTimeMillis()));
            socket.send(packet);
            System.out.println("Join sent");
            lastMessageSentTime.put(master, System.currentTimeMillis());
            lastMessageReceivedTime.put(master, System.currentTimeMillis());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void sendRoleChange(int playerId, SnakesProto.NodeRole role) {

        if(playerId == myId){
            return;
        }
        var roleMessage = SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                .setReceiverRole(role)
                .build();
        var message = SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(newSeqNum())
                .setRoleChange(roleMessage).build();
        var msg = message.toByteArray();
        var playerAddress = playerIds.get(playerId);
        playerIds.remove(playerId);
        players.put(playerAddress, -1);
        var packet = new DatagramPacket(msg, msg.length, playerAddress.ip, playerAddress.port);
        try {
            socket.send(packet);
            if(!messages.containsKey(playerAddress))
                messages.put(playerAddress,new ConcurrentHashMap<>());
            var messagesmap = messages.get(playerAddress);
            messagesmap.put(message.getMsgSeq(), new AbstractMap.SimpleEntry<>(message, System.currentTimeMillis()));
            System.out.println("New role enlightened");
            lastMessageSentTime.put(playerAddress, System.currentTimeMillis());

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (playerId == deputyId){
            deputy = null;
            deputyId = -1;
            chooseDeputy();
        }

    }



    private void sendError(SnakesProto.GameMessage message, HostInfo senderAddress) {
        byte[] msg = null;
        var answer = SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(message.getMsgSeq())
                .setError(SnakesProto.GameMessage.ErrorMsg.newBuilder().setErrorMessage("No free space on the field").build())
                .setReceiverId(-1)
                .setSenderId(myId)
                .build();
        msg = answer.toByteArray();

        try {

            socket.send(new DatagramPacket(msg, msg.length, senderAddress.ip, senderAddress.port));
            System.out.println("Error sent");

            lastMessageSentTime.put(senderAddress, System.currentTimeMillis());

        } catch (IOException e) {
            e.printStackTrace();
        }
        lastMessageSentTime.put(senderAddress,System.currentTimeMillis());

    }




    private void sendAck(SnakesProto.GameMessage message, HostInfo senderAddress) {

        byte[] msg = null;
            var answerbuilder = SnakesProto.GameMessage.newBuilder()
                    .setMsgSeq(message.getMsgSeq())
                    .setAck(SnakesProto.GameMessage.AckMsg.newBuilder().build());
            SnakesProto.GameMessage answer;
            if (role == SnakesProto.NodeRole.MASTER){
                answer = answerbuilder
                        .setReceiverId(players.get(senderAddress))
                        .setSenderId(myId)
                        .build();
            }
            else {
                answer = answerbuilder.build();
            }

            msg = answer.toByteArray();
        try {

            socket.send(new DatagramPacket(msg, msg.length,senderAddress.ip, senderAddress.port));
            System.out.println("Ack sent");

            lastMessageSentTime.put(senderAddress, System.currentTimeMillis());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void sendSteer(SnakesProto.Direction direction) {
        byte[] msg = null;

        var message = SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(newSeqNum())
                .setReceiverId(masterId)
                .setSenderId(myId)
                .setSteer(SnakesProto.GameMessage.SteerMsg.newBuilder()
                        .setDirection(direction)
                        .build())
                .build();
        msg = message.toByteArray();

        try {

            socket.send(new DatagramPacket(msg, msg.length, master.ip, master.port));
            System.out.println("Steer sent");

            // saving the message info
            if(!messages.containsKey(master))
                messages.put(master,new ConcurrentHashMap<>());
            var messagesmap = messages.get(master);
            messagesmap.put(message.getMsgSeq(), new AbstractMap.SimpleEntry<>(message, System.currentTimeMillis()));
            lastMessageSentTime.put(master, System.currentTimeMillis());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void changeRole(SnakesProto.NodeRole reqRole) {
        if (this.role == SnakesProto.NodeRole.MASTER){
                game.makeZombie(myId);
                myId = -1;
        }
        var roleMessage = SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                .setSenderRole(reqRole)
                .build();
        var message = SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(newSeqNum())
                .setRoleChange(roleMessage).build();
        var msg = message.toByteArray();
        var packet = new DatagramPacket(msg, msg.length, master.ip, master.port);
        try {

            socket.send(packet);
            System.out.println("Change role requested");

            if(!messages.containsKey(master))
                messages.put(master,new ConcurrentHashMap<>());
            var messagesmap = messages.get(master);
            messagesmap.put(message.getMsgSeq(), new AbstractMap.SimpleEntry<>(message, System.currentTimeMillis()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }













    public void register(Observer observer) {
        observers.add(observer);
    }


    public String getGameName() {
        return gameName;
    }

    public PlayerView getGameState() {
        return new PlayerView(myId, gameState);
    }

    public SnakesProto.GameConfig getConfig() {
        return config;
    }

    public void changeDirection(SnakesProto.Direction direction){
        if (role == SnakesProto.NodeRole.MASTER) {
            game.changeSnakeDirection(myId, direction);
        }
        else if (role == SnakesProto.NodeRole.NORMAL || role == SnakesProto.NodeRole.DEPUTY){
            sendSteer(direction);
        }
    }



    private volatile long lastSeqNum = 0;
    public long newSeqNum(){
        return ++lastSeqNum;
    }

    void notifyObservers(){
        for (Observer observer : observers) {
            observer.update();
        }
    }
    void notifyObserversFinal(){
        for (Observer observer : observers) {
            observer.finalUpdate();
        }
    }

}
