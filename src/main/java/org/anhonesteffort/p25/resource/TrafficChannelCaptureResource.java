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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.anhonesteffort.chnlzr.CapnpUtil;
import org.anhonesteffort.kinesis.producer.KinesisRecordProducer;
import org.anhonesteffort.p25.P25Channel;
import org.anhonesteffort.p25.P25ChannelSpec;
import org.anhonesteffort.p25.P25Config;
import org.anhonesteffort.p25.P25DcodrConfig;
import org.anhonesteffort.p25.chnlzr.ChnlzrController;
import org.anhonesteffort.p25.chnlzr.SamplesSourceHandler;
import org.anhonesteffort.p25.kinesis.KinesisRecordProducerFactory;
import org.anhonesteffort.p25.model.ChannelId;
import org.anhonesteffort.p25.model.GroupCaptureRequest;
import org.anhonesteffort.p25.monitor.ChannelMonitor;
import org.anhonesteffort.p25.protocol.GroupTrafficChannelCapture;
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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.anhonesteffort.chnlzr.Proto.ChannelRequest;

@Path("/channels/traffic")
@Produces(MediaType.APPLICATION_JSON)
public class TrafficChannelCaptureResource {

  private static final Logger log = LoggerFactory.getLogger(TrafficChannelCaptureResource.class);

  private final Queue<ChannelId> pendingRequests = new ConcurrentLinkedQueue<>();
  private final Object           txnLock         = new Object();

  private final P25DcodrConfig               config;
  private final ChnlzrController             chnlzr;
  private final ChannelMonitor               channelMonitor;
  private final KinesisRecordProducerFactory senderFactory;
  private final ListeningExecutorService     dspPool;

  public TrafficChannelCaptureResource(P25DcodrConfig               config,
                                       ChnlzrController             chnlzr,
                                       ChannelMonitor               channelMonitor,
                                       KinesisRecordProducerFactory senderFactory,
                                       ListeningExecutorService     dspPool)
  {
    this.config         = config;
    this.chnlzr         = chnlzr;
    this.channelMonitor = channelMonitor;
    this.senderFactory  = senderFactory;
    this.dspPool        = dspPool;
  }

  private ChannelRequest.Reader transform(GroupCaptureRequest request) {
    return CapnpUtil.channelRequest(
        request.getLatitude(), request.getLongitude(), 0, request.getPolarization(),
        request.getFrequency(), P25Config.CHANNEL_WIDTH, P25Config.SAMPLE_RATE,
        config.getP25Config().getMaxRateDiff()
    );
  }

  @POST
  @Path("/group")
  public void capture(@NotNull @Valid GroupCaptureRequest request, @Suspended AsyncResponse response) {
    log.debug(request.getChannelId() + " requested");

    synchronized (txnLock) {
      if (pendingRequests.contains(request.getChannelId()) ||
          channelMonitor.contains(request.getChannelId()))
      {
        response.resume(Response.status(409).build());
        return;
      } else {
        pendingRequests.add(request.getChannelId());
      }
    }

    ChannelRequest.Reader                  channelRequest = transform(request);
    ListenableFuture<SamplesSourceHandler> sourceFuture   = chnlzr.createSourceFor(channelRequest);

    Futures.addCallback(sourceFuture, new SamplesSourceCallback(request, response));

    response.setTimeout(config.getChannelRequestTimeoutMs(), TimeUnit.MILLISECONDS);
    response.setTimeoutHandler(asyncResponse -> sourceFuture.cancel(true));
  }

  private class SamplesSourceCallback extends AbstractSamplesSourceCallback {
    private final GroupCaptureRequest request;

    public SamplesSourceCallback(GroupCaptureRequest request, AsyncResponse response) {
      super(response, request.getChannelId());
      this.request = request;
    }

    @Override
    protected Logger log() {
      return log;
    }

    @Override
    public void onSuccess(SamplesSourceHandler samplesSource) {
      P25ChannelSpec             channelSpec   = new P25ChannelSpec(request.getFrequency());
      P25Channel                 channel       = new P25Channel(config.getP25Config(), channelSpec, config.getSamplesQueueSize());
      KinesisRecordProducer      sender        = senderFactory.create(request.getChannelId());
      Double                     srcLatitude   = samplesSource.getCapabilities().getLatitude();
      Double                     srcLongitude  = samplesSource.getCapabilities().getLongitude();
      ListenableFuture<Void>     channelFuture = dspPool.submit(channel);
      GroupTrafficChannelCapture capture       = new GroupTrafficChannelCapture(sender, request.getChannelId(), srcLatitude, srcLongitude, channelFuture);

      if (!channelMonitor.monitor(request, channelFuture, capture)) {
        pendingRequests.remove(channelId);
        channelFuture.cancel(true);
        samplesSource.close();
        response.resume(Response.status(409).build());
      } else {
        log.info(channelId + " now capturing");
        pendingRequests.remove(channelId);
        channel.addSink(capture);
        samplesSource.setSink(channel);
        response.resume(Response.ok().build());

        MonitoredChannelCleanupCallback callback = new MonitoredChannelCleanupCallback(samplesSource, channelId);
        Futures.addCallback(channelFuture, callback);
        samplesSource.getCloseFuture().addListener(callback);
      }
    }

    @Override
    public void onFailure(@Nonnull Throwable throwable) {
      pendingRequests.remove(channelId);
      super.onFailure(throwable);
    }
  }

}
