import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class scriptrunner implements Command {

    @Override
    public String name() {
        return "sh";
    }

    @Override
    public void run(String[] args) {
    if (args == null || args.length == 0) {
        System.out.println("usage: sh </path/to/script>");
        return;
    }
    try {
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;

        if (os.contains("win")) {
            pb = new ProcessBuilder("cmd.exe", "/c", args[0]);
        } else {
            pb = new ProcessBuilder("/bin/sh", args[0]);
        }

        pb.inheritIO(); // Connects stdin, stdout, stderr to the parent terminal
        Process process = pb.start();

        int exit = process.waitFor();
        if (exit != 0 && exit != 130) {
            System.out.println("Process exited with code " + exit);
        }

    } catch (IOException | InterruptedException e) {
        e.printStackTrace();
        Thread.currentThread().interrupt();
    }
}
}