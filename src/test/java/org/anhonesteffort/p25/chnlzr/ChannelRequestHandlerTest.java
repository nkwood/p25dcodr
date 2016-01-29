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
import org.anhonesteffort.chnlzr.CapnpUtil;
import org.anhonesteffort.chnlzr.ProtocolErrorException;
import org.capnproto.MessageBuilder;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.ExecutionException;

import static org.anhonesteffort.chnlzr.Proto.BaseMessage;
import static org.anhonesteffort.chnlzr.Proto.ChannelRequest;
import static org.anhonesteffort.chnlzr.Proto.ChannelState;
import static org.anhonesteffort.chnlzr.Proto.Error;

public class ChannelRequestHandlerTest {

  private ChannelRequest.Reader request() {
    return CapnpUtil.channelRequest(10d, 20d, 30d, 0, 40d, 50d, 60l, 70l);
  }

  @Test
  public void testRequestSentOnHandlerAdded() {
    final SettableFuture<ChannelRequestHandler> FUTURE  = SettableFuture.create();
    final ChannelRequest.Reader                 REQUEST = request();
    final ChannelHandlerContext                 CONTEXT = Mockito.mock(ChannelHandlerContext.class);
    final ChannelRequestHandler                 HANDLER = new ChannelRequestHandler(FUTURE, REQUEST);

    Mockito.verify(CONTEXT, Mockito.never()).writeAndFlush(Mockito.any());
    HANDLER.handlerAdded(CONTEXT);
    Mockito.verify(CONTEXT, Mockito.times(1)).writeAndFlush(Mockito.any());
  }

  @Test
  public void testGetContext() {
    final SettableFuture<ChannelRequestHandler> FUTURE  = SettableFuture.create();
    final ChannelRequest.Reader                 REQUEST = request();
    final ChannelHandlerContext                 CONTEXT = Mockito.mock(ChannelHandlerContext.class);
    final ChannelRequestHandler                 HANDLER = new ChannelRequestHandler(FUTURE, REQUEST);

    assert HANDLER.getContext() == null;
    HANDLER.handlerAdded(CONTEXT);
    assert HANDLER.getContext().equals(CONTEXT);
  }

  @Test
  public void testSendReceiveChannelState() throws Exception {
    final SettableFuture<ChannelRequestHandler> FUTURE  = SettableFuture.create();
    final ChannelRequest.Reader                 REQUEST = request();
    final ChannelHandlerContext                 CONTEXT = Mockito.mock(ChannelHandlerContext.class);
    final ChannelRequestHandler                 HANDLER = new ChannelRequestHandler(FUTURE, REQUEST);

    final MessageBuilder STATE_MESSAGE = CapnpUtil.state(10l, 20d);

    assert HANDLER.getState() == null;

    HANDLER.handlerAdded(CONTEXT);
    HANDLER.channelRead(CONTEXT, STATE_MESSAGE.getRoot(BaseMessage.factory).asReader());

    assert HANDLER.getState() != null;

    assert FUTURE.isDone();
    assert FUTURE.get().equals(HANDLER);

    final ChannelState.Reader STATE_SENT = STATE_MESSAGE.getRoot(BaseMessage.factory).asReader().getChannelState();
    final ChannelState.Reader STATE_READ = HANDLER.getState();

    assert STATE_SENT.getSampleRate()      == STATE_READ.getSampleRate();
    assert STATE_SENT.getCenterFrequency() == STATE_READ.getCenterFrequency();
  }

  @Test
  public void testFutureErrorOnReadError() throws Exception {
    final SettableFuture<ChannelRequestHandler> FUTURE  = SettableFuture.create();
    final ChannelRequest.Reader                 REQUEST = request();
    final ChannelHandlerContext                 CONTEXT = Mockito.mock(ChannelHandlerContext.class);
    final ChannelRequestHandler                 HANDLER = new ChannelRequestHandler(FUTURE, REQUEST);

    final MessageBuilder MESSAGE_SENT = CapnpUtil.error(Error.ERROR_BANDWIDTH_UNAVAILABLE);

    assert !FUTURE.isDone();
    Mockito.verify(CONTEXT, Mockito.never()).close();

    HANDLER.handlerAdded(CONTEXT);
    HANDLER.channelRead(CONTEXT, MESSAGE_SENT.getRoot(BaseMessage.factory).asReader());

    assert FUTURE.isDone();
    Mockito.verify(CONTEXT, Mockito.times(1)).close();

    try {

      FUTURE.get();
      assert false;

    } catch (ExecutionException e) {
      assert (e.getCause() instanceof ProtocolErrorException);
    }
  }

}
