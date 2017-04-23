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

package org.anhonesteffort.p25.chnlzr;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.timeout.IdleStateHandler;
import org.anhonesteffort.chnlzr.ChnlzrConfig;
import org.anhonesteffort.chnlzr.capnp.BaseMessageDecoder;
import org.anhonesteffort.chnlzr.capnp.BaseMessageEncoder;
import org.anhonesteffort.chnlzr.netty.IdleStateHeartbeatWriter;

import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

public class ChnlzrConnectionFactory {

  private final ChnlzrConfig             config;
  private final Class<? extends Channel> channel;
  private final EventLoopGroup           workerGroup;

  public ChnlzrConnectionFactory(ChnlzrConfig             config,
                                 Class<? extends Channel> channel,
                                 EventLoopGroup           workerGroup)
  {
    this.config      = config;
    this.channel     = channel;
    this.workerGroup = workerGroup;
  }

  public ListenableFuture<ChnlzrConnectionHandler> create(HostId chnlzrHost) {
    SettableFuture<ChnlzrConnectionHandler> future     = SettableFuture.create();
    ChnlzrConnectionHandler                 connection = new ChnlzrConnectionHandler(future);
    Bootstrap                               bootstrap  = new Bootstrap();

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
                 ch.pipeline().addLast("connector",  connection);
               }
             });

    bootstrap.connect(chnlzrHost.getHostname(), chnlzrHost.getPort())
             .addListener(connect -> {
               if (!connect.isSuccess())
                 future.setException(new ConnectException("failed to connect to chnlzr"));
             });

    return future;
  }

}
