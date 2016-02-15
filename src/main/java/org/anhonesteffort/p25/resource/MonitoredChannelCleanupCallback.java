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
import org.anhonesteffort.chnlzr.ProtocolErrorException;
import org.anhonesteffort.p25.chnlzr.SamplesSourceHandler;
import org.anhonesteffort.p25.metric.P25DcodrMetrics;
import org.anhonesteffort.p25.model.ChannelId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

class MonitoredChannelCleanupCallback implements FutureCallback<Void> {

  private static final Logger log = LoggerFactory.getLogger(MonitoredChannelCleanupCallback.class);

  private final SamplesSourceHandler samplesSource;
  private final ChannelId            channelId;

  private AtomicBoolean cleanupComplete = new AtomicBoolean(false);

  public MonitoredChannelCleanupCallback(SamplesSourceHandler samplesSource, ChannelId channelId) {
    this.samplesSource = samplesSource;
    this.channelId     = channelId;
  }

  @Override
  public void onSuccess(Void aVoid) {
    if (cleanupComplete.compareAndSet(false, true)) {
      samplesSource.close();
    }
  }

  @Override
  public void onFailure(@Nonnull Throwable cause) {
    if (cleanupComplete.compareAndSet(false, true)) {
      samplesSource.close();

      if (cause instanceof ProtocolErrorException) {
        ProtocolErrorException error = (ProtocolErrorException) cause;
        P25DcodrMetrics.getInstance().chnlzrStreamClosed(error.getCode());
        log.warn(channelId + " chnlzr closed connection with error: " + error.getCode());
      } else if (!(cause instanceof CancellationException)) {
        log.error(channelId + " unexpected error while decoding channel", cause);
      }
    }
  }

}
