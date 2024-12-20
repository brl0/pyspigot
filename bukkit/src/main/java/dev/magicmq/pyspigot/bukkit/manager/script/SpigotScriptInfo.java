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

package dev.magicmq.pyspigot.bukkit.manager.script;

import dev.magicmq.pyspigot.bukkit.PySpigot;
import dev.magicmq.pyspigot.bukkit.manager.placeholder.PlaceholderManager;
import dev.magicmq.pyspigot.bukkit.manager.placeholder.ScriptPlaceholder;
import dev.magicmq.pyspigot.bukkit.manager.protocol.ProtocolManager;
import dev.magicmq.pyspigot.bukkit.manager.protocol.ScriptPacketListener;
import dev.magicmq.pyspigot.manager.script.Script;
import dev.magicmq.pyspigot.manager.script.ScriptInfo;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class that fetches and returns a script's info (for the /pyspigot info command).
 */
public class SpigotScriptInfo extends ScriptInfo {

    private static SpigotScriptInfo instance;

    private SpigotScriptInfo() {
        super();
    }

    /**
     * {@inheritDoc}
     * @param script The script whose information should be printed
     * @param appendTo The info StringBuilder that platform-specific manager info should be appended to
     */
    @Override
    public void printPlatformManagerInfo(Script script, StringBuilder appendTo) {
        if (PySpigot.get().isPlaceholderApiAvailable()) {
            ScriptPlaceholder placeholder = PlaceholderManager.get().getPlaceholder(script);
            if (placeholder != null)
                appendTo.append(ChatColor.GOLD + "Registered placeholder: " + ChatColor.RESET + placeholder + "\n");
            else
                appendTo.append(ChatColor.GOLD + "Registered placeholder: " + ChatColor.RESET + "None" + "\n");
        }

        if (PySpigot.get().isProtocolLibAvailable()) {
            List<ScriptPacketListener> registeredPacketListeners = ProtocolManager.get().getPacketListeners(script);
            List<String> packetTypes = new ArrayList<>();
            if (registeredPacketListeners != null)
                registeredPacketListeners.forEach(listener -> packetTypes.add(listener.toString()));
            appendTo.append(ChatColor.GOLD + "Listening to packet types: " + ChatColor.RESET + packetTypes + "\n");

            List<ScriptPacketListener> registeredPacketListenersAsync = ProtocolManager.get().async().getAsyncPacketListeners(script);
            List<String> packetTypesAsync = new ArrayList<>();
            if (registeredPacketListenersAsync != null)
                registeredPacketListenersAsync.forEach(listener -> packetTypesAsync.add(listener.toString()));
            appendTo.append(ChatColor.GOLD + "Listening to packet types (async): " + ChatColor.RESET + packetTypesAsync + "\n");
        }
    }

    /**
     * Get the singleton instance of this SpigotScriptInfo.
     * @return The instance
     */
    public static SpigotScriptInfo get() {
        if (instance == null)
            instance = new SpigotScriptInfo();
        return instance;
    }
}
