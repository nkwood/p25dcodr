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

package org.anhonesteffort.p25.chnlzr;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.anhonesteffort.chnlzr.ProtocolErrorException;
import org.anhonesteffort.dsp.sample.DynamicSink;
import org.anhonesteffort.dsp.sample.Samples;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import static org.anhonesteffort.chnlzr.Proto.BaseMessage;
import static org.anhonesteffort.chnlzr.Proto.Capabilities;
import static org.anhonesteffort.chnlzr.Proto.ChannelState;

public class SamplesSourceHandler extends ChannelHandlerAdapter {

  private final SettableFuture<Void> closePromise;
  private final Capabilities.Reader  capabilities;
  private       ChannelState.Reader  state;

  private AtomicReference<DynamicSink<Samples>> sink = new AtomicReference<>(null);

  public SamplesSourceHandler(ChannelHandlerContext context,
                              Capabilities.Reader   capabilities,
                              ChannelState.Reader   state)
  {
    this.capabilities = capabilities;
    this.state        = state;
    closePromise      = SettableFuture.create();

    context.channel().closeFuture().addListener(close -> {
      if (close.isSuccess()) {
        closePromise.set(null);
      } else {
        closePromise.setException(close.cause());
      }
    });

    Futures.addCallback(closePromise, new FutureCallback<Void>() {
      @Override
      public void onSuccess(Void aVoid) {
        context.close();
      }

      @Override
      public void onFailure(@Nonnull Throwable error) {
        context.close();
      }
    });
  }

  public Capabilities.Reader getCapabilities() {
    return capabilities;
  }

  public ListenableFuture<Void> getCloseFuture() {
    return closePromise;
  }

  public void setSink(DynamicSink<Samples> sink) {
    sink.onSourceStateChange(state.getSampleRate(), state.getCenterFrequency());
    this.sink.set(sink);
  }

  public void close() {
    sink.set(null);
    closePromise.set(null);
  }

  @Override
  public void channelRead(ChannelHandlerContext context, Object msg)
      throws ProtocolErrorException, IllegalStateException
  {
    BaseMessage.Reader   message = (BaseMessage.Reader) msg;
    DynamicSink<Samples> sink    = this.sink.get();

    switch (message.getType()) {
      case CHANNEL_STATE:
        state = message.getChannelState();
        if (sink != null) {
          sink.onSourceStateChange(state.getSampleRate(), state.getCenterFrequency());
        }
        break;

      case SAMPLES:
        if (sink != null) {
          ByteBuffer samples = message.getSamples().getSamples().asByteBuffer();
          sink.consume(new Samples(samples.asFloatBuffer()));
        }
        break;

      case ERROR:
        ProtocolErrorException error = new ProtocolErrorException("chnlzr sent error while streaming", message.getError().getCode());
        if (!closePromise.setException(error)) {
          throw error;
        }
        break;

      default:
        IllegalStateException ex = new IllegalStateException("chnlzr sent unexpected while streaming " + message.getType().name());
        if (!closePromise.setException(ex)) {
          throw ex;
        }
        break;
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
    context.close();
    closePromise.setException(cause);
  }

  @Override
  public void channelInactive(ChannelHandlerContext context) {
    sink.set(null);
  }

}
