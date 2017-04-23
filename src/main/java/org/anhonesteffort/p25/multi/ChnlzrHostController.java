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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.anhonesteffort.chnlzr.capnp.ProtoFactory;
import org.anhonesteffort.dsp.StatefulSink;
import org.anhonesteffort.dsp.sample.Samples;
import org.anhonesteffort.p25.metric.P25DcodrMetrics;
import org.anhonesteffort.p25.multi.netty.ChnlzrConnections;
import org.anhonesteffort.p25.multi.netty.ConnectionHandler;
import org.anhonesteffort.p25.multi.netty.RequestHandler;
import org.anhonesteffort.p25.multi.netty.SamplingHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.anhonesteffort.chnlzr.capnp.Proto.ChannelRequest;

@AllArgsConstructor
public class ChnlzrHostController {

  private final ProtoFactory proto;
  private final ChnlzrConnections connections;
  @Getter private final ChnlzrHostId hostId;

  public CompletableFuture<SamplingHandler> createSamplerFor(StatefulSink<Samples> sink, ChannelRequest.Reader request) {
    P25DcodrMetrics.getInstance().chnlzrRequest(request.getCenterFrequency());

    CompletionStage<ConnectionHandler>   connecting = connections.connect(hostId);
    CompletableFuture<RequestHandler>    requesting = new CompletableFuture<>();
    CompletableFuture<SamplingHandler>   sampling   = new CompletableFuture<>();

    connecting.whenComplete((connector, err) -> {
      if (err != null) {
        P25DcodrMetrics.getInstance().chnlzrConnectFailure();
        sampling.completeExceptionally(err);
      } else if (sampling.isDone()) {
        connector.getContext().close();
      } else {
        connector.getContext().pipeline().replace(
            connector, "requester",
            new RequestHandler(proto, requesting, connector.getCapabilities(), request)
        );
      }
    });

    requesting.whenComplete((requester, err) -> {
      if (err != null) {
        P25DcodrMetrics.getInstance().chnlzrRequestFailure();
        sampling.completeExceptionally(err);
      } else {
        P25DcodrMetrics.getInstance().chnlzrRequestSuccess();

        SamplingHandler sampler = new SamplingHandler(requester.getCapabilities(), requester.getState(), sink);
        if (!sampling.complete(sampler)) {
          requester.getContext().close();
        } else {
          requester.getContext().pipeline().replace(requester, "sampler", sampler);
        }
      }
    });

    return sampling;
  }

}
