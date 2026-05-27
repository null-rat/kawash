public interface Command {
    String name();
    void run(String[] args);
}