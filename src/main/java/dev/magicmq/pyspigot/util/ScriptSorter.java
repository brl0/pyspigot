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

package dev.magicmq.pyspigot.util;

import dev.magicmq.pyspigot.config.ScriptOptions;
import dev.magicmq.pyspigot.manager.script.Script;

import java.util.*;

/**
 * Utility class that places scripts into the proper loading order, taking into account dependencies. This class uses script dependencies as defined in {@link ScriptOptions}.
 * <p>
 * Under the hood, utilizes depth-first search algorithm to order scripts.
 */
public class ScriptSorter {

    private final HashMap<String, Script> scripts;
    private final Set<Script> visited;
    private final LinkedList<Script> loadOrder;

    /**
     *
     * @param scripts An unordered list of scripts
     */
    public ScriptSorter(List<Script> scripts) {
        this.scripts = new HashMap<>();
        scripts.forEach(script -> this.scripts.put(script.getName(), script));
        this.visited = new HashSet<>();
        this.loadOrder = new LinkedList<>();
    }

    /**
     * Get a {@link LinkedList}, in the order that the scripts should be loaded. Ordering is done with respect to script dependencies. The first script in this list should load first, and the last script in this list should load last.
     * @return An ordered list of scripts
     */
    public LinkedList<Script> getOptimalLoadOrder() {
        for (Script script : scripts.values()) {
            if (!visited.contains(script)) {
                dfs(script);
            }
        }

        return loadOrder;
    }

    private void dfs(Script script) {
        visited.add(script);

        for (String dependency : script.getOptions().getDependencies()) {
            Script scriptDependency = scripts.get(dependency);
            if (!visited.contains(scriptDependency)) {
                dfs(scriptDependency);
            }
        }

        loadOrder.offer(script);
    }
}
