import com.fasterxml.jackson.core.JsonProcessingException;

public class SSHFailedLogMessage extends SSHLogMessage {
    String fromIP;

    public SSHFailedLogMessage(String json) throws JsonProcessingException, NullPointerException {
        super(json);
    }

    public SSHFailedLogMessage(SSHLogMessage SSHLogMessage) {
        this.SYSLOG_TIMESTAMP = SSHLogMessage.SYSLOG_TIMESTAMP;
        this.TRANSPORT = SSHLogMessage.TRANSPORT;
        String[] splitedMess = SSHLogMessage.MESSAGE.split(" ");
        for (int i = 0; i < splitedMess.length; i++) {
            if (splitedMess[i].equals("from")) {
                this.fromIP = splitedMess[i+1];
                break;
            }
        }
        this.MESSAGE = "Failed attempt from " + fromIP;
    }
}
