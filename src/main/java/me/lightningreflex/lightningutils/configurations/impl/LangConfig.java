package me.lightningreflex.lightningutils.configurations.impl;

import lombok.Getter;
import me.lightningreflex.lightningutils.LightningUtils;
import me.lightningreflex.lightningutils.utils.Utils;
import me.lightningreflex.lightningutils.configurations.Config;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;

@Getter
public class LangConfig extends Config {
    // The config classes are horrible, someone please improve it :(
    //------------------------------------------------
    public Commands commands;
    @Getter
    public static class Commands {
        public Send send;
        @Getter
        public static class Send {
//            public String success_executor;
//            public String warning_player;
            public Successes successes;
            @Getter
            public static class Successes {
                public String player_to_server;
                public String player_to_player;
                public String server_to_server;
                public String server_to_player;
            }
            public Warnings warnings;
            @Getter
            public static class Warnings {
                public String player_to_server;
                public String player_to_player;
                public String server_to_server;
                public String server_to_player;
            }
            public Server_Does_Not_Exist server_does_not_exist;
            @Getter
            public static class Server_Does_Not_Exist {
                public String from;
                public String to;
            }
            public Player_Offline player_offline;
            @Getter
            public static class Player_Offline {
                public String from;
                public String to;
            }

            public Arguments arguments;
            @Getter
            public static class Arguments {
                public String from;
                public String to;
                public String invalid_syntax;
            }
        }

        public Alert alert;
        @Getter
        public static class Alert {
            public String message;

            public Arguments arguments;
            @Getter
            public static class Arguments {
                public String message;
                public String invalid_syntax;
            }
        }

        public Lobby lobby;
        @Getter
        public static class Lobby {
            public String already_in_lobby;
            public String success;
            public String server_does_not_exist;
            public String is_not_lobby;

            public Arguments arguments;
            @Getter
            public static class Arguments {
                public String server;
                public String invalid_syntax;
            }
        }

        public StaffChat staffchat;
        @Getter
        public static class StaffChat {
            public String message;
            public String toggle_disabled;
            public String enabled;
            public String disabled;

            public Arguments arguments;
            @Getter
            public static class Arguments {
                public String message;
                public String invalid_syntax;
            }
        }

        public Sudo sudo;
        @Getter
        public static class Sudo {
            public String success;
            public String player_not_found;
            public String notify;

            public Arguments arguments;
            @Getter
            public static class Arguments {
                public String player;
                public String text;
                public String invalid_syntax;
            }
        }

        public Find find;
        @Getter
        public static class Find {
            public String success;
            public String player_not_found;

            public Arguments arguments;
            @Getter
            public static class Arguments {
                public String player;
                public String invalid_syntax;
            }
        }

        public Ip ip;
        @Getter
        public static class Ip {
            public String success;
            public String player_not_found;

            public Arguments arguments;
            @Getter
            public static class Arguments {
                public String player;
                public String invalid_syntax;
            }
        }
    }

    public double lang_version;
    //------------------------------------------------

    public LangConfig load(final String path) {
        // load from file datadirectory/path, if it doesn't exist, copy from resources path
        Path dataPath = LightningUtils.getDataDirectory();
        Path configPath = dataPath.resolve(path);
        if (!configPath.toFile().exists()) {
            // Ensure folders exist
            dataPath.toFile().mkdirs();
            // Copy from resources
            Utils.ExportResource(path, configPath.toString().substring(configPath.toString().indexOf("/") + 1));
        }
        // for development always export
//        dataPath.toFile().mkdirs();
//        Utils.ExportResource(path, configPath.toString().substring(configPath.toString().indexOf("/") + 1));

        // Load from file
        try {
            versionValidate(new FileInputStream(configPath.toFile()), path);
            InputStream inputStream = new FileInputStream(configPath.toFile());
            //            yaml.load(inputStream);
            LangConfig config = new Yaml(new Constructor(LangConfig.class, new LoaderOptions())).load(inputStream);
            // validate
            LangConfig validatedConfig = config.validate(path, config);
            return validatedConfig;
        } catch (Exception e) {
            LightningUtils.getLogger().error("Failed to load config file " + path);
            e.printStackTrace();
        }
        return null;
    }

    public LangConfig validate(final String path, LangConfig currentConfig) {
        // move lang.yml to lang-version.yml and create new lang.yml
//        if (currentConfig.getLang_version() != 1.1) {
//            // move
//            Path dataPath = LightningUtils.getDataDirectory();
//            Path configPath = dataPath.resolve(path);
//            // remove end from . and add -version.yml
//            String oldExtension = configPath.toString().substring(configPath.toString().lastIndexOf("."));
//            String newExtension = "-" + currentConfig.getLang_version() + ".yml";
//            Path oldConfigPath = dataPath.resolve(path.replace(oldExtension, newExtension));
//            // error
//            LightningUtils.getLogger().error("Invalid yml version, moving previous yml file to " + oldConfigPath.getFileName() + " and creating new " + configPath.getFileName());
//            // move
//            configPath.toFile().renameTo(oldConfigPath.toFile());
//            // create new
//            return load(path);
//        }
        return currentConfig;
    }

    public void versionValidate(InputStream inputStream, String path) {
        // get the last line of the file
        StringBuilder lastLine = new StringBuilder();
        try {
            while (inputStream.available() > 0) {
                char c = (char) inputStream.read();
                if (c == '\n') {
                    lastLine = new StringBuilder();
                } else {
                    lastLine.append(c);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // split at ": " and get the second part and parse to double
        double version = Double.parseDouble(lastLine.toString().split(": ")[1]);

        // replace file
        if (version != 1.1) {
            // move
            Path dataPath = LightningUtils.getDataDirectory();
            Path configPath = dataPath.resolve(path);
            // remove end from . and add -version.yml
            String oldExtension = configPath.toString().substring(configPath.toString().lastIndexOf("."));
            String newExtension = "-" + version + ".yml";
            Path oldConfigPath = dataPath.resolve(path.replace(oldExtension, newExtension));
            // error
            LightningUtils.getLogger().error("Invalid yml version, moving previous yml file to " + oldConfigPath.getFileName() + " and creating new " + configPath.getFileName());
            // move
            configPath.toFile().renameTo(oldConfigPath.toFile());
            // create new
            Utils.ExportResource(path, configPath.toString().substring(configPath.toString().indexOf("/") + 1));
        }
    }
}
