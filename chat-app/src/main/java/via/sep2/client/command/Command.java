package via.sep2.client.command;

public interface Command {
    void execute() throws Exception;

    void undo() throws Exception;

    boolean canUndo();

    String getDescription();
}
