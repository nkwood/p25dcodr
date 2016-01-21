/*
 * Copyright (C) 2015 An Honest Effort LLC, coping.
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

package org.anhonesteffort.p25.chnlbrkr;

import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.anhonesteffort.chnlzr.CapnpUtil;
import org.anhonesteffort.chnlzr.Proto;
import org.anhonesteffort.chnlzr.ProtocolErrorException;

import static org.anhonesteffort.chnlzr.Proto.BaseMessage;
import static org.anhonesteffort.chnlzr.Proto.ChannelRequest;
import static org.anhonesteffort.chnlzr.Proto.Capabilities;
import static org.anhonesteffort.chnlzr.Proto.ChannelState;

public class ChannelRequestHandler extends ChannelHandlerAdapter {

  private final SettableFuture<ChannelRequestHandler> future;
  private final ChannelRequest.Reader request;

  private ChannelHandlerContext context;
  private ChannelState.Reader   state;
  private Capabilities.Reader   capabilities;

  public ChannelRequestHandler(SettableFuture<ChannelRequestHandler> future,
                               ChannelRequest.Reader                 request)
  {
    this.future  = future;
    this.request = request;
  }

  public ChannelHandlerContext getContext() {
    return context;
  }

  public Capabilities.Reader getCapabilities() {
    return capabilities;
  }

  public ChannelState.Reader getState() {
    return state;
  }

  @Override
  public void handlerAdded(ChannelHandlerContext context) {
    this.context = context;
    context.writeAndFlush(CapnpUtil.channelRequest(request));
  }

  @Override
  public void channelRead(ChannelHandlerContext context, Object msg) throws ProtocolErrorException {
    BaseMessage.Reader message = (BaseMessage.Reader) msg;

    switch (message.getType()) {
      case CAPABILITIES:
        capabilities = message.getCapabilities();
        break;

      case CHANNEL_STATE:
        if (capabilities == null) {
          throw new ProtocolErrorException(
              "channel state received before capabilities", Proto.Error.ERROR_UNKNOWN
          );
        } else {
          state = message.getChannelState();
          future.set(this);
        }
        break;

      case ERROR:
        ProtocolErrorException error = new ProtocolErrorException(
            "chnlbrkr sent us an error", message.getError().getCode()
        );

        if (future.setException(error)) {
          context.close();
        } else {
          throw error;
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
