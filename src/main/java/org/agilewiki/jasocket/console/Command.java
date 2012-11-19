package org.agilewiki.jasocket.console;

public class Command {
    private String description;
    private String type;

    public Command(String description, String type) {
        this.description = description;
        this.type = type;
    }

    public String description() {return description;}

    public String type() {return type;}
}
