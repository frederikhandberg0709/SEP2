package via.sep2.client.command;

import java.util.List;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CommandManager {

    private static final Logger logger = Logger.getLogger(CommandManager.class.getName());
    private final Stack<Command> commandHistory = new Stack<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "CommandManager-Thread");
        t.setDaemon(true);
        return t;
    });
    private static final int MAX_HISTORY_SIZE = 50;

    public CompletableFuture<Void> executeAsync(Command command) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Executing command: " + command.getDescription());
                command.execute();

                if (command.canUndo()) {
                    synchronized (commandHistory) {
                        commandHistory.push(command);

                        while (commandHistory.size() > MAX_HISTORY_SIZE) {
                            commandHistory.remove(0);
                        }
                    }
                    logger.info("Command executed and added to history: " + command.getDescription());
                } else {
                    logger.info("Command executed (not undoable): " + command.getDescription());
                }
            } catch (Exception e) {
                logger.severe("Command execution failed: " + command.getDescription() + " - " + e.getMessage());
                throw new RuntimeException("Command execution failed: " + command.getDescription(), e);
            }
        }, executorService);
    }

    public CompletableFuture<Void> undoLastAsync() {
        return CompletableFuture.runAsync(() -> {
            Command lastCommand;
            synchronized (commandHistory) {
                if (commandHistory.isEmpty()) {
                    throw new IllegalStateException("No commands to undo");
                }
                lastCommand = commandHistory.pop();
            }

            try {
                logger.info("Undoing command: " + lastCommand.getDescription());
                lastCommand.undo();
                logger.info("Command undone successfully: " + lastCommand.getDescription());
            } catch (Exception e) {
                synchronized (commandHistory) {
                    commandHistory.push(lastCommand);
                }
                logger.severe("Undo failed for: " + lastCommand.getDescription() + " - " + e.getMessage());
                throw new RuntimeException("Undo failed for: " + lastCommand.getDescription(), e);
            }
        }, executorService);
    }

    public List<String> getCommandHistory() {
        synchronized (commandHistory) {
            return commandHistory.stream()
                    .map(Command::getDescription)
                    .collect(Collectors.toList());
        }
    }

    public boolean canUndo() {
        synchronized (commandHistory) {
            return !commandHistory.isEmpty();
        }
    }

    public void clearHistory() {
        synchronized (commandHistory) {
            commandHistory.clear();
        }
        logger.info("Command history cleared");
    }

    public void shutdown() {
        executorService.shutdown();
        logger.info("CommandManager shutdown");
    }
}
