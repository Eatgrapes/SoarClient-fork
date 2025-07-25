/*
 * Copyright 2017 John Grosh (john.a.grosh@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soarclient.libraries.discordipc;

import com.google.gson.JsonObject;
import com.soarclient.libraries.discordipc.entities.Packet;
import com.soarclient.libraries.discordipc.entities.User;

/**
 * An implementable listener used to handle events caught by an {@link IPCClient}.
 * <p>
 * Can be attached to an IPCClient using {@link IPCClient#setListener(IPCListener)}.
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public interface IPCListener {
    /**
     * Fired whenever an {@link IPCClient} sends a {@link Packet} to Discord.
     *
     * @param client The IPCClient sending the Packet.
     * @param packet The Packet being sent.
     */
    void onPacketSent(IPCClient client, Packet packet);

    /**
     * Fired whenever an {@link IPCClient} receives a {@link Packet} to Discord.
     *
     * @param client The IPCClient receiving the Packet.
     * @param packet The Packet being received.
     */
    void onPacketReceived(IPCClient client, Packet packet);

    /**
     * Fired whenever a RichPresence activity informs us that
     * a user has clicked a "join" button.
     *
     * @param client The IPCClient receiving the event.
     * @param secret The secret of the event, determined by the implementation and specified by the user.
     */
    void onActivityJoin(IPCClient client, String secret);

    /**
     * Fired whenever a RichPresence activity informs us that
     * a user has clicked a "spectate" button.
     *
     * @param client The IPCClient receiving the event.
     * @param secret The secret of the event, determined by the implementation and specified by the user.
     */
    void onActivitySpectate(IPCClient client, String secret);

    /**
     * Fired whenever a RichPresence activity informs us that
     * a user has clicked an "ask to join" button.
     * <p>
     * As opposed to {@link #onActivityJoin(IPCClient, String)},
     * this also provides packaged {@link User} data.
     *
     * @param client The IPCClient receiving the event.
     * @param secret The secret of the event, determined by the implementation and specified by the user.
     * @param user   The user who clicked the event, containing data on the account.
     */
    void onActivityJoinRequest(IPCClient client, String secret, User user);

    /**
     * Fired whenever an {@link IPCClient} is ready and connected to Discord.
     *
     * @param client The now ready IPCClient.
     */
    void onReady(IPCClient client);

    /**
     * Fired whenever an {@link IPCClient} has closed.
     *
     * @param client The now closed IPCClient.
     * @param json   A {@link JsonObject} with close data.
     */
    void onClose(IPCClient client, JsonObject json);

    /**
     * Fired whenever an {@link IPCClient} has disconnected,
     * either due to bad data or an exception.
     *
     * @param client The now closed IPCClient.
     * @param t      A {@link Throwable} responsible for the disconnection.
     */
    void onDisconnect(IPCClient client, Throwable t);
}
