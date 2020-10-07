import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SSHLogMessage {
    String TRANSPORT;
    String SYSLOG_TIMESTAMP;
    String MESSAGE;

    public SSHLogMessage() {

    }

    public SSHLogMessage(String json) throws JsonProcessingException, NullPointerException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);
        this.TRANSPORT = jsonNode.get("_TRANSPORT").asText();
        if (!TRANSPORT.equals("journal")) {
            this.SYSLOG_TIMESTAMP = jsonNode.get("SYSLOG_TIMESTAMP").asText();
            this.MESSAGE = jsonNode.get("MESSAGE").asText();
        }
    }

    public String getSYSLOG_TIMESTAMP() {
        return SYSLOG_TIMESTAMP;
    }

    public String getMESSAGE() {
        return MESSAGE;
    }

}
