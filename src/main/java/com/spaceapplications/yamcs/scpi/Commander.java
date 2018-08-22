package com.spaceapplications.yamcs.scpi;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Commander {
  private static String PROMPT = "\r\n$ ";

  private String context = "";

  private static class Command {
    private String cmd;
    private String description;
    private Function<String, String> exec;

    public static Command of(String cmd, String description, Function<String, String> exec) {
      Command c = new Command();
      c.cmd = cmd;
      c.description = description;
      c.exec = exec;
      return c;
    }

    public String cmd() {
      return cmd;
    }

    public String description() {
      return description;
    }

    public String execute(String cmd) {
      String args = cmd.replaceFirst(this.cmd, "").trim();
      return exec.apply(args);
    }
  }

  private List<Command> commands = new ArrayList<>();

  public Commander(Config config) {
    commands.add(Command.of("device list", "List available devices to manage.", args -> config.devices.toString()));
    commands.add(Command.of("device inspect", "Print device configuration details.", args -> {
      return Optional.ofNullable(config.devices)
        .map(devices -> devices.get(args))
        .map(Config::dump)
        .orElse(MessageFormat.format("device \"{0}\" not found", args));
    }));
    commands.add(Command.of("help", "Prints this description.", args -> {
      return "Available commands:\n" + commands.stream().map(c -> String.format("%-20s %s", c.cmd(), c.description()))
          .collect(Collectors.joining("\n"));
    }));
  }

  public String confirm() {
    return "Connected. Run help for more info." + PROMPT;
  }

  public String execute(String cmd) {
    String result = commands.stream().filter(c -> cmd.startsWith(c.cmd)).findFirst().map(c -> c.execute(cmd))
        .orElse(cmd + ": command not found");
    return context + result + PROMPT;
  }
}