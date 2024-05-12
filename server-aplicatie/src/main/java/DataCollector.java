import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

import com.jcraft.jsch.*;

public class DataCollector {

    public static void main(String[] args) {
        Timer timer = new Timer();
        int delay = 0;
        int period = 15 * 60 * 1000;

        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                String[] hosts = {"192.168.1.108", "192.168.1.149","192.168.1.107"};
                String[] usernames = {"student", "student","student"};
                String[] passwords = {"Welkom01!", "Chocomel9393","Chocomel9393"};

                String databaseHostname = "192.168.1.109";
                String databasePort = "3306";
                String databaseName = "component_info";
                String databaseUsername = "user";
                String databasePassword = "blopfis1";

                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                for (int i = 0; i < hosts.length; i++) {//elke host
                    String host = hosts[i];
                    String user = usernames[i];
                    String password = passwords[i];

                    try {
                        JSch jsch = new JSch();

                        Session session = jsch.getSession(user, host, 22);
                        session.setPassword(password);
                        session.setConfig("StrictHostKeyChecking", "no");
                        session.connect();

                        // Execute commands to gather system information
                        String uptimeCommand = "uptime";
                        String cpuInfoCommand = "top -b -n 1 | grep '%Cpu'";
                        String processInfoCommand = "ps aux";
                        String ramInfoCommand = "free -m | grep Mem";
                        String diskInfoCommand = "df -h";

                        String uptime = executeCommand(session, uptimeCommand);
                        String cpuInfo = executeCommand(session, cpuInfoCommand);
                        String processInfo = executeCommand(session, processInfoCommand);
                        String ramInfo = executeCommand(session, ramInfoCommand);
                        String diskInfo = executeCommand(session, diskInfoCommand);

                        // Extract relevant information from commands output
                        String cpuType = extractCpuType(cpuInfo);
                        float runningUserProcesses = extractUserTasks(processInfo);
                        float runningKernelProcesses = extractRunningKernelProcesses(processInfo);
                        float idleTime = extractIdleTime(cpuInfo);
                        String[] ramInfoParts = ramInfo.split("\\s+");
                        String ramTotal = ramInfoParts.length > 1 ? ramInfoParts[1] : "N/A";
                        String ramUsed = ramInfoParts.length > 2 ? ramInfoParts[2] : "N/A";
                        String ramFree = ramInfoParts.length > 3 ? ramInfoParts[3] : "N/A";
                        String[] diskInfoLines = diskInfo.split("\\n");
                        String diskSpace = extractDiskSpace(diskInfoLines[1]);
                        String freeDiskSpace = extractFreeDiskSpace(diskInfoLines[1]);

                        //printje voor het .bat file zodat je kan checken of alles goed is gegaan
                        System.out.println("Data to be inserted into component_data table:");
                        System.out.println("Host: " + host);
                        System.out.println("Timestamp: " + LocalDateTime.now().format(dateFormatter));
                        System.out.println("Uptime: " + uptime);
                        System.out.println("CPU Type: " + cpuType);
                        System.out.println("Running User Processes: " + runningUserProcesses);
                        System.out.println("Running Kernel Processes: " + runningKernelProcesses);
                        System.out.println("Idle Time: " + idleTime);
                        System.out.println("RAM Total: " + ramTotal);
                        System.out.println("RAM Used: " + ramUsed);
                        System.out.println("RAM Free: " + ramFree);
                        System.out.println("Disk Space: " + diskSpace);
                        System.out.println("Free Disk Space: " + freeDiskSpace);


                        saveDataToDatabase(host, uptime, cpuType, runningUserProcesses, runningKernelProcesses, idleTime,
                                ramTotal, ramUsed, ramFree, diskSpace, freeDiskSpace,
                                databaseHostname, databasePort, databaseName, databaseUsername, databasePassword);

                        session.disconnect();
                    } catch (JSchException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, delay, period);
    }

    private static String executeCommand(Session session, String command) throws JSchException {
        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
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
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }

            channel.disconnect();
            return output.toString().trim();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return "";
        }
    }


    //deze extract functies boeien geen fuck
    private static String extractCpuType(String cpuInfo) {
        String[] parts = cpuInfo.split(":");
        if (parts.length > 1) {
            return parts[1].trim();
        }
        return "N/A";
    }

    private static int extractRunningKernelProcesses(String processInfo) {
        int count = 0;
        String[] lines = processInfo.split("\\r?\\n");
        for (String line : lines) {
            if (line.contains("[k]")) {
                count++;
            }
        }
        return count;
    }

    private static int extractUserTasks(String processInfo) {
        int count = 0;
        String[] lines = processInfo.split("\\r?\\n");
        for (String line : lines) {
            if (!line.contains("[k]")) {
                count++;
            }
        }
        return count;
    }

    private static float extractIdleTime(String cpuInfo) {
        String[] parts = cpuInfo.split(",");
        for (String part : parts) {
            if (part.contains("id")) {
                String idlePercentage = part.trim().split("\\s+")[0];
                return Float.parseFloat(idlePercentage);
            }
        }
        return 0.0f;
    }


    private static String extractDiskSpace(String diskInfo) {
        String[] parts = diskInfo.split("\\s+");
        if (parts.length > 1) {
            return parts[1];
        }
        return "N/A";
    }

    private static String extractFreeDiskSpace(String diskInfo) {
        String[] parts = diskInfo.split("\\s+");
        if (parts.length > 3) {
            return parts[3];
        }
        return "N/A";
    }

    private static void saveDataToDatabase(String host, String uptime, String cpuType, float runningUserProcesses,
                                           float runningKernelProcesses, float idleTime,
                                           String ramTotal, String ramUsed, String ramFree,
                                           String diskSpace, String freeDiskSpace,
                                           String dbHostname, String dbPort, String dbName,
                                           String dbUsername, String dbPassword) {
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://" + dbHostname + ":" + dbPort + "/" + dbName, dbUsername, dbPassword)) {
            String sql = "INSERT INTO component_data (ip, timestamp, uptime, disk_space, free_disk_space, cpu_type, running_user_processes, running_kernel_processes, idle, ram_total, ram_used, ram_free) VALUES (?, NOW(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, host);
                statement.setString(2, uptime);
                statement.setString(3, diskSpace);
                statement.setString(4, freeDiskSpace);
                statement.setString(5, cpuType);
                statement.setFloat(6, runningUserProcesses);
                statement.setFloat(7, runningKernelProcesses);
                statement.setFloat(8, idleTime);
                statement.setString(9, ramTotal);
                statement.setString(10, ramUsed);
                statement.setString(11, ramFree);

                int rowsAffected = statement.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("Data inserted successfully.");
                } else {
                    System.out.println("Failed to insert data.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
