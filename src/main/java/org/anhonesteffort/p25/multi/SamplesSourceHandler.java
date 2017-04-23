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

package org.anhonesteffort.p25.multi;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.anhonesteffort.chnlzr.ProtocolErrorException;
import org.anhonesteffort.dsp.StatefulSink;
import org.anhonesteffort.dsp.sample.Samples;
import org.anhonesteffort.dsp.util.ComplexNumber;

import javax.annotation.Nonnull;
import java.nio.FloatBuffer;

import static org.anhonesteffort.chnlzr.capnp.Proto.BaseMessage;
import static org.anhonesteffort.chnlzr.capnp.Proto.Capabilities;
import static org.anhonesteffort.chnlzr.capnp.Proto.ChannelState;

public class SamplesSourceHandler extends ChannelInboundHandlerAdapter {

  private final SettableFuture<Void>  closePromise = SettableFuture.create();
  private final Capabilities.Reader   capabilities;
  private final StatefulSink<Samples> sink;

  private ChannelState.Reader state;
  private boolean initState = true;

  public SamplesSourceHandler(
      ChannelHandlerContext context,
      Capabilities.Reader   capabilities,
      ChannelState.Reader   state,
      StatefulSink<Samples> sink
  ) {
    this.capabilities = capabilities;
    this.state        = state;
    this.sink         = sink;

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

  public void close() {
    closePromise.set(null);
  }

  @Override
  public void channelRead(ChannelHandlerContext context, Object msg)
      throws ProtocolErrorException, IllegalStateException
  {
    if (initState) {
      sink.onStateChange(state.getSampleRate(), state.getCenterFrequency());
      initState = false;
    }

    BaseMessage.Reader message = (BaseMessage.Reader) msg;

    switch (message.getType()) {
      case CHANNEL_STATE:
        state = message.getChannelState();
        sink.onStateChange(state.getSampleRate(), state.getCenterFrequency());
        break;

      case SAMPLES:
        FloatBuffer     floats  = message.getSamples().getSamples().asByteBuffer().asFloatBuffer();
        ComplexNumber[] samples = new ComplexNumber[floats.limit() / 2];

        for (int i = 0; i < samples.length; i++) {
          samples[i] = new ComplexNumber(floats.get(i * 2), floats.get((i * 2) + 1));
        }

        sink.consume(new Samples(samples));
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

}
