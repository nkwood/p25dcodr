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

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.anhonesteffort.chnlzr.ProtocolErrorException;
import org.anhonesteffort.dsp.sample.DynamicSink;
import org.anhonesteffort.dsp.sample.Samples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Optional;

import static org.anhonesteffort.chnlzr.Proto.BaseMessage;
import static org.anhonesteffort.chnlzr.Proto.Capabilities;
import static org.anhonesteffort.chnlzr.Proto.ChannelState;

public class SamplesSourceHandler extends ChannelHandlerAdapter {

  private static final Logger log = LoggerFactory.getLogger(SamplesSourceHandler.class);

  private final ChannelHandlerContext context;
  private final ChannelPromise        closePromise;

  private Capabilities.Reader            capabilities;
  private ChannelState.Reader            state;
  private Optional<DynamicSink<Samples>> sink = Optional.empty();

  public SamplesSourceHandler(ChannelHandlerContext context,
                              Capabilities.Reader   capabilities,
                              ChannelState.Reader   state)
  {
    this.context      = context;
    this.capabilities = capabilities;
    this.state        = state;
    closePromise      = context.newPromise();

    context.channel().closeFuture().addListener(close -> {
      if (close.isSuccess() && !closePromise.isDone())
        closePromise.setSuccess();
      else if (!closePromise.isDone())
        closePromise.setFailure(close.cause());
    });

    closePromise.addListener(close -> context.close());
  }

  public Capabilities.Reader getCapabilities() {
    return capabilities;
  }

  public ChannelFuture getCloseFuture() {
    return closePromise;
  }

  public void setSink(DynamicSink<Samples> sink) {
    sink.onSourceStateChange(state.getSampleRate(), state.getCenterFrequency());
    this.sink = Optional.of(sink);
  }

  @Override
  public void channelRead(ChannelHandlerContext context, Object msg) throws Exception {
    BaseMessage.Reader message = (BaseMessage.Reader) msg;

    switch (message.getType()) {
      case CHANNEL_STATE:
        state = message.getChannelState();
        if (sink.isPresent()) {
          sink.get().onSourceStateChange(state.getSampleRate(), state.getCenterFrequency());
        }
        break;

      case SAMPLES:
        if (!sink.isPresent()) {
          break;
        }

        ByteBuffer samples = message.getSamples().getSamples().asByteBuffer();
        sink.get().consume(new Samples(samples.asFloatBuffer()));
        break;

      case ERROR:
        ProtocolErrorException error = new ProtocolErrorException(
            "received ERROR while streaming samples", message.getError().getCode()
        );
        if (!closePromise.isDone()) {
          closePromise.setFailure(error);
        } else {
          throw error;
        }
        break;
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
    log.error("caught exception while streaming channel grant", cause);
    if (!closePromise.isDone()) {closePromise.setFailure(cause);}
  }

  public void close() {
    context.close();
  }

}
