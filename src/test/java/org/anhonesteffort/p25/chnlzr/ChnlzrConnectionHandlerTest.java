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
import org.capnproto.MessageBuilder;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.ExecutionException;

import static org.anhonesteffort.chnlzr.Proto.BaseMessage;

public class ChnlzrConnectionHandlerTest {

  @Test
  public void testGetContext() throws Exception {
    final SettableFuture<ChnlzrConnectionHandler> FUTURE  = SettableFuture.create();
    final ChannelHandlerContext                   CONTEXT = Mockito.mock(ChannelHandlerContext.class);
    final ChnlzrConnectionHandler                 HANDLER = new ChnlzrConnectionHandler(FUTURE);

    HANDLER.channelActive(CONTEXT);

    assert !FUTURE.isDone();
    assert HANDLER.getContext().equals(CONTEXT);
  }

  @Test
  public void testFutureCompletesOnCapabilities() throws Exception {
    final SettableFuture<ChnlzrConnectionHandler> FUTURE  = SettableFuture.create();
    final ChannelHandlerContext                   CONTEXT = Mockito.mock(ChannelHandlerContext.class);
    final ChnlzrConnectionHandler                 HANDLER = new ChnlzrConnectionHandler(FUTURE);

    final MessageBuilder CAPS_MESSAGE = CapnpUtil.capabilities(10d, 20d, 0, 30d, 40d, 50l);

    assert HANDLER.getCapabilities() == null;
    assert !FUTURE.isDone();

    HANDLER.channelActive(CONTEXT);
    HANDLER.channelRead(CONTEXT, CAPS_MESSAGE.getRoot(BaseMessage.factory).asReader());

    assert FUTURE.isDone();
    assert FUTURE.get().equals(HANDLER);
    assert HANDLER.getContext().equals(CONTEXT);
  }

  @Test
  public void testFutureErrorsOnChannelInactive() throws InterruptedException {
    final SettableFuture<ChnlzrConnectionHandler> FUTURE  = SettableFuture.create();
    final ChannelHandlerContext                   CONTEXT = Mockito.mock(ChannelHandlerContext.class);
    final ChnlzrConnectionHandler                 HANDLER = new ChnlzrConnectionHandler(FUTURE);

    HANDLER.channelInactive(CONTEXT);

    try {

      assert FUTURE.isDone();
      FUTURE.get();
      assert false;

    } catch (ExecutionException e) {
      assert (e.getCause() instanceof ConnectException);
    }
  }

  @Test
  public void testFutureErrorsOnChannelException() throws Exception {
    final SettableFuture<ChnlzrConnectionHandler> FUTURE  = SettableFuture.create();
    final ChannelHandlerContext                   CONTEXT = Mockito.mock(ChannelHandlerContext.class);
    final IOException                             ERROR   = new IOException("qqq");
    final ChnlzrConnectionHandler                 HANDLER = new ChnlzrConnectionHandler(FUTURE);

    HANDLER.exceptionCaught(CONTEXT, ERROR);

    try {

      assert FUTURE.isDone();
      FUTURE.get();
      assert false;

    } catch (ExecutionException e) {
      assert (e.getCause().equals(ERROR));
    }
  }

  @Test
  public void testContextClosedOnChannelException() throws Exception {
    final SettableFuture<ChnlzrConnectionHandler> FUTURE  = SettableFuture.create();
    final ChannelHandlerContext                   CONTEXT = Mockito.mock(ChannelHandlerContext.class);
    final IOException                             ERROR   = new IOException("qqq");
    final ChnlzrConnectionHandler                 HANDLER = new ChnlzrConnectionHandler(FUTURE);

    Mockito.verify(CONTEXT, Mockito.never()).close();
    HANDLER.exceptionCaught(CONTEXT, ERROR);
    Mockito.verify(CONTEXT, Mockito.times(1)).close();
  }

}
