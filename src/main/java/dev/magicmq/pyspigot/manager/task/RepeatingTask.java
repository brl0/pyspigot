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
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyFunction;
import org.python.core.PyObject;

/**
 * Represents a repeating task defined by a script.
 */
public class RepeatingTask extends Task {

    private final long interval;

    /**
     *
     * @param script The script associated with this repeating task
     * @param function The script function that should be called every time the repeating task executes
     * @param functionArgs Any arguments that should be passed to the function
     */
    public RepeatingTask(Script script, PyFunction function, Object[] functionArgs, boolean async, long delay, long interval) {
        super(script, function, functionArgs, async, delay);
        this.interval = interval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            if (functionArgs != null) {
                PyObject[] pyObjects = Py.javas2pys(functionArgs);
                function.__call__(pyObjects);
            } else {
                function.__call__();
            }
        } catch (PyException e) {
            ScriptManager.get().handleScriptException(script, e, "Error when executing task #" + getTaskId());
        }
    }

    /**
     * Prints a representation of this RepeatingTask in string format, including the task ID, if it is async, delay (if applicable), and interval (if applicable)
     * @return A string representation of the RepeatingTask
     */
    @Override
    public String toString() {
        return String.format("RepeatingTask[Task ID: %d, Async: %b, Delay: %d, Interval: %d]", getTaskId(), async, (int) delay, (int) interval);
    }
}
