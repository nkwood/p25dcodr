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
import org.anhonesteffort.chnlzr.capnp.ProtoFactory;
import org.anhonesteffort.p25.metric.P25DcodrMetrics;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.anhonesteffort.chnlzr.capnp.Proto.ChannelRequest;

@AllArgsConstructor
public class ChnlzrController {

  private final ProtoFactory proto;
  private final ChnlzrConnectionFactory connections;
  private final ChnlzrHostId host;

  public CompletableFuture<SamplesSourceHandler> createSourceFor(ChannelRequest.Reader request) {
    P25DcodrMetrics.getInstance().chnlzrRequest(request.getCenterFrequency());

    CompletionStage<ChnlzrConnectionHandler> connecting = connections.create(host);
    CompletableFuture<ChannelRequestHandler> requesting = new CompletableFuture<>();
    CompletableFuture<SamplesSourceHandler>  streaming  = new CompletableFuture<>();

    connecting.whenComplete((connection, err) -> {
      if (err != null) {
        P25DcodrMetrics.getInstance().chnlzrConnectFailure();
        streaming.completeExceptionally(err);
      } else if (streaming.isDone()) {
        connection.getContext().close();
      } else {
        connection.getContext().pipeline().replace(
            connection, "requester",
            new ChannelRequestHandler(proto, requesting, connection.getCapabilities(), request)
        );
      }
    });

    requesting.whenComplete((requested, err) -> {
      if (err != null) {
        P25DcodrMetrics.getInstance().chnlzrRequestFailure();
        streaming.completeExceptionally(err);
      } else {
        P25DcodrMetrics.getInstance().chnlzrRequestSuccess();
        SamplesSourceHandler samplesSource = new SamplesSourceHandler(
            requested.getCapabilities(), requested.getState(), null
        );

        if (!streaming.complete(samplesSource)) {
          requested.getContext().close();
        } else {
          requested.getContext().pipeline().replace(requested, "streamer", samplesSource);
        }
      }
    });

    return streaming;
  }

}
