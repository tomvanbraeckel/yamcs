package com.spaceapplications.yamcs.scpi;

import static pl.touk.throwing.ThrowingFunction.unchecked;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class Config {
  public DaemonConfig daemon;
  public Map<String, DeviceConfig> devices;

  public static class DaemonConfig {
    public int port;
    public int max_connections;
  }

  public static class DeviceConfig {
    public String locator;

    @Override
    public String toString() {
      return "Device:";
    }
  }

  public static Config load(String path) {
    Constructor c = new Constructor(Config.class);
    TypeDescription d = new TypeDescription(Config.class);
    c.addTypeDescription(d);
    Yaml yaml = new Yaml(c);
    InputStream is = unchecked(Config::inputStream).apply(path);

    try {
      Config config = (Config) yaml.load(is);
      if (config == null)
        throw throwRuntimeException("The file is empty.", path);
      return config;
    } catch (Exception e) {
      throw throwRuntimeException("{1}", path, e);
    }
  }

  public static String dump(Object configObject) {
    DumperOptions opts = new DumperOptions();
    opts.setPrettyFlow(true);
    opts.setCanonical(false);
    opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    String dump = new Yaml(opts).dump(configObject);
    // FIXME Could not find a better way to remove class tags from the dump, we use a regex here as a workaround.
    return dump.replaceAll("\\!\\!.*\n","").trim(); 
  }

  private static RuntimeException throwRuntimeException(String msg, Object... args) {
    String baseMsg = "Error loading config file \"{0}\". ";
    msg = MessageFormat.format(baseMsg + msg, args);
    throw new RuntimeException(msg);
  }

  private static InputStream inputStream(String path) throws IOException {
    Path p = Paths.get("config.yaml");
    return Files.newInputStream(p);
  }
}