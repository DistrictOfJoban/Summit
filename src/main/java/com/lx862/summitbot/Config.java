package com.lx862.summitbot;

public class Config {
    public String token = null;
    public String brand = null;
    public PermissionConfig permissionConfig = new PermissionConfig(new String[0], new String[0]);
    public String[] autoModWatch = new String[0];
    public ServerManagers serverManagers = new ServerManagers(new Pterodactyl(false, null, null, null));
    public String sobEmoji = ":sob:";
    public String prefix = "!";
    public String mtrSysmapUrl = null;

    public record PermissionConfig(String[] staffRoles, String[] adminRoles) {}

    public record ServerManagers(Pterodactyl pterodactyl) {
    }

    public record Pterodactyl(boolean enabled, String url, String apiKey, String serverId) {
    }
}
