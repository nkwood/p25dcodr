/*
 * Copyright (C) 2016 An Honest Effort LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.anhonesteffort.p25.multi;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.AllArgsConstructor;
import org.anhonesteffort.chnlzr.ChnlzrConfig;
import org.anhonesteffort.chnlzr.capnp.BaseMessageDecoder;
import org.anhonesteffort.chnlzr.capnp.BaseMessageEncoder;
import org.anhonesteffort.chnlzr.netty.IdleStateHeartbeatWriter;
import org.anhonesteffort.p25.multi.handler.ConnectionHandler;

import java.net.ConnectException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
public class ChnlzrConnections {

  private final ChnlzrConfig             config;
  private final Class<? extends Channel> channel;
  private final EventLoopGroup           workerGroup;

  public CompletionStage<ConnectionHandler> connect(ChnlzrHostId chnlzrHost) {
    CompletableFuture<ConnectionHandler> connecting = new CompletableFuture<>();
    ConnectionHandler                    connector  = new ConnectionHandler(connecting);
    Bootstrap                            bootstrap  = new Bootstrap();

    bootstrap.group(workerGroup)
             .channel(channel)
             .option(ChannelOption.SO_KEEPALIVE, true)
             .option(ChannelOption.TCP_NODELAY, true)
             .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.connectionTimeoutMs())
             .handler(new ChannelInitializer<Channel>() {
               @Override
               public void initChannel(Channel ch) {
                 ch.pipeline().addLast("idle state", new IdleStateHandler(0, 0, config.idleStateThresholdMs(), TimeUnit.MILLISECONDS));
                 ch.pipeline().addLast("heartbeat",  IdleStateHeartbeatWriter.INSTANCE);
                 ch.pipeline().addLast("encoder",    BaseMessageEncoder.INSTANCE);
                 ch.pipeline().addLast("decoder",    new BaseMessageDecoder());
                 ch.pipeline().addLast("connector",  connector);
               }
             });

    bootstrap.connect(chnlzrHost.getHostname(), chnlzrHost.getPort())
             .addListener(connect -> {
               if (!connect.isSuccess()) {
                 connecting.completeExceptionally(new ConnectException("failed to connect to chnlzr"));
               }
             });

    return connecting;
  }

}
