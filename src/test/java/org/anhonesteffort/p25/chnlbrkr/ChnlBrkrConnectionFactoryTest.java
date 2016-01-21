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

package org.anhonesteffort.p25.chnlbrkr;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import org.anhonesteffort.chnlzr.CapnpUtil;
import org.anhonesteffort.chnlzr.ChnlzrConfig;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.ConnectException;

public class ChnlBrkrConnectionFactoryTest {

  private static class IdleCallback implements FutureCallback<ChnlBrkrConnectionHandler> {

    private boolean isSuccess      = false;
    private boolean isConnectError = false;

    @Override
    public void onSuccess(ChnlBrkrConnectionHandler connection) {
      isSuccess = (connection != null);
    }

    @Override
    public void onFailure(Throwable throwable) {
      isSuccess      = false;
      isConnectError = (throwable instanceof ConnectException);
    }

    public boolean isSuccess() {
      return isSuccess;
    }

    public boolean isConnectError() {
      return isConnectError;
    }
  }

  @Test
  public void testFutureCanceledOnConnectionFailure() throws InterruptedException {
    final EventLoopGroup            GROUP   = new NioEventLoopGroup();
    final ChnlzrConfig              CONFIG  = Mockito.mock(ChnlzrConfig.class);
    final ChnlBrkrConnectionFactory FACTORY = new ChnlBrkrConnectionFactory(CONFIG, EmbeddedChannel.class, GROUP);

    Mockito.when(CONFIG.connectionTimeoutMs()).thenReturn(250);
    Mockito.when(CONFIG.idleStateThresholdMs()).thenReturn(1000l);
    Mockito.when(CONFIG.bufferHighWaterMark()).thenReturn(32768);
    Mockito.when(CONFIG.bufferLowWaterMark()).thenReturn(8192);

    final ListenableFuture<ChnlBrkrConnectionHandler> FUTURE   = FACTORY.create(CapnpUtil.hostId("nope", 1337));
    final IdleCallback                                CALLBACK = new IdleCallback();

    Futures.addCallback(FUTURE, CALLBACK);
    Thread.sleep(CONFIG.connectionTimeoutMs());

    assert !CALLBACK.isSuccess() && CALLBACK.isConnectError();
    GROUP.shutdownGracefully();
  }

}
