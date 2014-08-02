package de.poweruser.powerserver.main;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import de.poweruser.powerserver.games.GameBase;
import de.poweruser.powerserver.games.GameServerBase;
import de.poweruser.powerserver.games.GameServerInterface;
import de.poweruser.powerserver.games.GeneralDataKeysEnum;
import de.poweruser.powerserver.logger.LogLevel;
import de.poweruser.powerserver.logger.Logger;
import de.poweruser.powerserver.network.UDPSender;
import de.poweruser.powerserver.settings.Settings;

public class ServerList {

    private GameBase game;
    private Map<InetSocketAddress, GameServerInterface> servers;

    public ServerList(GameBase game) {
        this.game = game;
        this.servers = new HashMap<InetSocketAddress, GameServerInterface>();
    }

    public boolean incomingHeartBeat(InetSocketAddress serverAddress, MessageData data) {
        if(serverAddress != null) {
            GameServerInterface server = this.getOrCreateServer(serverAddress);
            String serverName = server.getServerName();
            String logMessage = "Received a heartbeat from the server " + serverAddress.toString();
            if(serverName != null) {
                logMessage += (" ( " + serverName + " )");
            }
            Logger.logStatic(LogLevel.VERY_HIGH, logMessage);
            return server.incomingHeartbeat(serverAddress, data);
        }
        return false;
    }

    public boolean incomingHeartBeatBroadcast(InetSocketAddress serverAddress, MessageData data) {
        if(data.containsKey(GeneralDataKeysEnum.HEARTBEATBROADCAST) && data.containsKey(GeneralDataKeysEnum.HOST)) {
            GameServerInterface server = this.getOrCreateServer(serverAddress);
            if(server.isBroadcastedServer()) {
                String serverName = server.getServerName();
                String logMessage = "Received a heartbeat broadcast for the server " + serverAddress.toString();
                if(serverName != null) {
                    logMessage += (" ( " + serverName + " )");
                }
                Logger.logStatic(LogLevel.VERY_HIGH, logMessage);
                return server.incomingHeartBeatBroadcast(serverAddress, data);
            }
        } else {
            Logger.logStatic(LogLevel.HIGH, "Got a heartbeatbroadcast, that is missing the host key");
        }
        return false;
    }

    public void incomingQueryAnswer(InetSocketAddress sender, MessageData data) {
        GameServerInterface server = this.getServer(sender);
        if(server != null) {
            server.incomingQueryAnswer(sender, data);
        }
    }

    public boolean hasServer(InetSocketAddress server) {
        return this.servers.containsKey(server);
    }

    private GameServerInterface getServer(InetSocketAddress server) {
        if(this.servers.containsKey(server)) { return this.servers.get(server); }
        return null;
    }

    public boolean isBroadcastedServer(InetSocketAddress server) {
        GameServerInterface gameServer = this.getServer(server);
        if(gameServer != null) { return gameServer.isBroadcastedServer(); }
        return false;
    }

    private GameServerInterface getOrCreateServer(InetSocketAddress server) {
        GameServerInterface gameServer;
        if(!this.servers.containsKey(server)) {
            gameServer = this.game.createNewServer(server);
            this.servers.put(server, gameServer);
        } else {
            gameServer = this.servers.get(server);
        }
        return gameServer;
    }

    public List<InetSocketAddress> checkForServersToQueryAndOutdatedServers(Settings settings) {
        List<InetSocketAddress> serversToQuery = null;
        Iterator<Entry<InetSocketAddress, GameServerInterface>> iter = this.servers.entrySet().iterator();
        while(iter.hasNext()) {
            Entry<InetSocketAddress, GameServerInterface> entry = iter.next();
            GameServerInterface gsi = entry.getValue();
            if(!gsi.checkLastHeartbeat(settings.getAllowedHeartbeatTimeout(TimeUnit.MINUTES), TimeUnit.MINUTES)) {
                if(!gsi.checkLastMessage(settings.getMaximumServerTimeout(TimeUnit.MINUTES), TimeUnit.MINUTES)) {
                    iter.remove();
                    Logger.logStatic(LogLevel.NORMAL, "Removed server " + entry.getKey().toString() + " (" + entry.getValue().getServerName() + ") of game " + ((GameServerBase) gsi).getDisplayName() + ". Timeout reached.");
                } else if(!gsi.checkLastQueryRequest(settings.getEmergencyQueryInterval(TimeUnit.MINUTES), TimeUnit.MINUTES)) {
                    if(serversToQuery == null) {
                        serversToQuery = new ArrayList<InetSocketAddress>();
                    }
                    serversToQuery.add(entry.getKey());
                    String logMessage = "The server " + entry.getKey().toString();
                    String serverName = gsi.getServerName();
                    if(serverName != null) {
                        logMessage += " (" + serverName + ")";
                    }
                    logMessage += " does not send heartbeats anymore, or they dont reach this server. Sending back a query instead.";
                    Logger.logStatic(LogLevel.VERY_HIGH, logMessage);
                }
            }
        }
        return serversToQuery;
    }

    public List<InetSocketAddress> getActiveServers(Settings settings) {
        List<InetSocketAddress> list = new ArrayList<InetSocketAddress>();
        Iterator<Entry<InetSocketAddress, GameServerInterface>> iter = this.servers.entrySet().iterator();
        while(iter.hasNext()) {
            Entry<InetSocketAddress, GameServerInterface> entry = iter.next();
            GameServerInterface gsi = entry.getValue();
            if(gsi.checkLastMessage(settings.getMaximumServerTimeout(TimeUnit.MINUTES), TimeUnit.MINUTES)) {
                if(gsi.hasAnsweredToQuery()) {
                    list.add(entry.getKey());
                }
            }
        }
        return list;
    }

    public void queryServer(InetSocketAddress server, UDPSender udpSender, boolean queryPlayers) {
        GameServerInterface gsi = this.getServer(server);
        if(gsi != null) {
            gsi.markQueryRequestAsSentWithCurrentTime();
            udpSender.queueQuery(server, this.game.createStatusQuery(queryPlayers));
        }

    }

    public boolean addServer(InetSocketAddress server) {
        if(this.hasServer(server)) {
            GameServerBase gsb = (GameServerBase) this.getServer(server);
            Logger.logStatic(LogLevel.LOW, "The server " + server.toString() + " (" + gsb.getServerName() + ") is already enlisted for the game " + gsb.getDisplayName());
            return false;
        }
        GameServerInterface gsi = this.getOrCreateServer(server);
        ((GameServerBase) gsi).activateServerManually();
        return true;
    }
}
