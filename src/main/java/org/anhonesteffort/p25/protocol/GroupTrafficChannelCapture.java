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

package org.anhonesteffort.p25.protocol;

import org.anhonesteffort.kinesis.producer.KinesisRecordProducer;
import org.anhonesteffort.p25.kinesis.KinesisDataUnitSink;
import org.anhonesteffort.p25.model.ChannelId;
import org.anhonesteffort.p25.protocol.frame.DataUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupTrafficChannelCapture extends KinesisDataUnitSink {

  private static final Logger log = LoggerFactory.getLogger(GroupTrafficChannelCapture.class);

  private final ChannelId channelId;

  public GroupTrafficChannelCapture(KinesisRecordProducer sender,
                                    ChannelId             channelId,
                                    Double                srcLatitude,
                                    Double                srcLongitude)
  {
    super(sender, channelId, srcLatitude, srcLongitude);
    this.channelId = channelId;
  }

  @Override
  public void consume(DataUnit element) {
    super.consume(element);
    if (!element.isIntact())
      return;

    switch (element.getNid().getDuid().getId()) {
      case Duid.ID_TERMINATOR_W_LINK:
      case Duid.ID_TERMINATOR_WO_LINK:
        log.info(channelId + " terminated by protocol, canceling");
        Thread.currentThread().interrupt();
        break;
    }
  }

}
