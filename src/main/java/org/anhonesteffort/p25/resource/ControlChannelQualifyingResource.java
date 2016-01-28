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

package org.anhonesteffort.p25.resource;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.anhonesteffort.chnlzr.CapnpUtil;
import org.anhonesteffort.chnlzr.ProtocolErrorException;
import org.anhonesteffort.p25.P25Channel;
import org.anhonesteffort.p25.P25ChannelSpec;
import org.anhonesteffort.p25.P25Config;
import org.anhonesteffort.p25.P25DcodrConfig;
import org.anhonesteffort.p25.chnlzr.SamplesSourceHandler;
import org.anhonesteffort.p25.model.ControlChannelQualities;
import org.anhonesteffort.p25.model.QualifyChannelId;
import org.anhonesteffort.p25.model.QualifyRequest;
import org.anhonesteffort.p25.protocol.ControlChannelQualifier;
import org.anhonesteffort.p25.chnlzr.ChnlzrController;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.anhonesteffort.chnlzr.Proto.ChannelRequest;

@Path("/qualify")
@Produces(MediaType.APPLICATION_JSON)
public class ControlChannelQualifyingResource {

  private static final Logger log = LoggerFactory.getLogger(ControlChannelQualifyingResource.class);

  private final P25DcodrConfig           config;
  private final ChnlzrController         chnlzr;
  private final ListeningExecutorService dspPool;

  public ControlChannelQualifyingResource(P25DcodrConfig           config,
                                          ChnlzrController         chnlzr,
                                          ListeningExecutorService dspPool)
  {
    this.config  = config;
    this.chnlzr  = chnlzr;
    this.dspPool = dspPool;
  }

  private ChannelRequest.Reader transform(QualifyRequest request) {
    return CapnpUtil.channelRequest(
        request.getLatitude(), request.getLongitude(), 0, request.getPolarization(),
        request.getFrequency(), P25Config.CHANNEL_WIDTH, P25Config.SAMPLE_RATE,
        config.getP25Config().getMaxRateDiff()
    );
  }

  @POST
  @ManagedAsync
  public void qualify(@NotNull @Valid QualifyRequest request, @Suspended AsyncResponse response) {
    ChannelRequest.Reader                  channelRequest = transform(request);
    ListenableFuture<SamplesSourceHandler> sourceFuture   = chnlzr.createSourceFor(channelRequest);

    Futures.addCallback(sourceFuture, new SamplesSourceCallback(channelRequest, response));

    response.setTimeout(config.getChannelRequestTimeoutMs(), TimeUnit.MILLISECONDS);
    response.setTimeoutHandler(asyncResponse -> sourceFuture.cancel(true));
  }

  private class SamplesSourceCallback extends AbstractSamplesSourceCallback {
    private final ChannelRequest.Reader request;

    public SamplesSourceCallback(ChannelRequest.Reader request, AsyncResponse response) {
      super(response, new QualifyChannelId(request.getCenterFrequency()));
      this.request = request;
    }

    @Override
    protected Logger log() {
      return log;
    }

    @Override
    public void onSuccess(SamplesSourceHandler samplesSource) {
      P25ChannelSpec          channelSpec = new P25ChannelSpec(request.getCenterFrequency());
      P25Channel              channel     = new P25Channel(config.getP25Config(), channelSpec, config.getSamplesQueueSize());
      ControlChannelQualifier qualifier   = new ControlChannelQualifier();

      ListenableFuture<Void>   channelFuture   = dspPool.submit(channel);
      ChannelQualifiedCallback channelCallback = new ChannelQualifiedCallback(
          qualifier, response, samplesSource, channelFuture
      );

      channel.addSink(qualifier);
      samplesSource.setSink(channel);

      Futures.addCallback(channelFuture, channelCallback);
      samplesSource.getCloseFuture().addListener(channelCallback);

      response.setTimeout(config.getChannelQualifyTimeMs(), TimeUnit.MILLISECONDS);
      response.setTimeoutHandler(asyncResponse -> channelFuture.cancel(true));
    }
  }

  private class ChannelQualifiedCallback
      implements FutureCallback<Void>, ChannelFutureListener
  {
    private final ControlChannelQualifier qualifier;
    private final AsyncResponse           response;
    private final SamplesSourceHandler    samplesSource;
    private final ListenableFuture        channelFuture;

    private AtomicBoolean responseComplete = new AtomicBoolean(false);

    public ChannelQualifiedCallback(ControlChannelQualifier qualifier,
                                    AsyncResponse           response,
                                    SamplesSourceHandler    samplesSource,
                                    ListenableFuture        channelFuture)
    {
      this.qualifier     = qualifier;
      this.response      = response;
      this.samplesSource = samplesSource;
      this.channelFuture = channelFuture;
    }

    private void onQualifyComplete() {
      Optional<ControlChannelQualities> qualities = qualifier.getQualities();
      if (qualities.isPresent()) {
        log.info("qualified new control channel, qualities => " + qualities.get().toString());
        response.resume(qualities.get());
      } else {
        response.resume(Response.status(204).build());
      }
    }

    @Override
    public void onSuccess(Void nothing) {
      if (responseComplete.compareAndSet(false, true)) {
        samplesSource.close();
        onQualifyComplete();
      }
    }

    @Override
    public void onFailure(@Nonnull Throwable throwable) {
      if (responseComplete.compareAndSet(false, true)) {
        samplesSource.close();

        if (throwable instanceof CancellationException) {
          onQualifyComplete();
        } else {
          log.error("unable to qualify channel, unexpected dsp error", throwable);
          response.resume(Response.status(500).build());
        }
      }
    }

    @Override
    public void operationComplete(ChannelFuture sourceClosedFuture) {
      if (responseComplete.compareAndSet(false, true)) {
        channelFuture.cancel(true);

        if (sourceClosedFuture.isSuccess()) {
          log.warn("unable to qualify channel, chnlzr connection closed unexpectedly");
          response.resume(Response.status(503).build());
        } else if (sourceClosedFuture.cause() instanceof ProtocolErrorException) {
          ProtocolErrorException error = (ProtocolErrorException) sourceClosedFuture.cause();
          log.warn("unable to qualify channel, chnlzr closed connection with error: " + error.getCode());
          response.resume(Response.status(503).build());
        } else {
          log.error("unable to qualify channel, unexpected netty error", sourceClosedFuture.cause());
          response.resume(Response.status(500).build());
        }
      }
    }
  }

}
