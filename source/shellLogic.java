import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class shellLogic {
    private String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
    private final Map<String, Consumer<String[]>> commands = new HashMap<>();
    private URLClassLoader dynamicClassLoader = null;

    public shellLogic() {
        registerBuiltInCommands();
    }

    private void registerBuiltInCommands() {
        commands.put("exit", args -> {
            System.out.println("");
            closeClassLoader();
            System.exit(0);
        });
        
        commands.put("reload", args -> {
            try {
                loadCommands();
                System.out.println("Commands reloaded :D");
            } catch (Exception e) {
                System.out.println("Failed to reload commands: " + e.getMessage());
            }
        });
        
        commands.put("say", args -> {
            if (args.length > 0) {
                System.out.println(String.join(" ", args));
            } else {
                System.out.println("usage: say <text>");
            }
        });
        
        commands.put("clear", args -> {
            System.out.print("\r\n");
            for (int i = 0; i < 80; i++) {
                System.out.println();
            }
            System.out.flush();
        });
        
        commands.put("chgdir", args -> {
            if (args.length == 0) {
                System.out.println("usage: chgdir <path>");
                return;
            }

            try {
                String targetPath = String.join(" ", args);
                Path base = Paths.get(cwd);
                Path target = base.resolve(targetPath).normalize();
                File targetFile = target.toFile();

                if (targetFile.exists() && targetFile.isDirectory()) {
                    cwd = targetFile.getCanonicalPath();
                } else {
                    System.out.println("Directory does not exist: " + targetPath);
                }
            } catch (Exception e) {
                System.out.println("Error: Invalid path format.");
            }
        });
        
        commands.put("dir", args -> {
            Path targetPath = (args.length == 0) ? Paths.get(cwd) : Paths.get(cwd).resolve(String.join(" ", args)).normalize();
            File targetFolder = targetPath.toFile();

            if (!targetFolder.exists() || !targetFolder.isDirectory()) {
                System.out.println("Error: Directory does not exist or is invalid: " + targetPath);
                return;
            }

            File[] files = targetFolder.listFiles();
            if (files == null) {
                System.out.println("Error: Unable to read directory.");
                return;
            }
            if (files.length == 0) {
                System.out.println("Directory is empty.");
                return;
            }

            Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

            for (File file : files) {
                String type = file.isDirectory() ? "<DIR> " : "      ";
                System.out.println(type + file.getName());
            }
        });
        
        commands.put("file", args -> {
            if (args.length == 0) {
                System.out.println("usage: file <filename>");
                return;
            }
            try {
                String fileName = String.join(" ", args);
                File target = new File(cwd, fileName);
                if (!target.exists() || target.isDirectory()) {
                    System.out.println("file: " + fileName + ": No such file");
                    return;
                }
                Files.lines(target.toPath()).forEach(System.out::println);
            } catch (Exception e) {
                System.out.println("file: error reading file");
            }
        });
        
        commands.put("help", args -> {
            System.out.println("Available commands:");
            commands.keySet().stream()
                .sorted()
                .forEach(cmd -> System.out.println("  " + cmd));
        });
        
        commands.put("load", args -> {
            if (args.length < 2) {
                System.out.println("usage: load <folder-path> <class-name>");
                return;
            }

            try {
                String folderPath = args[0];
                String className = args[1];

                Path base = Paths.get(cwd);
                Path targetDir = base.resolve(folderPath).normalize();
                File folder = targetDir.toFile();

                if (!folder.exists() || !folder.isDirectory()) {
                    System.out.println("Error: Directory does not exist -> " + targetDir);
                    return;
                }

                File classFile = new File(folder, className + ".class");
                if (!classFile.exists()) {
                    System.out.println("Error: Class file " + className + ".class not found in " + folder.getAbsolutePath());
                    return;
                }

                URL[] urls = { folder.toURI().toURL() };
                try (URLClassLoader customLoader = new URLClassLoader(urls, shellLogic.class.getClassLoader())) {
                    Class<?> clazz = customLoader.loadClass(className);
                    
                    if (Command.class.isAssignableFrom(clazz)) {
                        Command command = (Command) clazz.getDeclaredConstructor().newInstance();
                        commands.put(command.name(), command::run);
                        System.out.println("Successfully loaded external command: " + command.name());
                    } else {
                        System.out.println("Error: Class '" + className + "' does not implement Command interface.");
                    }
                }
            } catch (Exception e) {
                System.out.println("Failed to load external command: " + e.getMessage());
            }
        });
        
        commands.put("edit", args -> {
            if (args.length == 0) {
                System.out.println("usage: edit <filename>");
                return;
            }

            String fileName = String.join(" ", args);
            File target = new File(cwd, fileName);
            List<String> fileBuffer = new ArrayList<>();

            if (target.exists() && !target.isDirectory()) {
                try {
                    fileBuffer.addAll(Files.readAllLines(target.toPath()));
                } catch (IOException e) {
                    System.out.println("Error reading file.");
                }
            }

            System.out.println("---" + fileName + "---");
            System.out.println("Press Enter to make a new line");
            System.out.println(" Type '-' and press Enter to delete the last line.");
            System.out.println(" Press Ctrl+X to save and exit back to shell.");
            System.out.println("----------------------------------------------------------------------");

            for (int i = 0; i < fileBuffer.size(); i++) {
                System.out.printf("%2d | %s%n", (i + 1), fileBuffer.get(i));
            }

            try {
                StringBuilder currentLine = new StringBuilder();

                while (true) {
                    if (currentLine.length() == 0) {
                        System.out.print((fileBuffer.size() + 1) + " > ");
                    }

                    int c = System.in.read();

                    if (c == 24) {
                        try {
                            Files.write(target.toPath(), fileBuffer);
                            System.out.println("\nFile safely saved to " + fileName);
                        } catch (IOException e) {
                            System.out.println("Critical error saving file!");
                        }
                        break;
                    }

                    if (c == 13 || c == 10) {
                        if (c == 13 && System.in.available() > 0) {
                            System.in.mark(1);
                            int nextChar = System.in.read();
                            if (nextChar != 10) {
                                System.in.reset();
                            }
                        }

                        String lineText = currentLine.toString();
                        if (lineText.equals("-")) {
                            if (!fileBuffer.isEmpty()) {
                                fileBuffer.remove(fileBuffer.size() - 1);
                                System.out.println("[Previous line deleted. Total lines: " + fileBuffer.size() + "]");
                            } else {
                                System.out.println("[File is already empty]");
                            }
                        } else {
                            fileBuffer.add(lineText);
                            System.out.println();
                        }
                        currentLine.setLength(0);
                    } else if (c == 127 || c == 8) {
                        if (currentLine.length() > 0) {
                            currentLine.deleteCharAt(currentLine.length() - 1);
                            System.out.print("\b \b");
                        }
                    } else if (c >= 32 && c <= 126) {
                        currentLine.append((char) c);
                        System.out.print((char) c);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error during edit session.");
            }
        });
    }

    public void loadCommands() throws Exception {
        closeClassLoader();

        URL classLocation = shellLogic.class.getProtectionDomain().getCodeSource().getLocation();
        File baseDir = new File(classLocation.toURI()).getParentFile();
        File folder = new File(baseDir, "cmd");
        
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("External 'cmd' folder not found. " + "Use load or make one to get non-internal commands.");
            return;
        }

        File[] files = folder.listFiles(f -> f.getName().endsWith(".class"));
        if (files == null) return;
        
        URL[] urls = { folder.toURI().toURL() };
        dynamicClassLoader = new URLClassLoader(urls);
        
        commands.keySet().removeIf(key -> !isBuiltIn(key));

        for (File file : files) {
            String className = file.getName().replace(".class", "");
            if (className.equals("shellLogic") || className.equals("Command")) continue;
            
            try {
                Class<?> clazz = dynamicClassLoader.loadClass(className);
                if (Command.class.isAssignableFrom(clazz)) {
                    Command command = (Command) clazz.getDeclaredConstructor().newInstance();
                    commands.put(command.name(), command::run);
                    System.out.println("Loaded command: " + command.name());
                }
            } catch (Exception e) {
                System.out.println("Failed to load plugin class: " + className);
            }
        }
    }

    private boolean isBuiltIn(String cmd) {
        return List.of("exit", "reload", "say", "clear", "chgdir", "dir", "file", "help", "load", "edit").contains(cmd);
    }

    private void closeClassLoader() {
        if (dynamicClassLoader != null) {
            try {
                dynamicClassLoader.close();
            } catch (IOException e) {
            }
        }
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        String ver = "1.5.1";
        System.out.println("kawash " + "v" + ver);
        System.out.println("Use 'help' for more command information.");
        String user = System.getProperty("user.name");
        String homeDir = System.getProperty("user.home");

        while (true) {
            System.out.println("┌──" + "(" + (user) + ")" + "──" + "[" + cwd + "]");
            System.out.print("└─> ");
            if (!scanner.hasNextLine()) break;
            
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;
            
            List<String> tokenList = new ArrayList<>();
            Matcher matcher = regex.matcher(input);
            
            while (matcher.find()) {
                String token;
                if (matcher.group(1) != null) {
                    token = matcher.group(1);
                } else if (matcher.group(2) != null) {
                    token = matcher.group(2);
                } else {
                    token = matcher.group();
                }

                if (token.startsWith("~")) {
                    token = homeDir + token.substring(1);
                }
                tokenList.add(token);
            }
            
            if (tokenList.isEmpty()) continue;
            
            String commandName = tokenList.get(0);
            String[] args = tokenList.subList(1, tokenList.size()).toArray(new String[0]);
            
            Consumer<String[]> command = commands.get(commandName);
            if (command == null) {
                System.out.println("Unknown Command: " + "'" + commandName + "'");
                continue;
            }
            
            try {
                command.accept(args);
            } catch (Exception e) {
                System.out.println("Execution error running command '" + commandName + "'");
            }
        }
        scanner.close();
    }

    public static void main(String[] args) {
        try {
            shellLogic shell = new shellLogic();
            shell.loadCommands();
            shell.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
