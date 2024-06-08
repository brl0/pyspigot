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

package dev.magicmq.pyspigot.manager.listener;

import dev.magicmq.pyspigot.PySpigot;
import dev.magicmq.pyspigot.manager.script.Script;
import dev.magicmq.pyspigot.manager.script.ScriptManager;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Manager to interface with Bukkit's event framework. Primarily used by scripts to register and unregister event listeners.
 */
public class ListenerManager {

    private static ListenerManager manager;

    private final HashMap<Script, List<ScriptEventListener>> registeredListeners;

    private ListenerManager() {
        registeredListeners = new HashMap<>();
    }

    /**
     * Register a new event listener with default priority.
     * <p>
     * <b>Note:</b> This should be called from scripts only!
     * @param function The function that should be called when the event occurs
     * @param eventClass The type of event to listen to
     * @return The ScriptEventListener that was registered
     */
    public ScriptEventListener registerListener(Value function, Class<? extends Event> eventClass) {
        return registerListener(function, eventClass, EventPriority.NORMAL, false);
    }

    /**
     * Register a new event listener.
     * <p>
     * <b>Note:</b> This should be called from scripts only!
     * @param function The function that should be called when the event occurs
     * @param eventClass The type of event to listen to
     * @param priority The priority of the event relative to other listeners
     * @return The ScriptEventListener that was registered
     */
    public ScriptEventListener registerListener(Value function, Class<? extends Event> eventClass, EventPriority priority) {
        return registerListener(function, eventClass, priority, false);
    }

    /**
     * Register a new event listener with default priority.
     * <p>
     * <b>Note:</b> This should be called from scripts only!
     * @param function The function that should be called when the event occurs
     * @param eventClass The type of event to listen to
     * @param ignoreCancelled If true, the event listener will not be called if the event has been previously cancelled by another listener.
     * @return The ScriptEventListener that was registered
     */
    public ScriptEventListener registerListener(Value function, Class<? extends Event> eventClass, boolean ignoreCancelled) {
        return registerListener(function, eventClass, EventPriority.NORMAL, ignoreCancelled);
    }

    /**
     * Register a new event listener.
     * <p>
     * <b>Note:</b> This should be called from scripts only!
     * @param function The function that should be called when the event occurs
     * @param eventClass The type of event to listen to
     * @param priority The priority of the event relative to other listeners
     * @param ignoreCancelled If true, the event listener will not be called if the event has been previously cancelled by another listener.
     * @return The ScriptEventListener that was registered
     */
    public ScriptEventListener registerListener(Value function, Class<? extends Event> eventClass, EventPriority priority, boolean ignoreCancelled) {
        Script script = ScriptManager.get().getScript(Context.getCurrent());
        ScriptEventListener listener = getEventListener(script, eventClass);
        if (listener == null) {
            if (!function.canExecute())
                throw new RuntimeException("Event function must be a function (callable)");

            listener = new ScriptEventListener(script, function, eventClass);
            Bukkit.getPluginManager().registerEvent(eventClass, listener, priority, listener.getEventExecutor(), PySpigot.get(), ignoreCancelled);
            addListener(listener);
            return listener;
        } else {
            throw new RuntimeException("Script already has an event listener for '" + eventClass.getSimpleName() + "' registered");
        }
    }

    /**
     * Unregister an event listener.
     * <p>
     * <b>Note:</b> This should be called from scripts only!
     * @param listener The listener to unregister
     */
    public void unregisterListener(ScriptEventListener listener) {
        removeFromHandlers(listener);
        removeListener(listener);
    }

    /**
     * Get all event listeners associated with a script
     * @param script The script to get event listeners from
     * @return An immutable List of {@link ScriptEventListener} containing all events associated with the script. Will return null if there are no event listeners associated with the script
     */
    public List<ScriptEventListener> getListeners(Script script) {
        List<ScriptEventListener> scriptListeners = registeredListeners.get(script);
        if (scriptListeners != null)
            return new ArrayList<>(scriptListeners);
        else
            return null;
    }

    /**
     * Get the event listener for a particular event associated with a script
     * @param script The script
     * @param eventClass The event
     * @return The {@link ScriptEventListener} associated with the script and event, null if there is none
     */
    public ScriptEventListener getEventListener(Script script, Class<? extends Event> eventClass) {
        List<ScriptEventListener> scriptListeners = registeredListeners.get(script);
        if (scriptListeners != null) {
            for (ScriptEventListener listener : scriptListeners) {
                if (listener.getEvent().equals(eventClass))
                    return listener;
            }
        }
        return null;
    }

    /**
     * Unregister all event listeners belonging to a script.
     * @param script The script whose event listeners should be unregistered
     */
    public void unregisterListeners(Script script) {
        List<ScriptEventListener> associatedListeners = registeredListeners.get(script);
        if (associatedListeners != null) {
            for (ScriptEventListener eventListener : associatedListeners) {
                removeFromHandlers(eventListener);
            }
            registeredListeners.remove(script);
        }
    }

    private void removeFromHandlers(ScriptEventListener listener) {
        try {
            Method method = getRegistrationClass(listener.getEvent()).getDeclaredMethod("getHandlerList");
            method.setAccessible(true);
            HandlerList list = (HandlerList) method.invoke(null);
            list.unregister(listener);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            //This should not happen, because all events *should* have getHandlerList defined
            throw new RuntimeException("Unhandled exception when unregistering listener '" + listener.getEvent().getSimpleName() + "'", e);
        }
    }

    private void addListener(ScriptEventListener listener) {
        Script script = listener.getScript();
        if (registeredListeners.containsKey(script))
            registeredListeners.get(script).add(listener);
        else {
            List<ScriptEventListener> scriptListeners = new ArrayList<>();
            scriptListeners.add(listener);
            registeredListeners.put(script, scriptListeners);
        }
    }

    private void removeListener(ScriptEventListener listener) {
        Script script = listener.getScript();
        List<ScriptEventListener> scriptListeners = registeredListeners.get(script);
        scriptListeners.remove(listener);
        if (scriptListeners.isEmpty())
            registeredListeners.remove(script);
    }

    //Copied from org.bukkit.plugin.SimplePluginManager#getRegistrationClass. Resolves getHandlerList for events, including those where getHandlerList is defined in a superclass (such as BlockBreakEvent)
    private Class<? extends Event> getRegistrationClass(Class<? extends Event> clazz) {
        try {
            clazz.getDeclaredMethod("getHandlerList");
            return clazz;
        } catch (NoSuchMethodException e) {
            if (clazz.getSuperclass() != null
                    && !clazz.getSuperclass().equals(Event.class)
                    && Event.class.isAssignableFrom(clazz.getSuperclass())) {
                return getRegistrationClass(clazz.getSuperclass().asSubclass(Event.class));
            } else {
                throw new RuntimeException("Unable to find handler list for event " + clazz.getName() + ". Static getHandlerList method required!");
            }
        }
    }

    /**
     * Get the singleton instance of this ListenerManager.
     * @return The instance
     */
    public static ListenerManager get() {
        if (manager == null)
            manager = new ListenerManager();
        return manager;
    }
}
