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

package org.anhonesteffort.p25.multi.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import org.anhonesteffort.chnlzr.ProtocolErrorException;

import java.net.ConnectException;
import java.util.concurrent.CompletableFuture;

import static org.anhonesteffort.chnlzr.capnp.Proto.BaseMessage;
import static org.anhonesteffort.chnlzr.capnp.Proto.Capabilities;

public class ConnectionHandler extends ChannelInboundHandlerAdapter {

  private final CompletableFuture<ConnectionHandler> future;

  @Getter private ChannelHandlerContext context;
  @Getter private Capabilities.Reader   capabilities;

  public ConnectionHandler(CompletableFuture<ConnectionHandler> future) {
    this.future = future;
  }

  @Override
  public void channelActive(ChannelHandlerContext context) {
    this.context = context;

    future.whenComplete((ok, err) -> context.close());
    context.channel().closeFuture().addListener(close -> {
      if (close.isSuccess()) {
        future.completeExceptionally(new ConnectException("chnlzr closed connection unexpectedly"));
      } else {
        future.completeExceptionally(close.cause());
      }
    });
  }

  @Override
  public void channelRead(ChannelHandlerContext context, Object msg) {
    BaseMessage.Reader message = (BaseMessage.Reader) msg;

    switch (message.getType()) {
      case CAPABILITIES:
        capabilities = message.getCapabilities();
        future.complete(this);
        break;

      case ERROR:
        future.completeExceptionally(
            new ProtocolErrorException("chnlzr sent error", message.getError().getCode())
        );
        break;

      default:
        future.completeExceptionally(
            new IllegalStateException("chnlzr sent unexpected " + message.getType().name())
        );
        break;
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
    context.close();
    future.completeExceptionally(cause);
  }

}
