package me.lightningreflex.lightningutils.utils;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.lightningreflex.lightningutils.LightningUtils;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Placeholder {
    // this is terrible (someone please remake it or make a pr for a better implementation, perhaps MiniPlaceholders?)

    public static class Builder {
        // builder class to build a Placeholder object
        // with the placeholders with .addPlaceholder(key, value)
        // and a method to fill the placeholders with .fill(string)
        private final HashMap<String, String> placeholders = new HashMap<>();

        public Builder addPlaceholder(String key, String value) {
            // if it exists already error
//            if (placeholders.containsKey(key)) {
//                throw new IllegalArgumentException("Placeholder key already exists: " + key);
//            }
            // nvm lets not error, just overwrite
            // add a placeholder to the builder
            placeholders.put(key, value);
            return this;
        }

        public Builder addPlaceholder(String key, Player player) {
//            addPlaceholder(key, player.getUsername());
            addPlaceholder(key, "Player{" +
                "name=" + player.getUsername() + ", " +
                "uuid=" + player.getUniqueId().toString() +
            "}");
            addPlaceholder(key + ":name", player.getUsername());
            addPlaceholder(key + ":uuid", player.getUniqueId().toString());
            addPlaceholder(key + ":ip", player.getRemoteAddress().getAddress().getHostAddress());
            addPlaceholder(key + ":address", player.getRemoteAddress().getAddress().getHostAddress());
            addPlaceholder(key + ":ping", String.valueOf(player.getPing()));
//            addPlaceholder(key + ":getClientBrand", player.getClientBrand());
            // clientbrand is somehow always null
            addPlaceholder(key + ":protocol_version", String.valueOf(player.getProtocolVersion().getProtocol()));
            addPlaceholder(key + ":version", player.getProtocolVersion().getVersionIntroducedIn());

            // Servers
            if (player.getCurrentServer().isPresent()) {
                addPlaceholder(key + ":server", player.getCurrentServer().get().getServer());
            } else {
                addPlaceholder(key + ":server", "null");
            }
            return this;
        }

        public Builder addPlaceholder(String key, RegisteredServer server) {
//            addPlaceholder(key, server.getServerInfo().getName());
            addPlaceholder(key, "Server{" +
                "name=" + server.getServerInfo().getName() + ", " +
                "players=" + server.getPlayersConnected().size() +
            "}");
            addPlaceholder(key + ":name", server.getServerInfo().getName());
            addPlaceholder(key + ":ip", server.getServerInfo().getAddress().getHostName());
            addPlaceholder(key + ":address", server.getServerInfo().getAddress().getHostName());
            addPlaceholder(key + ":port", String.valueOf(server.getServerInfo().getAddress().getPort()));
            addPlaceholder(key + ":players", String.valueOf(server.getPlayersConnected().size()));
            return this;
        }

        public Builder addProxyPlaceholders() {
            String key = "proxy";
//            addPlaceholder(key, "proxy");
            addPlaceholder(key, "Proxy{" +
                "name=proxy, " +
                "players=" + LightningUtils.getProxy().getPlayerCount() + ", " +
                "servers=" + LightningUtils.getProxy().getAllServers().size() +
            "}");
            addPlaceholder(key + ":name", "proxy");
            addPlaceholder(key + ":ip", LightningUtils.getProxy().getBoundAddress().getAddress().getHostAddress());
            addPlaceholder(key + ":address", LightningUtils.getProxy().getBoundAddress().getAddress().getHostAddress());
            addPlaceholder(key + ":port", String.valueOf(LightningUtils.getProxy().getBoundAddress().getPort()));
            addPlaceholder(key + ":players", String.valueOf(LightningUtils.getProxy().getPlayerCount()));
            addPlaceholder(key + ":servers", String.valueOf(LightningUtils.getProxy().getAllServers().size()));
            return this;
        }

        public String fill(String string) {
            // fill the placeholders in the string
            String filled = string;
            for (String key : placeholders.keySet()) {
                String replacement = placeholders.get(key);
                if (replacement == null) {
                    replacement = "null-(" + key + ")";
                }
                filled = filled.replace("{" + key + "}", replacement);
            }
            return filled;
        }

        // component replace, so after minimessage has been applied (to prevent minimessage injection)
        public Component fill(Component component) {
            // regex pattern for {key}
            Pattern pattern = Pattern.compile("\\{([^}]*)\\}");
            return component.replaceText(compBuilder -> {
                // builder.match(pattern)
//                // doesn't support gradient/multi-colored variables cause it's ass :(
                compBuilder.match(pattern).replacement(matchResult -> {
//                    System.out.println(matchResult.content());
                    // get the key from the match
                    String key = matchResult.content();
                    // strip brackets off
                    key = key.substring(1, key.length() - 1);
                    // get the value from the key
                    String value = placeholders.get(key);
                    // return the value if it exists, otherwise return the key
                    return value != null ? Component.text(value) : Component.text(key);
                });
            });
        }
    }

    public static Builder builder() {
        // create a new builder
        return new Builder()
            .addProxyPlaceholders();
    }
}

