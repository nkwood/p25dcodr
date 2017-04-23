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

import org.anhonesteffort.chnlzr.ProtocolErrorException;
import org.anhonesteffort.chnlzr.capnp.ProtoFactory;
import org.anhonesteffort.dsp.StatefulSink;
import org.anhonesteffort.dsp.sample.Samples;
import org.anhonesteffort.p25.multi.netty.ChnlzrConnections;
import org.anhonesteffort.p25.multi.netty.SamplingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.anhonesteffort.chnlzr.capnp.Proto.ChannelRequest;
import static org.anhonesteffort.chnlzr.capnp.Proto.Error;

public class ChnlzrHostsController {

  private static final Logger log = LoggerFactory.getLogger(ChnlzrHostsController.class);

  private final List<ChnlzrHostController> controllers;

  public ChnlzrHostsController(ProtoFactory proto, ChnlzrConnections connections, ChnlzrHosts hosts) {
    controllers = hosts.getHosts()
        .stream()
        .map(hostId -> new ChnlzrHostController(proto, connections, hostId))
        .collect(Collectors.toList());
  }

  private void logError(int controllerIdx, Throwable err) {
    ChnlzrHostId hostId = controllers.get(controllerIdx).getHostId();
    log.debug(String.format("host %s failed to create sampler", hostId.toString()), err);
  }

  private void tryNext(
      StatefulSink<Samples> sink, ChannelRequest.Reader request,
      CompletableFuture<SamplingHandler> parent, int controllerIdx
  ) {
    if (controllers.get(controllerIdx) == null) {
      parent.completeExceptionally(new ProtocolErrorException(
          "all chnlzr hosts have rejected our request", Error.ERROR_BANDWIDTH_UNAVAILABLE
      ));
    } else {
      controllers.get(controllerIdx).createSamplerFor(sink, request).whenComplete((streamer, err) -> {
        if (err != null) {
          logError(controllerIdx, err);
          tryNext(sink, request, parent, controllerIdx + 1);
        } else {
          parent.complete(streamer);
        }
      });
    }
  }

  public CompletableFuture<SamplingHandler> createSamplerFor(StatefulSink<Samples> sink, ChannelRequest.Reader request) {
    CompletableFuture<SamplingHandler> parent = new CompletableFuture<>();
    tryNext(sink, request, parent, 0);
    return parent;
  }

}
