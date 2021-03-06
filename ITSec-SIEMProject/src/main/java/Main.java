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
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
//    static HashMap<String, Integer> attempts = new HashMap<>();

    public static void main(String[] args) throws IOException, NullPointerException {
        123 45678 10 1 1 12
        EPRuntime SSHLogMessageRuntime = initSSHLogMessageRuntime();
        System.out.println("Finished compiling");
        int journalLines = 0;
        while (true) {
            ProcessBuilder builder = new ProcessBuilder("bash", "-c", "journalctl -u ssh.service -o json");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            InputStream is = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            ArrayList<String> jsonList = jsonList(reader);
            int newJournalLines = jsonList.size();

            String line = null;
            for (int i = journalLines; i < newJournalLines; i++) {
                line = jsonList.get(i);
                SSHLogMessage mess = new SSHLogMessage(line);
                SSHLogMessageRuntime.getEventService().sendEventBean(mess, "SSHLogMessage");
            }
            journalLines = newJournalLines;
        }
    }


    public static EPRuntime initSSHLogMessageRuntime() {
//        EPRuntime SSHLoginAttemptMessageRuntime = initSSHLoginAttemptMessageRuntime();

        EPCompiler epc = new EPCompilerImpl();

        Configuration configuration = new Configuration();
        configuration.getCommon().addEventType(SSHLoginAttemptMessage.class);
        configuration.getCommon().addEventType(SSHLogMessage.class);
        configuration.getCommon().addEventType(SSHAlert.class);


        EPRuntime runtime = EPRuntimeProvider.getDefaultRuntime(configuration);

        CompilerArguments arguments = new CompilerArguments(configuration);

        EPCompiled epCompiled;
        try {
            epCompiled = epc.compile("@name('SSHLogMessage') select * from SSHLogMessage as log " +
                    "where log.MESSAGE like \'%Failed authentication%\' " +
                    "or log.MESSAGE like \'%Failed password%\' " +
                    "or log.MESSAGE like \'Accepted password\'", arguments);
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

        EPStatement statement = runtime.getDeploymentService().getStatement(deployment.getDeploymentId(), "SSHLogMessage");
        statement.addListener( (newData, oldData, statement1, runtime1) -> {
            String MESSAGE = (String) newData[0].get("MESSAGE");
            String SYSLOG_TIMESTAMP = (String) newData[0].get("SYSLOG_TIMESTAMP");
            runtime.getEventService().sendEventBean(new SSHLoginAttemptMessage(MESSAGE, SYSLOG_TIMESTAMP), "SSHLoginAttemptMessage");
        });

        //--------------------------------

        EPCompiled epCompiled2;
        try {
            epCompiled2 = epc.compile("@name('SSHLoginAttemptMessage') select * from SSHLoginAttemptMessage " +
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

        EPDeployment deployment2;
        try {
            deployment2 = runtime.getDeploymentService().deploy(epCompiled2);
        }
        catch (EPDeployException ex) {
            // handle exception here
            throw new RuntimeException(ex);
        }

        EPStatement statement2 = runtime.getDeploymentService().getStatement(deployment2.getDeploymentId(), "SSHLoginAttemptMessage");
        statement2.addListener( (newData, oldData, statement1, runtime1) -> {
            String FROMIP = (String) newData[0].get("log1.FROMIP");
            String SYSLOG_TIMESTAMP = (String) newData[0].get("log1.SYSLOG_TIMESTAMP");
            runtime.getEventService().sendEventBean(new SSHAlert(SYSLOG_TIMESTAMP, "Three consecutive failed login attempt", FROMIP), "SSHAlert");
        });

        //------------------------

        EPCompiled epCompiled3;
        try {
            epCompiled3 = epc.compile("@name('SSHAlert') select * from SSHAlert ", arguments);
        }
        catch (EPCompileException ex) {
            // handle exception here
            throw new RuntimeException(ex);
        }



        EPDeployment deployment3;
        try {
            deployment3 = runtime.getDeploymentService().deploy(epCompiled3);
        }
        catch (EPDeployException ex) {
            // handle exception here
            throw new RuntimeException(ex);
        }

        EPStatement statement3 = runtime.getDeploymentService().getStatement(deployment3.getDeploymentId(), "SSHAlert");
        statement3.addListener( (newData, oldData, statement1, runtime1) -> {
            String FROMIP = (String) newData[0].get("FROMIP");
            String SYSLOG_TIMESTAMP = (String) newData[0].get("SYSLOG_TIMESTAMP");
            System.out.println(String.format("Detected 3 consecutive failed login attempts from: " + FROMIP + " at: " + SYSLOG_TIMESTAMP));
        });

        return runtime;
    }

//    public static EPRuntime initSSHLoginAttemptMessageRuntime() {
//        EPRuntime SSHAlertRuntime = initSSHAlertRuntime();
//        EPCompiler epc = new EPCompilerImpl();
//
//        Configuration configuration = new Configuration();
//        configuration.getCommon().addEventType(SSHLoginAttemptMessage.class);
//        configuration.getCommon().addEventType(SSHLogMessage.class);
//
//
//        EPRuntime runtime = EPRuntimeProvider.getDefaultRuntime(configuration);
//
//        CompilerArguments arguments = new CompilerArguments(configuration);
//
//        EPCompiled epCompiled;
//        try {
//            epCompiled = epc.compile("@name('SSHLoginAttemptMessage') select * from SSHLoginAttemptMessage " +
//                    " match_recognize (" +
//                    "   measures A as log1, B as log2, C as log3" +
//                    "   pattern (A B C)" +
//                    "   define " +
//                    "     A as A.MESSAGE like \'Failed attempt\'," +
//                    "     C as C.MESSAGE like \'Failed attempt\' and C.FROMIP like A.FROMIP," +
//                    "     B as B.MESSAGE like \'Failed attempt\' and B.FROMIP like A.FROMIP)", arguments);
//        }
//        catch (EPCompileException ex) {
//            // handle exception here
//            throw new RuntimeException(ex);
//        }
//
//
//
//        EPDeployment deployment;
//        try {
//            deployment = runtime.getDeploymentService().deploy(epCompiled);
//        }
//        catch (EPDeployException ex) {
//            // handle exception here
//            throw new RuntimeException(ex);
//        }
//
//        EPStatement statement = runtime.getDeploymentService().getStatement(deployment.getDeploymentId(), "SSHLoginAttemptMessage");
//        statement.addListener( (newData, oldData, statement2, runtime2) -> {
//            String FROMIP = (String) newData[0].get("log1.FROMIP");
//            String SYSLOG_TIMESTAMP = (String) newData[0].get("log1.SYSLOG_TIMESTAMP");
//            SSHAlertRuntime.getEventService().sendEventBean(new SSHAlert(SYSLOG_TIMESTAMP, "Three consecutive failed login attempt", FROMIP), "SSHAlert");
//        });
//        return runtime;
//    }
//
//    public static EPRuntime initSSHAlertRuntime() {
//        EPCompiler epc = new EPCompilerImpl();
//
//        Configuration configuration = new Configuration();
//        configuration.getCommon().addEventType(SSHLoginAttemptMessage.class);
//        configuration.getCommon().addEventType(SSHLogMessage.class);
//        configuration.getCommon().addEventType(SSHAlert.class);
//
//
//        EPRuntime runtime = EPRuntimeProvider.getDefaultRuntime(configuration);
//
//        CompilerArguments arguments = new CompilerArguments(configuration);
//
//        EPCompiled epCompiled;
//        try {
//            epCompiled = epc.compile("@name('SSHAlert') select * from SSHAlert ", arguments);
//        }
//        catch (EPCompileException ex) {
//            // handle exception here
//            throw new RuntimeException(ex);
//        }
//
//
//
//        EPDeployment deployment;
//        try {
//            deployment = runtime.getDeploymentService().deploy(epCompiled);
//        }
//        catch (EPDeployException ex) {
//            // handle exception here
//            throw new RuntimeException(ex);
//        }
//
//        EPStatement statement = runtime.getDeploymentService().getStatement(deployment.getDeploymentId(), "SSHAlert");
//        statement.addListener( (newData, oldData, statement2, runtime2) -> {
//            String FROMIP = (String) newData[0].get("FROMIP");
//            String SYSLOG_TIMESTAMP = (String) newData[0].get("SYSLOG_TIMESTAMP");
//            System.out.println(String.format("Detected 3 consecutive failed login attempts from: " + FROMIP + " at: " + SYSLOG_TIMESTAMP));
//        });
//        return runtime;
//    }

    public static ArrayList<String> jsonList(BufferedReader br) throws IOException {
        String line = null;
        ArrayList<String> result = new ArrayList<>();
        while ((line = br.readLine()) != null) result.add(line);
        return result;
    }
}
