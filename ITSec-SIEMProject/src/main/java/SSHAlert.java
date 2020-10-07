public class SSHAlert {
    String SYSLOG_TIMESTAMP;
    String MESSAGE;
    String FROMIP;

    public SSHAlert(String SYSLOG_TIMESTAMP, String MESSAGE, String FROMIP) {
        this.FROMIP = FROMIP;
        this.MESSAGE = MESSAGE;
        this.SYSLOG_TIMESTAMP = SYSLOG_TIMESTAMP;
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
