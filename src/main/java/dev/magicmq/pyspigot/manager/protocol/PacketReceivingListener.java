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

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketEvent;
import dev.magicmq.pyspigot.manager.script.Script;
import org.graalvm.polyglot.Value;

/**
 * A listener that listens for packets received by the server from the client.
 * @see ScriptPacketListener
 */
public class PacketReceivingListener extends ScriptPacketListener {

    /**
     *
     * @param script The script associated with this packet listener
     * @param function The function to be called when the packet is received
     * @param packetType The packet type to listen for
     * @param listenerPriority The {@link com.comphenix.protocol.events.ListenerPriority} of this listener
     * @param listenerType The {@link ListenerType} of this listener
     */
    public PacketReceivingListener(Script script, Value function, PacketType packetType, ListenerPriority listenerPriority, ListenerType listenerType) {
        super(script, function, packetType, listenerPriority, listenerType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPacketReceiving(PacketEvent event) {
        super.callToScript(event);
    }
}
