import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompiler;
import com.espertech.esper.compiler.internal.util.EPCompilerImpl;
import com.espertech.esper.runtime.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class Main {
    static HashMap<String, Integer> attempts;

    public static void main(String[] args) throws IOException, NullPointerException {
        EPCompiler epc = new EPCompilerImpl();

        Configuration configuration = new Configuration();
        configuration.getCommon().addEventType(SSHLogMessage.class);


        EPRuntime runtime = EPRuntimeProvider.getDefaultRuntime(configuration);

        CompilerArguments arguments = new CompilerArguments(configuration);

        EPCompiled epCompiled;
        try {
            epCompiled = epc.compile("@name('my-statement') select SYSLOG_TIMESTAMP, MESSAGE from SSHLogMessage", arguments);
        }
        catch (EPCompileException ex) {
            // handle exception here
            System.out.println("2");
            throw new RuntimeException(ex);
        }



        EPDeployment deployment;
        try {
            deployment = runtime.getDeploymentService().deploy(epCompiled);
        }
        catch (EPDeployException ex) {
            // handle exception here
            System.out.println("1");
            throw new RuntimeException(ex);
        }

        EPStatement statement = runtime.getDeploymentService().getStatement(deployment.getDeploymentId(), "my-statement");
        statement.addListener( (newData, oldData, statement1, runtime1) -> {
            String SYSLOG_TIMESTAMP = (String) newData[0].get("SYSLOG_TIMESTAMP");
            String MESSAGE = (String) newData[0].get("MESSAGE");
//            if (MESSAGE.contains("Failed password")) {
//                String attemptIP = (String) newData[0].get("fromIP");
//                if (attempts.containsKey(attemptIP)) {
//                    attempts.put(attemptIP, attempts.get(attemptIP) + 1);
//                } else {
//                    attempts.put(attemptIP, 0);
//                }
//            }
            System.out.println(String.format("SYSLOG_TIMESTAMP: %s, MESSAGE: %s, COUNTER : %d", SYSLOG_TIMESTAMP, MESSAGE, 42));
        });



        ProcessBuilder builder = new ProcessBuilder("bash", "-c", "journalctl -u ssh.service -o json");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        InputStream is = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String line = null;
        while ((line = reader.readLine()) != null) {
//            System.out.println(line);
            SSHLogMessage mess = new SSHLogMessage(line);
//            messageList.add(mess);
//            System.out.println(mess.TRANSPORT + " " + mess.MESSAGE + " " + mess.SYSLOG_TIMESTAMP);
            runtime.getEventService().sendEventBean(mess, "SSHLogMessage");
        }
    }
}
