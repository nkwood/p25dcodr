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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import javax.annotation.Nonnull;

import static org.anhonesteffort.chnlzr.Proto.HostId;
import static org.anhonesteffort.chnlzr.Proto.ChannelRequest;

public class ChnlBrkrController {

  private final HostId.Reader             brkrHost;
  private final ChnlBrkrConnectionFactory factory;

  public ChnlBrkrController(HostId.Reader brkrHost, ChnlBrkrConnectionFactory factory) {
    this.brkrHost = brkrHost;
    this.factory  = factory;
  }

  public ListenableFuture<SamplesSourceHandler> createSourceFor(ChannelRequest.Reader request) {
    ListenableFuture<ChnlBrkrConnectionHandler> connectFuture = factory.create(brkrHost);
    SettableFuture<SamplesSourceHandler>        sourceFuture  = SettableFuture.create();

    Futures.addCallback(connectFuture, new BrkrConnectionCallback(sourceFuture, request));

    return sourceFuture;
  }

  private class BrkrConnectionCallback implements FutureCallback<ChnlBrkrConnectionHandler> {
    private final SettableFuture<SamplesSourceHandler> sourceFuture;
    private final ChannelRequest.Reader                request;

    public BrkrConnectionCallback(SettableFuture<SamplesSourceHandler> sourceFuture,
                                  ChannelRequest.Reader                request)
    {
      this.sourceFuture = sourceFuture;
      this.request      = request;
    }

    @Override
    public void onSuccess(ChnlBrkrConnectionHandler connection) {
      if (sourceFuture.isCancelled()) {
        connection.getContext().close();
      } else {
        SettableFuture<ChannelRequestHandler> requestFuture = SettableFuture.create();
        ChannelRequestHandler                 requester     = new ChannelRequestHandler(requestFuture, request);

        connection.getContext().pipeline().replace(connection, "requester", requester);
        Futures.addCallback(requestFuture, new ChannelRequestCallback(sourceFuture));
      }
    }

    @Override
    public void onFailure(@Nonnull Throwable throwable) {
      sourceFuture.setException(throwable);
    }
  }

  private class ChannelRequestCallback implements FutureCallback<ChannelRequestHandler> {
    private final SettableFuture<SamplesSourceHandler> sourceFuture;

    public ChannelRequestCallback(SettableFuture<SamplesSourceHandler> sourceFuture) {
      this.sourceFuture = sourceFuture;
    }

    @Override
    public void onSuccess(ChannelRequestHandler requester) {
      SamplesSourceHandler samplesSource = new SamplesSourceHandler(requester.getContext(), requester.getState());
      requester.getContext().pipeline().replace(requester, "sampler", samplesSource);

      if (!sourceFuture.set(samplesSource)) {
        requester.getContext().close();
      }
    }

    @Override
    public void onFailure(@Nonnull Throwable throwable) {
      sourceFuture.setException(throwable);
    }
  }

}
