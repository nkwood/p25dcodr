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

import com.google.common.util.concurrent.FutureCallback;
import org.anhonesteffort.chnlzr.ProtocolErrorException;
import org.anhonesteffort.p25.chnlzr.SamplesSourceHandler;
import org.anhonesteffort.p25.metric.P25DcodrMetrics;
import org.anhonesteffort.p25.model.ChannelId;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.util.concurrent.CancellationException;

public abstract class AbstractSamplesSourceCallback implements FutureCallback<SamplesSourceHandler> {

  protected final AsyncResponse response;
  protected final ChannelId     channelId;

  protected AbstractSamplesSourceCallback(AsyncResponse response, ChannelId channelId) {
    this.response  = response;
    this.channelId = channelId;
  }

  protected abstract Logger log();

  @Override
  public void onFailure(@Nonnull Throwable throwable) {
    if (throwable instanceof ProtocolErrorException) {
      ProtocolErrorException error = (ProtocolErrorException) throwable;
      P25DcodrMetrics.getInstance().chnlzrRequestDenied(error.getCode());
      log().warn(channelId + " channel request not granted: " + error.getCode());
      response.resume(Response.status(503).build());
    } else if (throwable instanceof CancellationException) {
      log().warn(channelId + " channel request timed out");
      response.resume(Response.status(504).build());
    } else {
      log().error(channelId + " unexpected channel request error", throwable);
      response.resume(Response.status(500).build());
    }
  }

}
