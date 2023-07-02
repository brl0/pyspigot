package dev.magicmq.pyspigot.manager.placeholder;

import dev.magicmq.pyspigot.manager.script.Script;
import dev.magicmq.pyspigot.manager.script.ScriptManager;
import org.python.core.PyBaseCode;
import org.python.core.PyFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager to interface with PlaceholderAPI. Primarily used by scripts to register and unregister placeholder expansions.
 * <p>
 * Do not call this manager if PlaceholderAPI is not loaded and enabled on the server! It will not work.
 */
public class PlaceholderManager {

    private static PlaceholderManager instance;

    private List<ScriptPlaceholder> registeredPlaceholders;

    private PlaceholderManager() {
        registeredPlaceholders = new ArrayList<>();
    }

    /**
     * Register a new script placeholder expansion.
     * <p>
     * <b>Note:</b> This should be called from scripts only!
     * @param placeholderFunction The function that should be called when the placeholder is used
     * @return A {@link ScriptPlaceholder} representing the placeholder expansion that was registered
     */
    public ScriptPlaceholder registerPlaceholder(PyFunction placeholderFunction) {
        return registerPlaceholder(placeholderFunction, "Script Author", "1.0.0");
    }

    /**
     * Register a new script placeholder expansion.
     * <p>
     * <b>Note:</b> This should be called from scripts only!
     * @param placeholderFunction The function that should be called when the placeholder is used
     * @param author The author of the placeholder
     * @param version The version of the placeholder
     * @return A {@link ScriptPlaceholder} representing the placeholder expansion that was registered
     */
    public ScriptPlaceholder registerPlaceholder(PyFunction placeholderFunction, String author, String version) {
        Script script = ScriptManager.get().getScript(((PyBaseCode) placeholderFunction.__code__).co_filename);
        if (getPlaceholder(script) == null) {
            ScriptPlaceholder placeholder = new ScriptPlaceholder(script, placeholderFunction, author, version);
            placeholder.register();
            registeredPlaceholders.add(placeholder);
            return placeholder;
        } else
            throw new UnsupportedOperationException("Script already has a placeholder expansion registered!");
    }

    /**
     * Unregister a script placeholder expansion.
     * <p>
     * <b>Note:</b> This should be called from scripts only!
     * @param placeholder The placeholder expansion to unregister
     */
    public void unregisterPlaceholder(ScriptPlaceholder placeholder) {
        placeholder.unregister();
        registeredPlaceholders.remove(placeholder);
    }

    /**
     * Unregister a script's placeholder expansion.
     * <p>
     * Similar to {@link #unregisterPlaceholder(ScriptPlaceholder)}, except this method can be called from outside a script to unregister a script's placeholder expansion (for example when the script is unloaded and stopped).
     * @param script The script whose placeholder should be unregistered
     * @return True if a placeholder was unregistered, false if the script did not have a placeholder registered previously
     */
    public boolean unregisterPlaceholder(Script script) {
        ScriptPlaceholder placeholder = getPlaceholder(script);
        if (placeholder != null) {
            unregisterPlaceholder(placeholder);
            return true;
        }
        return false;
    }

    /**
     * Get a script's placeholder expansion.
     * @param script The script to get the placeholder expansion from
     * @return The script's placeholder expansion, null if the script does not have a placeholder expansion registered
     */
    public ScriptPlaceholder getPlaceholder(Script script) {
        for (ScriptPlaceholder placeholder : registeredPlaceholders) {
            if (placeholder.getScript().equals(script))
                return placeholder;
        }
        return null;
    }

    /**
     * Get the singleton instance of this PlaceholderManager.
     * @return The instance
     */
    public static PlaceholderManager get() {
        if (instance == null)
            instance = new PlaceholderManager();
        return instance;
    }
}