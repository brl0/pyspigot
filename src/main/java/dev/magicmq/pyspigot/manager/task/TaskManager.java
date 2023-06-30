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

import dev.magicmq.pyspigot.PySpigot;
import dev.magicmq.pyspigot.manager.script.Script;
import dev.magicmq.pyspigot.manager.script.ScriptManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.python.core.PyBaseCode;
import org.python.core.PyFunction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manager to interface with Bukkit's scheduler. Primarily used by scripts to register and unregister tasks.
 */
public class TaskManager {

    private static TaskManager manager;

    private final List<RepeatingTask> repeatingTasks;

    private TaskManager() {
        repeatingTasks = new ArrayList<>();
    }

    /**
     * <strong>WARNING: This should be called from scripts only!</strong>
     * <p>
     * Schedule a new synchronous task.
     * @param function The function that should be called when the synchronous task executes
     * @return An ID representing the synchronous task that was scheduled
     */
    public int runTask(PyFunction function) {
        Script script = ScriptManager.get().getScript(((PyBaseCode) function.__code__).co_filename);
        Task task = new Task(script, function);
        return Bukkit.getScheduler().runTask(PySpigot.get(), task).getTaskId();
    }

    /**
     * <strong>WARNING: This should be called from scripts only!</strong>
     * <p>
     * Schedule a new asynchronous task.
     * @param function The function that should be called when the asynchronous task executes
     * @return An ID representing the asynchronous task that was scheduled
     */
    public int runTaskAsync(PyFunction function) {
        Script script = ScriptManager.get().getScript(((PyBaseCode) function.__code__).co_filename);
        Task task = new Task(script, function);
        return Bukkit.getScheduler().runTaskAsynchronously(PySpigot.get(), task).getTaskId();
    }

    /**
     * <strong>WARNING: This should be called from scripts only!</strong>
     * <p>
     * Schedule a new synchronous task to run at a later point in time.
     * @param function The function that should be called when the synchronous task executes
     * @param delay The delay, in ticks, that the scheduler should wait before executing the synchronous task
     * @return An ID representing the synchronous task that was scheduled
     */
    public int runTaskLater(PyFunction function, long delay) {
        Script script = ScriptManager.get().getScript(((PyBaseCode) function.__code__).co_filename);
        Task task = new Task(script, function);
        return Bukkit.getScheduler().runTaskLater(PySpigot.get(), task, delay).getTaskId();
    }

    /**
     * <strong>WARNING: This should be called from scripts only!</strong>
     * <p>
     * Schedule a new asynchronous task to run at a later point in time.
     * @param function The function that should be called when the asynchronous task executes
     * @param delay The delay, in ticks, that the scheduler should wait before executing the asynchronous task
     * @return An ID representing the asynchronous task that was scheduled
     */
    public int runTaskLaterAsync(PyFunction function, long delay) {
        Script script = ScriptManager.get().getScript(((PyBaseCode) function.__code__).co_filename);
        Task task = new Task(script, function);
        return Bukkit.getScheduler().runTaskLaterAsynchronously(PySpigot.get(), task, delay).getTaskId();
    }

    /**
     * <strong>WARNING: This should be called from scripts only!</strong>
     * <p>
     * Schedule a new synchronous repeating task.
     * @param function The function that should be called each time the synchronous task executes
     * @param delay The interval, in ticks, that the synchronous task should be executed
     * @return An ID representing the synchronous task that was scheduled
     */
    public int scheduleRepeatingTask(PyFunction function, long delay, long interval) {
        Script script = ScriptManager.get().getScript(((PyBaseCode) function.__code__).co_filename);
        RepeatingTask task = new RepeatingTask(script, function);
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(PySpigot.get(), task, delay, interval);
        task.setTaskId(bukkitTask.getTaskId());
        repeatingTasks.add(task);
        return bukkitTask.getTaskId();
    }

    /**
     * <strong>WARNING: This should be called from scripts only!</strong>
     * <p>
     * Schedule a new asynchronous repeating task.
     * @param function The function that should be called each time the asynchronous task executes
     * @param delay The interval, in ticks, that the asynchronous task should be executed
     * @return An ID representing the asynchronous task that was scheduled
     */
    public int scheduleAsyncRepeatingTask(PyFunction function, long delay, long interval) {
        Script script = ScriptManager.get().getScript(((PyBaseCode) function.__code__).co_filename);
        RepeatingTask task = new RepeatingTask(script, function);
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(PySpigot.get(), task, delay, interval);
        task.setTaskId(bukkitTask.getTaskId());
        repeatingTasks.add(task);
        return bukkitTask.getTaskId();
    }

    /**
     * Terminate a task with the given task ID.
     * @param taskId The ID of the task to terminate
     */
    public void stopTask(int taskId) {
        for (Iterator<RepeatingTask> iterator = repeatingTasks.iterator(); iterator.hasNext();) {
            RepeatingTask next = iterator.next();
            if (next.getTaskId() == taskId) {
                Bukkit.getScheduler().cancelTask(next.getTaskId());
                iterator.remove();
            }
        }
    }

    /**
     * Terminate all tasks belonging to a script.
     * @param script The script whose tasks should be terminated
     */
    public void stopTasks(Script script) {
        for (Iterator<RepeatingTask> iterator = repeatingTasks.iterator(); iterator.hasNext();) {
            RepeatingTask next = iterator.next();
            if (next.getScript().equals(script)) {
                Bukkit.getScheduler().cancelTask(next.getTaskId());
                iterator.remove();
            }
        }
    }

    /**
     * Get the singleton instance of this TaskManager.
     * @return The instance
     */
    public static TaskManager get() {
        if (manager == null)
            manager = new TaskManager();
        return manager;
    }
}
