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

package dev.magicmq.pyspigot.manager.redis;

import io.lettuce.core.pubsub.RedisPubSubListener;
import org.graalvm.polyglot.Value;

/**
 * A wrapper class that wraps the RedisPubSubListener from lettuce for use by scripts.
 * @see io.lettuce.core.pubsub.RedisPubSubListener
 */
public class ScriptPubSubListener implements RedisPubSubListener<String, String> {

    private final Value function;
    private final String channel;

    /**
     *
     * @param function The function that should be called when a message is received on the given channel
     * @param channel The channel to listen on
     */
    public ScriptPubSubListener(Value function, String channel) {
        this.function = function;
        this.channel = channel;
    }

    /**
     * Called internally when a message is received on the given channel.
     * @param channel The channel on which the message was received
     * @param message The message that was received
     */
    @Override
    public void message(String channel, String message) {
        if (channel.equals(this.channel)) {
            function.executeVoid(message);
        }
    }

    /**
     * Implemented from {@link RedisPubSubListener}, but unused.
     */
    @Override
    public void message(String s, String k1, String s2) {}

    /**
     * Implemented from {@link RedisPubSubListener}, but unused.
     */
    @Override
    public void subscribed(String s, long l) {}

    /**
     * Implemented from {@link RedisPubSubListener}, but unused.
     */
    @Override
    public void psubscribed(String s, long l) {}

    /**
     * Implemented from {@link RedisPubSubListener}, but unused.
     */
    @Override
    public void unsubscribed(String s, long l) {}

    /**
     * Implemented from {@link RedisPubSubListener}, but unused.
     */
    @Override
    public void punsubscribed(String s, long l) {}

    /**
     * Implemented from {@link RedisPubSubListener}, but unused.
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Prints a representation of this ScriptPubSubListener in string format, including the channel being listened to.
     * @return A string representation of the ScriptPubSubListener
     */
    @Override
    public String toString() {
        return String.format("ScriptPubSubListener[Channel: %s]", channel);
    }
}
