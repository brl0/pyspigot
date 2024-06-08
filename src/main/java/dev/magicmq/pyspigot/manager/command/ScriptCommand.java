/*
 *    Copyright 2023 magicmq
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

package dev.magicmq.pyspigot.manager.command;

import dev.magicmq.pyspigot.PySpigot;
import dev.magicmq.pyspigot.manager.script.Script;
import dev.magicmq.pyspigot.manager.script.ScriptManager;
import dev.magicmq.pyspigot.util.CommandAliasHelpTopic;
import dev.magicmq.pyspigot.util.TypeLiterals;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.help.*;
import org.bukkit.plugin.Plugin;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Represents a command belonging to a script.
 * @see org.bukkit.command.TabExecutor
 * @see org.bukkit.command.defaults.BukkitCommand
 */
public class ScriptCommand implements TabExecutor {

    private final Script script;
    private final Value commandFunction;
    private final Value tabFunction;
    private final String name;
    private final PluginCommand bukkitCommand;

    private List<HelpTopic> helps;

    /**
     *
     * @param script The script to which this command belongs
     * @param commandFunction The command function that should be called when the command is executed
     * @param tabFunction The tab function that should be called for tab completion of the command. Can be null
     * @param name The name of the command to register
     * @param description The description of the command. Use an empty string for no description
     * @param usage The usage message for the command
     * @param aliases A List of String containing all the aliases for this command. Use an empty list for no aliases
     * @param permission The required permission node to use this command. Can be null
     * @param permissionMessage The message do display if there is insufficient permission to run the command. Can be null
     */
    public ScriptCommand(Script script, Value commandFunction, Value tabFunction, String name, String description, String usage, List<String> aliases, String permission, String permissionMessage) {
        this.script = script;
        this.commandFunction = commandFunction;
        this.tabFunction = tabFunction;
        this.name = name;

        try {
            final Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            final PluginCommand bukkitCommand = constructor.newInstance(name, PySpigot.get());
            bukkitCommand.setLabel(name.toLowerCase());
            bukkitCommand.setDescription(description);
            bukkitCommand.setUsage(usage);
            bukkitCommand.setAliases(aliases);
            bukkitCommand.setPermission(permission);
            bukkitCommand.setPermissionMessage(permissionMessage);
            bukkitCommand.setExecutor(this);
            this.bukkitCommand = bukkitCommand;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            //This should not happen, reflection checks done on plugin enable
            throw new RuntimeException("Unhandled exception when initializing command '" + name + "'", e);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        try {
            Value result = commandFunction.execute(sender, label, args);
            if (result.isBoolean())
                return result.asBoolean();
            else
                script.getLogger().log(Level.SEVERE, "Script command function '" + commandFunction + "' should return a boolean!");
        } catch (PolyglotException exception) {
            ScriptManager.get().handleScriptException(script, exception, "Unhandled exception when executing command '" + label + "'");
            //Mimic Bukkit behavior
            sender.sendMessage(ChatColor.RED + "An internal error occurred while attempting to perform this command");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (tabFunction != null) {
            try {
                Value result = tabFunction.execute(sender, alias, args);
                try {
                    return result.as(TypeLiterals.STRING_LIST);
                } catch (ClassCastException e) {
                    script.getLogger().log(Level.SEVERE, "Script tab complete function '" + tabFunction + "' should return a list of str!");
                    return Collections.emptyList();
                }
            } catch (PolyglotException exception) {
                ScriptManager.get().handleScriptException(script, exception,  "Unhandled exception when tab completing command '" + bukkitCommand.getLabel() + "'");
            }
        }
        return Collections.emptyList();
    }

    /**
     * Get the script associated with this command.
     * @return The script associated with this command
     */
    public Script getScript() {
        return script;
    }

    /**
     * Get the name of this command.
     * @return The name of this command
     */
    public String getName() {
        return name;
    }

    /**
     * Get the {@link org.bukkit.command.PluginCommand} that underlies this ScriptCommand
     * @return The underlying PluginCommand
     */
    public PluginCommand getBukkitCommand() {
        return bukkitCommand;
    }

    protected void initHelp() {
        helps = new ArrayList<>();
        HelpMap helpMap = Bukkit.getHelpMap();
        HelpTopic helpTopic = new GenericCommandHelpTopic(bukkitCommand);
        helpMap.addTopic(helpTopic);
        helps.add(helpTopic);

        HelpTopic aliases = helpMap.getHelpTopic("Aliases");
        if (aliases instanceof IndexHelpTopic) {
            aliases.getFullText(Bukkit.getConsoleSender());
            try {
                Field topics = IndexHelpTopic.class.getDeclaredField("allTopics");
                topics.setAccessible(true);
                List<HelpTopic> aliasTopics = new ArrayList<>((Collection<HelpTopic>) topics.get(aliases));
                for (String alias : bukkitCommand.getAliases()) {
                    HelpTopic toAdd = new CommandAliasHelpTopic("/" + alias, "/" + bukkitCommand.getLabel(), helpMap);
                    aliasTopics.add(toAdd);
                    helps.add(toAdd);
                }
                aliasTopics.sort(HelpTopicComparator.helpTopicComparatorInstance());
                topics.set(aliases, aliasTopics);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                //This should not happen, reflection checks done on plugin enable
                throw new RuntimeException("Unhandled exception when initializing command '" + name + "'", e);
            }
        }
    }

    protected void removeHelp() {
        Bukkit.getHelpMap().getHelpTopics().removeAll(helps);

        HelpTopic aliases = Bukkit.getHelpMap().getHelpTopic("Aliases");
        if (aliases instanceof IndexHelpTopic) {
            try {
                Field topics = IndexHelpTopic.class.getDeclaredField("allTopics");
                topics.setAccessible(true);
                List<HelpTopic> aliasTopics = new ArrayList<>((Collection<HelpTopic>) topics.get(aliases));
                aliasTopics.removeAll(helps);
                topics.set(aliases, aliasTopics);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                //This should not happen, reflection checks done on plugin enable
                throw new RuntimeException("Unhandled exception when unregistering command '" + name + "'", e);
            }
        }
    }

    /**
     * Prints a representation of this ScriptCommand in string format, including all variables that pertain to the command (such as name, label, description, etc.)
     * @return A string representation of the ScriptCommand
     */
    @Override
    public String toString() {
        return String.format("ScriptCommand[Name: %s, Label: %s, Description: %s, Usage: %s, Aliases: %s, Permission: %s, Permission Message: %s]",
                name,
                bukkitCommand.getLabel(),
                bukkitCommand.getDescription(),
                bukkitCommand.getUsage(),
                bukkitCommand.getAliases(),
                bukkitCommand.getPermission(),
                bukkitCommand.getPermissionMessage());
    }
}
