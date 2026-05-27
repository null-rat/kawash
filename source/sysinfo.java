import java.net.InetAddress;
import java.time.LocalDateTime;

public class sysinfo implements Command {

    @Override
    public String name() {
        return "sys-info";
    }

    @Override
    public void run(String[] args) {
        try {
            String ascii = """
                                               
                         @                   
                       @@@                   
                    @@@@    @@               
                  @@@@   @@@                 
                 @@@.   @@@                  
                 @@@     @@@                 
                   @@     @@                 
                     @    @                  
            @@@@                . @@@        
           .@@@@@@@@@@@@@@@@@@     @@@       
              @@@@@@@@@@@@@        @@        
              @@@@@@@@@@@@@@@:   @@@                                   
               -@@@@@@@@@@@@    @@           
        @@@@                 @@@@            
           .@@@@@@@@@@@@@@@@@      .@        
                               .@@@          
               @@@@@@@@@@@@@@@.              
""";

            String user = System.getProperty("user.name");
            String os = System.getProperty("os.name");
            String javaVersion = System.getProperty("java.version");
            String javaVendor = System.getProperty("java.vendor");
            String arch = System.getProperty("os.arch");
            int cpuCores = Runtime.getRuntime().availableProcessors();
            
            String host;
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                host = "Host missing :(";
            }

            System.out.println(ascii);
            System.out.println(user + "@" + host);
            System.out.println("-------------------------");
            System.out.println("User:             " + user);
            System.out.println("Host:             " + host);
            System.out.println("Operating System: " + os);
            System.out.println("CPU Architecture: " + arch + " (" + cpuCores + " cores)");
            System.out.println("Java Environment: " + javaVendor + ", Java " + javaVersion);
            System.out.println("System Time:      " + LocalDateTime.now());
            System.out.println();

        } catch (Exception e) {
            System.out.println("Fetch error: " + e.getMessage());
        }
    }
}