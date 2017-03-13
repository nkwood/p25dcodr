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

import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.anhonesteffort.chnlzr.ProtocolErrorException;
import org.anhonesteffort.chnlzr.capnp.ProtoFactory;

import static org.anhonesteffort.chnlzr.capnp.Proto.BaseMessage;
import static org.anhonesteffort.chnlzr.capnp.Proto.ChannelRequest;
import static org.anhonesteffort.chnlzr.capnp.Proto.ChannelState;

public class ChannelRequestHandler extends ChannelInboundHandlerAdapter {

  private final ProtoFactory proto = new ProtoFactory();
  private final SettableFuture<ChannelRequestHandler> future;
  private final ChannelRequest.Reader request;

  private ChannelHandlerContext context;
  private ChannelState.Reader state;

  public ChannelRequestHandler(SettableFuture<ChannelRequestHandler> future, ChannelRequest.Reader request) {
    this.future  = future;
    this.request = request;
  }

  public ChannelHandlerContext getContext() {
    return context;
  }

  public ChannelState.Reader getState() {
    return state;
  }

  @Override
  public void handlerAdded(ChannelHandlerContext context) {
    this.context = context;
    context.writeAndFlush(proto.channelRequest(request));
  }

  @Override
  public void channelRead(ChannelHandlerContext context, Object msg)
      throws ProtocolErrorException, IllegalStateException
  {
    BaseMessage.Reader message = (BaseMessage.Reader) msg;

    switch (message.getType()) {
      case CHANNEL_STATE:
        state = message.getChannelState();
        future.set(this);
        break;

      case ERROR:
        ProtocolErrorException error = new ProtocolErrorException("chnlzr sent error", message.getError().getCode());
        if (future.setException(error)) {
          context.close();
        } else {
          throw error;
        }
        break;

      default:
        IllegalStateException ex = new IllegalStateException("chnlzr sent unexpected " + message.getType().name());
        if (future.setException(ex)) {
          context.close();
        } else {
          throw ex;
        }
        break;
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
    context.close();
    if (!future.setException(cause)) {
      super.exceptionCaught(context, cause);
    }
  }

}
