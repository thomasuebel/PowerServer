package de.poweruser.powerserver.games.cneagle;

import de.poweruser.powerserver.games.*;
import de.poweruser.powerserver.main.MessageData;
import de.poweruser.powerserver.main.parser.DataParserInterface;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public class CodenameEagle extends GameBase {
    private String displayName = "Codename: Eagle";

    public CodenameEagle(String gamename, String gamespyKey, DataParserInterface parser) {
        super(gamename, gamespyKey, parser, DataKeyEnum.values());
        //this.adjustPlayerInfoDataKeys();
    }

    private void adjustPlayerInfoDataKeys() {
        DataKeyEnum player = DataKeyEnum.PLAYER_;
        DataKeyEnum score = DataKeyEnum.SCORE_;
        DataKeyEnum deaths = DataKeyEnum.DEATHS_;
        DataKeyEnum team = DataKeyEnum.TEAM_;
        this.keyMap.remove(player.getKeyString());
        this.keyMap.remove(score.getKeyString());
        this.keyMap.remove(deaths.getKeyString());
        this.keyMap.remove(team.getKeyString());
        for(int i = 0; i <= 30; i++) {
            String index = String.valueOf(i);
            this.keyMap.put(player.getKeyString() + index, player);
            this.keyMap.put(score.getKeyString() + index, score);
            this.keyMap.put(deaths.getKeyString() + index, deaths);
            this.keyMap.put(team.getKeyString() + index, team);
        }
    }

    @Override
    public DataKeysInterface getHeartBeatDataKey() {
        return DataKeyEnum.HEARTBEAT;
    }

    @Override
    public DataKeysInterface getHeartBeatBroadcastDataKey() {
        return DataKeyEnum.HEARTBEATBROADCAST;
    }

    @Override
    public GameServerInterface createNewServer(InetSocketAddress server) {
        return new CEServer(this, server);
    }

    @Override
    public String getGameDisplayName(GameServerBase gameServer) throws IllegalArgumentException {
        if(!this.equals(gameServer.getGame())) {
            throw new IllegalArgumentException("The game of the gameServer does not match the selected game: " + gameServer.getGame().getGameName() + " vs " + this.getGameName()); }
        MessageData data = gameServer.getQueryInfo();
        String out = this.displayName;
        String version = null;
        if(data.containsKey(DataKeyEnum.GAMEVERSION)) {
            version = data.getData(DataKeyEnum.GAMEVERSION);
        } else if(data.containsKey(DataKeyEnum.ACTUALVERSION)) {
            version = data.getData(DataKeyEnum.ACTUALVERSION);
        }
        if(version != null) {
            out += " (version " + version + ")";
        }
        return out;
    }

    @Override
    public String getGamePort(GameServerBase gameServer) {
        if(!this.equals(gameServer.getGame())) { throw new IllegalArgumentException("The game of the gameServer does not match the selected game: " + gameServer.getGame().getGameName() + " vs " + this.getGameName()); }
        MessageData data = gameServer.getQueryInfo();
        String out = null;
        if(data.containsKey(de.poweruser.powerserver.games.opflashr.DataKeyEnum.HOSTPORT)) {
            out = data.getData(DataKeyEnum.HOSTPORT);
        }
        return out;
    }

    @Override
    public DatagramPacket createHeartbeatBroadcast(InetSocketAddress server, MessageData data) {
        DataKeysInterface heartbeat = this.getHeartBeatDataKey();
        DataKeysInterface broadcast = this.getHeartBeatBroadcastDataKey();
        String queryPort = (heartbeat != null && data.containsKey(heartbeat) ? data.getData(heartbeat) : String.valueOf(server.getPort()));
        if(broadcast != null) {
            StringBuilder builder = new StringBuilder();
            builder.append("\\");
            builder.append(broadcast.getKeyString());
            builder.append("\\");
            builder.append(queryPort);
            builder.append("\\");
            builder.append(GeneralDataKeysEnum.HOST.getKeyString());
            builder.append("\\");
            builder.append(server.getAddress().getHostAddress());
            builder.append("\\");
            builder.append(GeneralDataKeysEnum.GAMENAME.getKeyString());
            builder.append("\\");
            builder.append(this.gamename);
            if(data.containsKey(GeneralDataKeysEnum.STATECHANGED)) {
                builder.append("\\");
                builder.append(GeneralDataKeysEnum.STATECHANGED.getKeyString());
                builder.append("\\");
                builder.append(data.getData(GeneralDataKeysEnum.STATECHANGED));
            }
            byte[] message = builder.toString().getBytes();
            return new DatagramPacket(message, message.length);
        }
        return null;
    }

    @Override
    public DatagramPacket createStatusQuery(boolean queryPlayers) {
        StringBuilder builder = new StringBuilder();
        builder.append("\\");
        if(queryPlayers) {
            builder.append("info\\rules\\players");
        } else {
            builder.append("status");
        }
        builder.append("\\");
        byte[] message = builder.toString().getBytes();
        return new DatagramPacket(message, message.length);
    }
}
