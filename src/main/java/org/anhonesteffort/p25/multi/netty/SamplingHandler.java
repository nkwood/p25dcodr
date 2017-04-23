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
import org.anhonesteffort.chnlzr.ProtocolErrorException;
import org.anhonesteffort.dsp.StatefulSink;
import org.anhonesteffort.dsp.sample.Samples;
import org.anhonesteffort.dsp.util.ComplexNumber;

import java.nio.FloatBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.anhonesteffort.chnlzr.capnp.Proto.BaseMessage;
import static org.anhonesteffort.chnlzr.capnp.Proto.Capabilities;
import static org.anhonesteffort.chnlzr.capnp.Proto.ChannelState;

public class SamplingHandler extends ChannelInboundHandlerAdapter {

  private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();
  private final Capabilities.Reader     capabilities;
  private final StatefulSink<Samples>   sink;

  private ChannelState.Reader initState;

  public SamplingHandler(Capabilities.Reader capabilities, ChannelState.Reader initState, StatefulSink<Samples> sink) {
    this.capabilities = capabilities;
    this.initState    = initState;
    this.sink         = sink;
  }

  public Capabilities.Reader getCapabilities() {
    return capabilities;
  }

  public CompletionStage<Void> getCloseFuture() {
    return closeFuture;
  }

  public void close() {
    closeFuture.complete(null);
  }

  @Override
  public void handlerAdded(ChannelHandlerContext context) {
    closeFuture.whenComplete((ok, err) -> context.close());
    context.channel().closeFuture().addListener(close -> {
      if (close.isSuccess()) {
        closeFuture.complete(null);
      } else {
        closeFuture.completeExceptionally(close.cause());
      }
    });
  }

  @Override
  public void channelRead(ChannelHandlerContext context, Object msg) {
    if (initState != null) {
      sink.onStateChange(initState.getSampleRate(), initState.getCenterFrequency());
      initState = null;
    }

    BaseMessage.Reader message = (BaseMessage.Reader) msg;

    switch (message.getType()) {
      case CHANNEL_STATE:
        ChannelState.Reader state = message.getChannelState();
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
        closeFuture.completeExceptionally(
            new ProtocolErrorException("chnlzr sent error while streaming", message.getError().getCode())
        );
        break;

      default:
        closeFuture.completeExceptionally(
            new IllegalStateException("chnlzr sent unexpected while streaming " + message.getType().name())
        );
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
    context.close();
    closeFuture.completeExceptionally(cause);
  }

}
