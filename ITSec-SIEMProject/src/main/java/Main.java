import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompiler;
import com.espertech.esper.compiler.internal.util.EPCompilerImpl;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws IOException, NullPointerException {
        EPCompiler epc = new EPCompilerImpl();

        Configuration configuration = new Configuration();
        configuration.getCommon().addEventType(SSHLogMessage.class);


        EPRuntime runtime = EPRuntimeProvider.getDefaultRuntime(configuration);






        ProcessBuilder builder = new ProcessBuilder("bash", "-c", "journalctl -u ssh.service -o json");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        InputStream is = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String line = null;
        ArrayList<SSHLogMessage> messageList = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
//            System.out.println(line);
            SSHLogMessage mess = new SSHLogMessage(line);
//            messageList.add(mess);
//            System.out.println(mess.TRANSPORT + " " + mess.MESSAGE + " " + mess.SYSLOG_TIMESTAMP);
            runtime.getEventService().sendEventBean(mess, "SSHLogMessage");
        }
    }
}
