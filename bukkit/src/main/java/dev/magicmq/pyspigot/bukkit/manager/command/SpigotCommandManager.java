/*
 *    Copyright 2025 magicmq
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package dev.magicmq.pyspigot.bukkit.manager.command;

import dev.magicmq.pyspigot.PyCore;
import dev.magicmq.pyspigot.bukkit.util.ReflectionUtils;
import dev.magicmq.pyspigot.manager.command.CommandManager;
import dev.magicmq.pyspigot.manager.script.Script;
import dev.magicmq.pyspigot.util.ScriptUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.python.core.PyFunction;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * Manager to interface with Bukkit's command framework. Primarily used by scripts to register and unregister commands.
 */
public class SpigotCommandManager extends CommandManager<SpigotScriptCommand> {

    private static SpigotCommandManager manager;

    private final Method bSyncCommands;

    private SimpleCommandMap bCommandMap;
    private HashMap<String, Command> bKnownCommands;

    private SpigotCommandManager() {
        super();

        bSyncCommands = ReflectionUtils.getMethod(Bukkit.getServer().getClass(), "syncCommands");
        if (bSyncCommands != null)
            bSyncCommands.setAccessible(true);
        try {
            bCommandMap = getCommandMap();
            bKnownCommands = getKnownCommands(bCommandMap);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            //This should not happen, reflection checks done on plugin enable
            PyCore.get().getLogger().log(Level.SEVERE, "Error when initializing command manager:", e);
        }
    }

    @Override
    public SpigotScriptCommand registerCommand(PyFunction commandFunction, String name) {
        return registerCommand(commandFunction, null, name, "", "", new ArrayList<>(), null);
    }

    @Override
    public SpigotScriptCommand registerCommand(PyFunction commandFunction, PyFunction tabFunction, String name) {
        return registerCommand(commandFunction, tabFunction, name, "", "", new ArrayList<>(), null);
    }

    @Override
    public SpigotScriptCommand registerCommand(PyFunction commandFunction, String name, String permission) {
        return registerCommand(commandFunction, null, name, "", "", new ArrayList<>(), permission);
    }

    @Override
    public SpigotScriptCommand registerCommand(PyFunction commandFunction, PyFunction tabFunction, String name, String permission) {
        return registerCommand(commandFunction, tabFunction, name, "", "", new ArrayList<>(), permission);
    }

    @Override
    public SpigotScriptCommand registerCommand(PyFunction commandFunction, String name, List<String> aliases, String permission) {
        return registerCommand(commandFunction, null, name, "", "", new ArrayList<>(), null);
    }

    @Override
    public SpigotScriptCommand registerCommand(PyFunction commandFunction, PyFunction tabFunction, String name, List<String> aliases, String permission) {
        return registerCommand(commandFunction, tabFunction, name, "", "", aliases, null);
    }

    @Override
    public SpigotScriptCommand registerCommand(PyFunction commandFunction, String name, String description, String usage) {
        return registerCommand(commandFunction, null, name, description, usage, new ArrayList<>(), null);
    }

    @Override
    public SpigotScriptCommand registerCommand(PyFunction commandFunction, PyFunction tabFunction, String name, String description, String usage) {
        return registerCommand(commandFunction, tabFunction, name, description, usage, new ArrayList<>(), null);
    }

    @Override
    public SpigotScriptCommand registerCommand(PyFunction commandFunction, String name, String description, String usage, List<String> aliases) {
        return registerCommand(commandFunction, null, name, description, usage, aliases, null);
    }

    @Override
    public SpigotScriptCommand registerCommand(PyFunction commandFunction, PyFunction tabFunction, String name, String description, String usage, List<String> aliases) {
        return registerCommand(commandFunction, tabFunction, name, description, usage, aliases, null);
    }

    @Override
    public SpigotScriptCommand registerCommand(PyFunction commandFunction, PyFunction tabFunction, String name, String description, String usage, List<String> aliases, String permission) {
        Script script = ScriptUtils.getScriptFromCallStack();
        SpigotScriptCommand command = getCommand(script, name);
        if (command == null) {
            SpigotScriptCommand newCommand = new SpigotScriptCommand(script, commandFunction, tabFunction, name, description, usage, aliases, permission);
            if (!addCommandToBukkit(newCommand))
                script.getLogger().log(Level.WARNING, "Used fallback prefix (script name) when registering command '" + name + "'");
            syncBukkitCommands();
            newCommand.initHelp();
            addCommand(script, newCommand);
            return newCommand;
        } else
            throw new RuntimeException("Command '" + name + "' is already registered");
    }

    @Override
    public void unregisterCommand(SpigotScriptCommand command) {
        removeCommandFromBukkit(command);
        command.removeHelp();
        syncBukkitCommands();
        removeCommand(command.getScript(), command);
    }

    @Override
    public void unregisterCommands(Script script) {
        List<SpigotScriptCommand> associatedCommands = getCommands(script);
        if (associatedCommands != null) {
            for (SpigotScriptCommand command : associatedCommands) {
                removeCommandFromBukkit(command);
                command.removeHelp();
            }
            removeCommands(script);
            syncBukkitCommands();
        }
    }

    @Override
    public SpigotScriptCommand getCommand(Script script, String name) {
        List<SpigotScriptCommand> scriptCommands = getCommands(script);
        if (scriptCommands != null) {
            for (SpigotScriptCommand command : scriptCommands) {
                if (command.getName().equalsIgnoreCase(name))
                    return command;
            }
        }
        return null;
    }

    private boolean addCommandToBukkit(SpigotScriptCommand command) {
        return bCommandMap.register(command.getScript().getName(), command.getBukkitCommand());
    }

    private void removeCommandFromBukkit(SpigotScriptCommand command) {
        command.getBukkitCommand().unregister(bCommandMap);
        bKnownCommands.remove(command.getBukkitCommand().getLabel());
        for (String alias : command.getBukkitCommand().getAliases())
            bKnownCommands.remove(alias);
    }

    private void syncBukkitCommands() {
        if (bSyncCommands != null) {
            try {
                bSyncCommands.invoke(Bukkit.getServer());
            } catch (IllegalAccessException | InvocationTargetException e) {
                //This should not happen
                throw new RuntimeException("Unhandled exception when syncing commands", e);
            }
        }
    }

    private SimpleCommandMap getCommandMap() throws NoSuchFieldException, IllegalAccessException {
        Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        field.setAccessible(true);
        return (SimpleCommandMap) field.get(Bukkit.getServer());
    }

    @SuppressWarnings("unchecked")
    private HashMap<String, Command> getKnownCommands(SimpleCommandMap commandMap) throws NoSuchFieldException, IllegalAccessException {
        Field field = SimpleCommandMap.class.getDeclaredField("knownCommands");
        field.setAccessible(true);
        return (HashMap<String, Command>) field.get(commandMap);
    }

    /**
     * Get the singleton instance of this SpigotCommandManager.
     * @return The instance
     */
    public static SpigotCommandManager get() {
        if (manager == null)
            manager = new SpigotCommandManager();
        return manager;
    }
}

