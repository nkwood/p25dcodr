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

package org.anhonesteffort.p25.resource;

import com.codahale.metrics.annotation.Timed;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import io.radiowitness.kinesis.producer.KinesisRecordProducer;
import org.anhonesteffort.chnlzr.capnp.ProtoFactory;
import org.anhonesteffort.p25.P25Channel;
import org.anhonesteffort.p25.P25ChannelSpec;
import org.anhonesteffort.p25.P25Config;
import org.anhonesteffort.p25.P25DcodrConfig;
import org.anhonesteffort.p25.chnlzr.ChnlzrController;
import org.anhonesteffort.p25.chnlzr.SamplesSourceHandler;
import org.anhonesteffort.p25.kinesis.KinesisRecordProducerFactory;
import org.anhonesteffort.p25.model.ChannelId;
import org.anhonesteffort.p25.model.FollowList;
import org.anhonesteffort.p25.model.FollowRequest;
import org.anhonesteffort.p25.model.UnfollowRequest;
import org.anhonesteffort.p25.monitor.ChannelMonitor;
import org.anhonesteffort.p25.protocol.ControlChannelFollower;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.anhonesteffort.chnlzr.capnp.Proto.ChannelRequest;

@Path("/channels/control")
@Produces(MediaType.APPLICATION_JSON)
public class ControlChannelFollowingResource {

  private static final Logger log = LoggerFactory.getLogger(ControlChannelFollowingResource.class);

  private final ProtoFactory     proto           = new ProtoFactory();
  private final Queue<ChannelId> pendingRequests = new ConcurrentLinkedQueue<>();
  private final Object           txnLock         = new Object();

  private final P25DcodrConfig               config;
  private final ChnlzrController             chnlzr;
  private final ChannelMonitor               channelMonitor;
  private final KinesisRecordProducerFactory senderFactory;
  private final WebTarget                    trafficTarget;
  private final ListeningExecutorService     dspPool;

  public ControlChannelFollowingResource(P25DcodrConfig               config,
                                         ChnlzrController             chnlzr,
                                         ChannelMonitor               channelMonitor,
                                         KinesisRecordProducerFactory senderFactory,
                                         WebTarget                    trafficTarget,
                                         ListeningExecutorService     dspPool)
  {
    this.config         = config;
    this.chnlzr         = chnlzr;
    this.channelMonitor = channelMonitor;
    this.senderFactory  = senderFactory;
    this.trafficTarget  = trafficTarget;
    this.dspPool        = dspPool;
  }

  private ChannelRequest.Reader transform(FollowRequest request) {
    return proto.channelRequest(
        request.getFrequency(), P25Config.CHANNEL_WIDTH, P25Config.SAMPLE_RATE,
        config.getP25Config().getMaxRateDiff()
    );
  }

  @GET
  @Timed
  public FollowList getList() {
    List<FollowRequest> followed =
        channelMonitor.getMonitored()
                      .stream()
                      .filter(reference -> reference instanceof FollowRequest)
                      .map(reference -> (FollowRequest) reference)
                      .collect(Collectors.toList());

    return new FollowList(followed);
  }

  @POST
  @Timed
  @ManagedAsync
  public void follow(@NotNull @Valid FollowRequest request, @Suspended AsyncResponse response) {
    synchronized (txnLock) {
      if (pendingRequests.contains(request.getChannelId()) ||
          channelMonitor.contains(request.getChannelId()))
      {
        response.resume(Response.status(409).build());
        return;
      } else {
        pendingRequests.add(request.getChannelId());
        log.info(request.getChannelId() + " requesting channel");
      }
    }

    ChannelRequest.Reader                  channelRequest = transform(request);
    ListenableFuture<SamplesSourceHandler> sourceFuture   = chnlzr.createSourceFor(channelRequest);

    Futures.addCallback(sourceFuture, new SamplesSourceCallback(request, response));

    response.setTimeout(config.getChannelRequestTimeoutMs(), TimeUnit.MILLISECONDS);
    response.setTimeoutHandler(asyncResponse -> sourceFuture.cancel(true));
  }

  @DELETE
  @Timed
  public void unfollow(@NotNull @Valid UnfollowRequest request) {
    channelMonitor.cancel(request);
  }

  private class SamplesSourceCallback extends AbstractSamplesSourceCallback {
    private final FollowRequest request;

    public SamplesSourceCallback(FollowRequest request, AsyncResponse response) {
      super(response, request.getChannelId());
      this.request = request;
    }

    @Override
    protected Logger log() {
      return log;
    }

    @Override
    public void onSuccess(SamplesSourceHandler samplesSource) {
      P25ChannelSpec         channelSpec   = new P25ChannelSpec(request.getFrequency());
      P25Channel             channel       = new P25Channel(config.getP25Config(), channelSpec, config.getSamplesQueueSize());
      KinesisRecordProducer  sender        = senderFactory.create(request.getChannelId());
      Double                 srcLatitude   = samplesSource.getCapabilities().getLatitude();
      Double                 srcLongitude  = samplesSource.getCapabilities().getLongitude();
      ControlChannelFollower follower      = new ControlChannelFollower(sender, request, srcLatitude, srcLongitude, trafficTarget);
      ListenableFuture<Void> channelFuture = dspPool.submit(channel);

      if (!channelMonitor.monitor(request, channelFuture, follower)) {
        pendingRequests.remove(channelId);
        channelFuture.cancel(true);
        samplesSource.close();
        response.resume(Response.status(409).build());
      } else {
        log.info(channelId + " now following");
        pendingRequests.remove(channelId);
        channel.addSink(follower);
        samplesSource.setSink(channel);
        response.resume(Response.ok().build());

        MonitoredChannelCleanupCallback callback = new MonitoredChannelCleanupCallback(samplesSource, channelId);
        Futures.addCallback(channelFuture, callback);
        Futures.addCallback(samplesSource.getCloseFuture(), callback);
      }
    }

    @Override
    public void onFailure(@Nonnull Throwable throwable) {
      pendingRequests.remove(channelId);
      super.onFailure(throwable);
    }
  }

}
