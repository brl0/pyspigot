package dev.magicmq.pyspigot;

import dev.magicmq.pyspigot.command.PySpigotCommand;
import dev.magicmq.pyspigot.config.PluginConfig;
import dev.magicmq.pyspigot.manager.command.CommandManager;
import dev.magicmq.pyspigot.manager.config.ConfigManager;
import dev.magicmq.pyspigot.manager.listener.ListenerManager;
import dev.magicmq.pyspigot.manager.protocol.ProtocolManager;
import dev.magicmq.pyspigot.manager.script.ScriptManager;
import dev.magicmq.pyspigot.manager.task.TaskManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author magicmq
 */
public class PySpigot extends JavaPlugin {

    private static PySpigot instance;

    //Define static variables for ease of access by scripts
    public static ListenerManager listener;
    public static CommandManager command;
    public static TaskManager scheduler;
    public static ConfigManager config;
    public static ProtocolManager protocol;

    @Override
    public void onEnable() {
        instance = this;

        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        reloadConfig();

        getCommand("pyspigot").setExecutor(new PySpigotCommand());

        ScriptManager.get();
        listener = ListenerManager.get();
        command = CommandManager.get();
        scheduler = TaskManager.get();
        config = ConfigManager.get();

        if (isProtocolLibAvailable())
            protocol = ProtocolManager.get();
    }

    @Override
    public void onDisable() {
        ScriptManager.get().shutdown();
    }

    public void reload() {
        reloadConfig();
        PluginConfig.reload();
    }

    public boolean isProtocolLibAvailable() {
        return Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;
    }

    public static PySpigot get() {
        return instance;
    }
}
