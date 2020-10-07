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
    static int counter = 0;
    static HashMap<String, Integer> attempts = new HashMap<>();

    public static void main(String[] args) throws IOException, NullPointerException {
        EPRuntime SSHLogMessageRuntime = initSSHLogMessageRuntime();

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
            SSHLogMessageRuntime.getEventService().sendEventBean(mess, "SSHLogMessage");
//            System.out.println(mess.MESSAGE + " " + mess.FROMIP);
        }
    }


    public static EPRuntime initSSHLogMessageRuntime() {
        EPRuntime SSHLoginAttemptMessageRuntime = initSSHLoginAttemptMessageRuntime();

        EPCompiler epc = new EPCompilerImpl();

        Configuration configuration = new Configuration();
        configuration.getCommon().addEventType(SSHLoginAttemptMessage.class);
        configuration.getCommon().addEventType(SSHLogMessage.class);


        EPRuntime runtime = EPRuntimeProvider.getDefaultRuntime(configuration);

        CompilerArguments arguments = new CompilerArguments(configuration);

        EPCompiled epCompiled;
        try {
            epCompiled = epc.compile("@name('my-statement') select * from SSHLogMessage",arguments);
        }
        catch (EPCompileException ex) {
            // handle exception here
            throw new RuntimeException(ex);
        }



        EPDeployment deployment;
        try {
            deployment = runtime.getDeploymentService().deploy(epCompiled);
        }
        catch (EPDeployException ex) {
            // handle exception here
            throw new RuntimeException(ex);
        }

        EPStatement statement = runtime.getDeploymentService().getStatement(deployment.getDeploymentId(), "my-statement");
        statement.addListener( (newData, oldData, statement1, runtime1) -> {
            String MESSAGE = (String) newData[0].get("MESSAGE");
            String SYSLOG_TIMESTAMP = (String) newData[0].get("SYSLOG_TIMESTAMP");
            if (MESSAGE.contains("Failed password") || MESSAGE.contains("Failed authentication")) {
                counter++;
                System.out.println(counter);
                SSHLoginAttemptMessageRuntime.getEventService().sendEventBean(new SSHLoginAttemptMessage(MESSAGE, SYSLOG_TIMESTAMP), "SSHLoginAttemptMessage");
            }
        });

        return runtime;
    }

    public static EPRuntime initSSHLoginAttemptMessageRuntime() {
        EPCompiler epc = new EPCompilerImpl();

        Configuration configuration = new Configuration();
        configuration.getCommon().addEventType(SSHLoginAttemptMessage.class);
        configuration.getCommon().addEventType(SSHLogMessage.class);


        EPRuntime runtime = EPRuntimeProvider.getDefaultRuntime(configuration);

        CompilerArguments arguments = new CompilerArguments(configuration);

        EPCompiled epCompiled;
        try {
            epCompiled = epc.compile("@name('my-statement2') select * from SSHLoginAttemptMessage " +
                    " match_recognize (" +
                    "   measures A as log1, B as log2, C as log3" +
                    "   pattern (A B C)" +
                    "   define " +
                    "     A as A.MESSAGE like \'Failed attempt\'," +
                    "     C as C.MESSAGE like \'Failed attempt\' and C.FROMIP like A.FROMIP," +
                    "     B as B.MESSAGE like \'Failed attempt\' and B.FROMIP like A.FROMIP)", arguments);
        }
        catch (EPCompileException ex) {
            // handle exception here
            throw new RuntimeException(ex);
        }



        EPDeployment deployment;
        try {
            deployment = runtime.getDeploymentService().deploy(epCompiled);
        }
        catch (EPDeployException ex) {
            // handle exception here
            throw new RuntimeException(ex);
        }

        EPStatement statement = runtime.getDeploymentService().getStatement(deployment.getDeploymentId(), "my-statement2");
        statement.addListener( (newData, oldData, statement2, runtime2) -> {
            String SYSLOG_TIMESTAMP = (String) newData[0].get("log1.FROMIP");
            System.out.println(String.format("Detected 3 consecutive failed login attempts from: " + SYSLOG_TIMESTAMP));
//            String MESSAGE = (String) newData[0].get("MESSAGE");
        });
        return runtime;
    }
}
