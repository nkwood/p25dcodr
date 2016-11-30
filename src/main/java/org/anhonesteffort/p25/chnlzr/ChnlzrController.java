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

package org.anhonesteffort.p25.chnlzr;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.anhonesteffort.p25.metric.P25DcodrMetrics;

import javax.annotation.Nonnull;

import static org.anhonesteffort.chnlzr.capnp.Proto.Capabilities;
import static org.anhonesteffort.chnlzr.capnp.Proto.ChannelRequest;

public class ChnlzrController {

  private final HostId chnlzrHost;
  private final ChnlzrConnectionFactory factory;

  public ChnlzrController(HostId chnlzrHost, ChnlzrConnectionFactory factory) {
    this.chnlzrHost = chnlzrHost;
    this.factory    = factory;
  }

  public ListenableFuture<SamplesSourceHandler> createSourceFor(ChannelRequest.Reader request) {
    P25DcodrMetrics.getInstance().chnlzrRequest(request.getCenterFrequency());

    ListenableFuture<ChnlzrConnectionHandler> connectFuture = factory.create(chnlzrHost);
    SettableFuture<SamplesSourceHandler>      sourceFuture  = SettableFuture.create();

    Futures.addCallback(connectFuture, new ChnlzrConnectionCallback(sourceFuture, request));

    return sourceFuture;
  }

  private static class ChnlzrConnectionCallback implements FutureCallback<ChnlzrConnectionHandler> {
    private final SettableFuture<SamplesSourceHandler> sourceFuture;
    private final ChannelRequest.Reader                request;

    public ChnlzrConnectionCallback(SettableFuture<SamplesSourceHandler> sourceFuture,
                                    ChannelRequest.Reader                request)
    {
      this.sourceFuture = sourceFuture;
      this.request      = request;
    }

    @Override
    public void onSuccess(ChnlzrConnectionHandler connection) {
      P25DcodrMetrics.getInstance().chnlzrConnectSuccess();

      if (sourceFuture.isCancelled()) {
        connection.getContext().close();
      } else {
        SettableFuture<ChannelRequestHandler> requestFuture = SettableFuture.create();
        ChannelRequestHandler                 requester     = new ChannelRequestHandler(requestFuture, request);

        connection.getContext().pipeline().replace(connection, "requester", requester);
        Futures.addCallback(requestFuture, new ChannelRequestCallback(sourceFuture, connection.getCapabilities()));
      }
    }

    @Override
    public void onFailure(@Nonnull Throwable throwable) {
      P25DcodrMetrics.getInstance().chnlzrConnectFailure();
      sourceFuture.setException(throwable);
    }
  }

  private static class ChannelRequestCallback implements FutureCallback<ChannelRequestHandler> {
    private final SettableFuture<SamplesSourceHandler> sourceFuture;
    private final Capabilities.Reader capabilities;

    public ChannelRequestCallback(SettableFuture<SamplesSourceHandler> sourceFuture,
                                  Capabilities.Reader                  capabilities)
    {
      this.sourceFuture = sourceFuture;
      this.capabilities = capabilities;
    }

    @Override
    public void onSuccess(ChannelRequestHandler requester) {
      P25DcodrMetrics.getInstance().chnlzrRequestSuccess();

      SamplesSourceHandler samplesSource = new SamplesSourceHandler(requester.getContext(), capabilities, requester.getState());
      requester.getContext().pipeline().replace(requester, "streamer", samplesSource);

      if (!sourceFuture.set(samplesSource)) {
        requester.getContext().close();
      }
    }

    @Override
    public void onFailure(@Nonnull Throwable throwable) {
      P25DcodrMetrics.getInstance().chnlzrRequestFailure();
      sourceFuture.setException(throwable);
    }
  }

}
