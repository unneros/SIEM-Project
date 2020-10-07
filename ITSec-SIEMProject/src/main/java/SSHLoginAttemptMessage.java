import com.fasterxml.jackson.core.JsonProcessingException;

public class SSHLoginAttemptMessage {
    String TRANSPORT;
    String SYSLOG_TIMESTAMP;
    String MESSAGE;
    String FROMIP;

    public SSHLoginAttemptMessage(String MESSAGE, String SYSLOG_TIMESTAMP) {
        this.MESSAGE = MESSAGE;
        this.SYSLOG_TIMESTAMP = SYSLOG_TIMESTAMP;
        String[] splitedMess = this.MESSAGE.split(" ");
        for (int i = 0; i < splitedMess.length; i++) {
            if (splitedMess[i].equals("from")) {
                this.FROMIP = splitedMess[i+1];
                break;
            }
        }
        if (this.MESSAGE.contains("Failed password") || this.MESSAGE.contains("Failed authentication")) {
            this.MESSAGE = "Failed attempt";
        }
        if (this.MESSAGE.contains("Accepted password")) {
            this.MESSAGE = "Successful attempt";
        }
    }

    public SSHLoginAttemptMessage(SSHLogMessage SSHLogMessage) {
        this.SYSLOG_TIMESTAMP = SSHLogMessage.SYSLOG_TIMESTAMP;
        this.TRANSPORT = SSHLogMessage.TRANSPORT;
        String[] splitedMess = SSHLogMessage.MESSAGE.split(" ");
        for (int i = 0; i < splitedMess.length; i++) {
            if (splitedMess[i].equals("from")) {
                this.FROMIP = splitedMess[i+1];
                break;
            }
        }
        this.MESSAGE = "Failed attempt from " + FROMIP;
    }

    public String getSYSLOG_TIMESTAMP() {
        return SYSLOG_TIMESTAMP;
    }

    public String getMESSAGE() {
        return MESSAGE;
    }

    public String getFROMIP() {
        return FROMIP;
    }
}
