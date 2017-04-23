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

package org.anhonesteffort.p25.chnlzr.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import org.anhonesteffort.chnlzr.ProtocolErrorException;
import org.anhonesteffort.chnlzr.capnp.ProtoFactory;

import java.net.ConnectException;
import java.util.concurrent.CompletableFuture;

import static org.anhonesteffort.chnlzr.capnp.Proto.BaseMessage;
import static org.anhonesteffort.chnlzr.capnp.Proto.ChannelRequest;
import static org.anhonesteffort.chnlzr.capnp.Proto.ChannelState;
import static org.anhonesteffort.chnlzr.capnp.Proto.Capabilities;

public class RequestHandler extends ChannelInboundHandlerAdapter {

  private final ProtoFactory proto;
  private final CompletableFuture<RequestHandler> future;
  private final ChannelRequest.Reader request;

  @Getter private final Capabilities.Reader capabilities;
  @Getter private ChannelHandlerContext context;
  @Getter private ChannelState.Reader state;

  public RequestHandler(
      ProtoFactory proto, CompletableFuture<RequestHandler> future,
      Capabilities.Reader capabilities, ChannelRequest.Reader request
  ) {
    this.proto        = proto;
    this.future       = future;
    this.capabilities = capabilities;
    this.request      = request;
  }

  @Override
  public void handlerAdded(ChannelHandlerContext context) {
    this.context = context;

    future.whenComplete((ok, err) -> context.close());
    context.channel().closeFuture().addListener(close -> {
      if (close.isSuccess()) {
        future.completeExceptionally(new ConnectException("chnlzr closed connection unexpectedly"));
      } else {
        future.completeExceptionally(close.cause());
      }
    });

    context.writeAndFlush(proto.channelRequest(request));
  }

  @Override
  public void channelRead(ChannelHandlerContext context, Object msg) {
    BaseMessage.Reader message = (BaseMessage.Reader) msg;

    switch (message.getType()) {
      case CHANNEL_STATE:
        state = message.getChannelState();
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
