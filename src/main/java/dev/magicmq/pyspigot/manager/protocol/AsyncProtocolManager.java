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

package dev.magicmq.pyspigot.manager.protocol;

import com.comphenix.protocol.AsynchronousManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.async.AsyncListenerHandler;
import com.comphenix.protocol.events.ListenerPriority;
import dev.magicmq.pyspigot.manager.script.Script;
import dev.magicmq.pyspigot.manager.script.ScriptManager;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Manager to interface with ProtocolLib's AsynchronousManager. Primarily used by scripts to register and unregister asynchronous packet listeners.
 * @see com.comphenix.protocol.AsynchronousManager
 */
public class AsyncProtocolManager {

    private final com.comphenix.protocol.AsynchronousManager asynchronousManager;
    private final HashMap<Script, List<ScriptPacketListener>> registeredAsyncListeners;

    protected AsyncProtocolManager() {
        asynchronousManager = ProtocolLibrary.getProtocolManager().getAsynchronousManager();
        registeredAsyncListeners = new HashMap<>();
    }

    /**
     * Get the current ProtocolLib AsynchronousManager.
     * @return The {@link com.comphenix.protocol.AsynchronousManager}
     */
    public AsynchronousManager getAsynchronousManager() {
        return asynchronousManager;
    }

    /**
     * Register a new asynchronous packet listener with default priority.
     * <p>
     * This method will automatically register a {@link PacketReceivingListener} or a {@link PacketSendingListener}, depending on if the {@link PacketType} is for the server or client, respectively.
     * <p>
     * <b>Note:</b> This should be called from scripts only!
     * @param function The function that should be called when the packet event occurs
     * @param type The packet type to listen for
     * @return A {@link ScriptPacketListener} representing the asynchronous packet listener that was registered
     */
    public ScriptPacketListener registerAsyncPacketListener(Value function, PacketType type) {
        return registerAsyncPacketListener(function, type, ListenerPriority.NORMAL);
    }

    /**
     * Register a new asynchronous packet listener.
     * <p>
     * This method will automatically register a {@link PacketReceivingListener} or a {@link PacketSendingListener}, depending on if the {@link PacketType} is for the server or client, respectively.
     * <p>
     * <b>Note:</b> This should be called from scripts only!
     * @param function The function that should be called when the packet event occurs
     * @param type The packet type to listen for
     * @param priority The priority of the asynchronous packet listener relative to other packet listeners
     * @return A {@link ScriptPacketListener} representing the asynchronous packet listener that was registered
     */
    public ScriptPacketListener registerAsyncPacketListener(Value function, PacketType type, ListenerPriority priority) {
        Script script = ScriptManager.get().getScript(Context.getCurrent());
        if (getAsyncPacketListener(script, type) == null) {
            if (!function.canExecute())
                throw new RuntimeException("Listener function must be a function (callable)");

            ScriptPacketListener listener = null;
            if (type.getSender() == PacketType.Sender.CLIENT) {
                listener = new PacketReceivingListener(script, function, type, priority, ListenerType.ASYNCHRONOUS);
                addAsyncPacketListener(listener);
                AsyncListenerHandler handler = asynchronousManager.registerAsyncHandler(listener);
                handler.start();
            } else if (type.getSender() == PacketType.Sender.SERVER) {
                listener = new PacketSendingListener(script, function, type, priority, ListenerType.ASYNCHRONOUS);
                addAsyncPacketListener(listener);
                AsyncListenerHandler handler = asynchronousManager.registerAsyncHandler(listener);
                handler.start();
            }
            return listener;
        } else
            throw new RuntimeException("Script already has an async packet listener for '" + type.name() + "' registered");
    }

    /**
     * Register a new asynchronous timeout packet listener with default priority.
     * <p>
     * This method will automatically register a {@link PacketReceivingListener} or a {@link PacketSendingListener}, depending on if the {@link PacketType} is for the server or client, respectively.
     * <p>
     * <b>Note:</b> This should be called from scripts only!
     * @param function The function that should be called when the packet event occurs
     * @param type The packet type to listen for
     * @return A {@link ScriptPacketListener} representing the asynchronous timeout packet listener that was registered
     */
    public ScriptPacketListener registerTimeoutPacketListener(Value function, PacketType type) {
        return registerTimeoutPacketListener(function, type, ListenerPriority.NORMAL);
    }

    /**
     * Register a new asynchronous timeout packet listener.
     * <p>
     * This method will automatically register a {@link PacketReceivingListener} or a {@link PacketSendingListener}, depending on if the {@link PacketType} is for the server or client, respectively.
     * <p>
     * <b>Note:</b> This should be called from scripts only!
     * @param function The function that should be called when the packet event occurs
     * @param type The packet type to listen for
     * @param priority The priority of the asynchronous timeout packet listener relative to other asynchronous timeout packet listeners
     * @return A {@link ScriptPacketListener} representing the asynchronous timeout packet listener that was registered
     *
     */
    public ScriptPacketListener registerTimeoutPacketListener(Value function, PacketType type, ListenerPriority priority) {
        Script script = ScriptManager.get().getScript(Context.getCurrent());
        if (getAsyncPacketListener(script, type) == null) {
            if (!function.canExecute())
                throw new RuntimeException("Listener function must be a function (callable)");

            ScriptPacketListener listener = null;
            if (type.getSender() == PacketType.Sender.CLIENT) {
                listener = new PacketReceivingListener(script, function, type, priority, ListenerType.ASYNCHRONOUS_TIMEOUT);
                addAsyncPacketListener(listener);
                asynchronousManager.registerTimeoutHandler(listener);
            } else if (type.getSender() == PacketType.Sender.SERVER) {
                listener = new PacketSendingListener(script, function, type, priority, ListenerType.ASYNCHRONOUS_TIMEOUT);
                addAsyncPacketListener(listener);
                asynchronousManager.registerTimeoutHandler(listener);
            }
            return listener;
        } else
            throw new RuntimeException("Script already has an async packet listener for '" + type.name() + "' registered");
    }

    /**
     * Unregister an asynchronous packet listener.
     * <p>
     * <b>Note:</b> This should be called from scripts only!
     * @param listener The asynchronous packet listener to unregister
     */
    public void unregisterAsyncPacketListener(ScriptPacketListener listener) {
        if (listener.getListenerType() == ListenerType.ASYNCHRONOUS) {
            asynchronousManager.unregisterAsyncHandler(listener);
            removeAsyncPacketListener(listener);
        } else if (listener.getListenerType() == ListenerType.ASYNCHRONOUS_TIMEOUT) {
            asynchronousManager.unregisterTimeoutHandler(listener);
            removeAsyncPacketListener(listener);
        }
    }

    /**
     * Unregister all asynchronous packet listeners belonging to a script.
     * @param script The script whose asynchronous packet listeners should be unregistered
     */
    public void unregisterAsyncPacketListeners(Script script) {
        List<ScriptPacketListener> scriptPacketListeners = registeredAsyncListeners.get(script);
        if (scriptPacketListeners != null) {
            for (ScriptPacketListener listener : scriptPacketListeners) {
                if (listener.getListenerType() == ListenerType.ASYNCHRONOUS)
                    asynchronousManager.unregisterAsyncHandler(listener);
                else if (listener.getListenerType() == ListenerType.ASYNCHRONOUS_TIMEOUT)
                    asynchronousManager.unregisterTimeoutHandler(listener);
            }
            registeredAsyncListeners.remove(script);
        }
    }

    /**
     * Get all asynchronous packet listeners associated with a script
     * @param script The script to get asynchronous packet listeners from
     * @return A List of {@link ScriptPacketListener} containing all asynchronous packet listeners associated with this script. Will return null if there are no asynchronous packet listeners associated with the script
     */
    public List<ScriptPacketListener> getAsyncPacketListeners(Script script) {
        return registeredAsyncListeners.get(script);
    }

    /**
     * Get the asynchronous packet listener for a particular packet type associated with a script
     * @param script The script
     * @param packetType The packet type
     * @return The {@link ScriptPacketListener} associated with the script and packet type, null if there is none
     */
    public ScriptPacketListener getAsyncPacketListener(Script script, PacketType packetType) {
        List<ScriptPacketListener> scriptAsyncPacketListeners = registeredAsyncListeners.get(script);
        if (scriptAsyncPacketListeners != null) {
            for (ScriptPacketListener listener : scriptAsyncPacketListeners) {
                if (listener.getPacketType() == packetType)
                    return listener;
            }
        }
        return null;
    }

    private void addAsyncPacketListener(ScriptPacketListener listener) {
        Script script = listener.getScript();
        if (registeredAsyncListeners.containsKey(script))
            registeredAsyncListeners.get(script).add(listener);
        else {
            List<ScriptPacketListener> scriptAsyncPacketListeners = new ArrayList<>();
            scriptAsyncPacketListeners.add(listener);
            registeredAsyncListeners.put(script, scriptAsyncPacketListeners);
        }
    }

    private void removeAsyncPacketListener(ScriptPacketListener listener) {
        Script script = listener.getScript();
        List<ScriptPacketListener> scriptAsyncPacketListeners = registeredAsyncListeners.get(script);
        scriptAsyncPacketListeners.remove(listener);
        if (scriptAsyncPacketListeners.isEmpty())
            registeredAsyncListeners.remove(script);
    }
}
