package de.poweruser.powerserver.games.cneagle;

import de.poweruser.powerserver.games.GameBase;
import de.poweruser.powerserver.games.GameServerBase;
import de.poweruser.powerserver.main.MessageData;

import java.net.InetSocketAddress;

public class CEServer extends GameServerBase {
    public CEServer(GameBase game, InetSocketAddress server) {
        super(game, server);
    }

    @Override
    public boolean processNewMessage(MessageData message) {
        return super.processNewMessage(message);
    }

    @Override
    public String getServerName() {
        if(this.queryInfo.containsKey(DataKeyEnum.HOSTNAME)) { return this.queryInfo.getData(DataKeyEnum.HOSTNAME); }
        return null;
    }
}
