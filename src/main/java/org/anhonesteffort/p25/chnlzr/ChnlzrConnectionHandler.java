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
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.anhonesteffort.chnlzr.ProtocolErrorException;

import java.net.ConnectException;

import static org.anhonesteffort.chnlzr.capnp.Proto.BaseMessage;
import static org.anhonesteffort.chnlzr.capnp.Proto.Capabilities;

public class ChnlzrConnectionHandler extends ChannelHandlerAdapter {

  private final SettableFuture<ChnlzrConnectionHandler> future;
  private ChannelHandlerContext context;
  private Capabilities.Reader capabilities;

  public ChnlzrConnectionHandler(SettableFuture<ChnlzrConnectionHandler> future) {
    this.future = future;
  }

  public ChannelHandlerContext getContext() {
    return context;
  }

  public Capabilities.Reader getCapabilities() {
    return capabilities;
  }

  @Override
  public void channelActive(ChannelHandlerContext context) {
    this.context = context;
  }

  @Override
  public void channelRead(ChannelHandlerContext context, Object msg)
      throws ProtocolErrorException, IllegalStateException
  {
    BaseMessage.Reader message = (BaseMessage.Reader) msg;

    switch (message.getType()) {
      case CAPABILITIES:
        capabilities = message.getCapabilities();
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
  public void channelInactive(ChannelHandlerContext context) {
    future.setException(new ConnectException("failed to connect to chnlzr"));
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
    context.close();
    if (!future.setException(cause)) {
      super.exceptionCaught(context, cause);
    }
  }

}
