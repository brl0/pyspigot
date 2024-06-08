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

package dev.magicmq.pyspigot.manager.task;

import dev.magicmq.pyspigot.manager.script.Script;
import dev.magicmq.pyspigot.manager.script.ScriptManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

import java.util.Arrays;

/**
 * Represents a task defined by a script.
 */
public class Task extends BukkitRunnable {

    protected final Script script;
    protected final Value function;
    protected final Object[] functionArgs;
    protected final boolean async;
    protected final long delay;

    /**
     *
     * @param script The script associated with this task
     * @param function The script function that should be called when the task executes
     * @param functionArgs Any arguments that should be passed to the function
     * @param async True if the task is asynchronous, false if otherwise
     * @param delay The delay, in ticks, to wait until running the task
     */
    public Task(Script script, Value function, Object[] functionArgs, boolean async, long delay) {
        this.script = script;
        this.function = function;

        if (functionArgs != null) {
            int numOfFunctionArgs = function.getContext().eval("python", "import pyspigot;pyspigot.get_function_args").execute(function).asInt();
            if (numOfFunctionArgs < functionArgs.length)
                functionArgs = Arrays.copyOf(functionArgs, numOfFunctionArgs);
            this.functionArgs = functionArgs;
        } else
            this.functionArgs = null;

        this.async = async;
        this.delay = delay;
    }

    /**
     * Called internally when the task executes.
     */
    @Override
    public void run() {
        try {
            if (functionArgs != null) {
                function.executeVoid(functionArgs);
            } else {
                function.executeVoid();
            }
        } catch (PolyglotException e) {
            ScriptManager.get().handleScriptException(script, e, "Error when executing task #" + getTaskId());
        } finally {
            TaskManager.get().taskFinished(this);
        }
    }

    /**
     * Get the script associated with this task.
     * @return The script associated with this task
     */
    public Script getScript() {
        return script;
    }

    /**
     * Prints a representation of this Task in string format, including the task ID, if it is async, and delay (if applicable)
     * @return A string representation of the Task
     */
    @Override
    public String toString() {
        return String.format("Task[Task ID: %d, Async: %b, Delay: %d]", getTaskId(), async, (int) delay);
    }
}
