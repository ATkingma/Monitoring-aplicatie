import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.jcraft.jsch.*;

public class SSHTest {

    public static void main(String[] args) {
        String[] hosts = {"192.168.1.108", "192.168.1.149", "192.168.1.107"};
        String[] usernames = {"student", "student", "student"};
        String[] passwords = {"Welkom01!", "Chocomel9393", "Chocomel9393"};

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < hosts.length; i++) {
            String host = hosts[i];
            String user = usernames[i];
            String password = passwords[i];

            try {
                // Create JSch instance
                JSch jsch = new JSch();

                // Establish SSH session
                Session session = jsch.getSession(user, host, 22);
                session.setPassword(password);
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect();

                // Execute command to fetch CPU usage
                String command = "top -b -n 1 | grep '%Cpu'";
                Channel channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);
                channel.setInputStream(null);
                ((ChannelExec) channel).setErrStream(System.err);

                // Get command output
                java.io.InputStream in = channel.getInputStream();
                channel.connect();
                byte[] tmp = new byte[1024];
                StringBuilder output = new StringBuilder();
                while (true) {
                    while (in.available() > 0) {
                        int len = in.read(tmp, 0, 1024);
                        if (len < 0) break;
                        output.append(new String(tmp, 0, len));
                    }
                    if (channel.isClosed()) {
                        if (in.available() > 0) continue;
                        System.out.println("CPU usage from server: " + host);
                        System.out.println(output.toString().trim()); // Trim to remove extra spaces or new lines
                        System.out.println();
                        saveData(host, output.toString(), dtf.format(LocalDateTime.now()), dateFormatter.format(currentDate)); // Save data to file
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ee) {
                    }
                }

                // Disconnect session
                channel.disconnect();
                session.disconnect();
            } catch (JSchException | java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void saveData(String host, String data, String timestamp, String currentDate) {
        try (FileWriter writer = new FileWriter(host + "_" + currentDate + "_data.txt", true)) {
            writer.write("Timestamp: " + timestamp + "\n");
            writer.write(data + "\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
