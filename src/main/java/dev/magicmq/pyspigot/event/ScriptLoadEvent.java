package dev.magicmq.pyspigot.event;

import dev.magicmq.pyspigot.manager.script.Script;
import org.bukkit.event.HandlerList;

public class ScriptLoadEvent extends ScriptEvent {

    private static final HandlerList handlers = new HandlerList();

    public ScriptLoadEvent(Script script) {
        super(script);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
