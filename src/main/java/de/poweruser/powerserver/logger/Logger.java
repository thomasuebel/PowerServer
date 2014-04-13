package de.poweruser.powerserver.logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.poweruser.powerserver.main.parser.ParserException;

public class Logger {

    private File logFile;
    private SimpleDateFormat dateFormat;

    public Logger(File logFile) throws IOException {
        this.logFile = logFile;
        if(!this.logFile.exists()) {
            this.logFile.mkdirs();
            this.logFile.createNewFile();
        }
        this.dateFormat = new SimpleDateFormat("[dd.MM.yy-HH:mm:ss]");
    }

    public void log(ParserException exception) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.currentTimeString());
        sb.append(" ");
        sb.append(exception.getErrorMessage());
        sb.append(" For Game \"");
        sb.append(exception.getGame().getGameName());
        sb.append("\" with received data:\n");
        sb.append(exception.getUDPMessage().toString());
        this.writeToFile(sb.toString());
    }

    public void log(String message) {
        this.writeToFile(message);
    }

    private void writeToFile(String message) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(this.logFile, true));
            bw.newLine();
            bw.write(this.currentTimeString() + " " + message);
            bw.flush();
        } catch(IOException e) {}
        if(bw != null) {
            try {
                bw.close();
            } catch(IOException e) {}
        }
    }

    private String currentTimeString() {
        return this.dateFormat.format(new Date());
    }
}
