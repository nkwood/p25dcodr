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

import com.google.common.util.concurrent.ListenableFuture;
import org.anhonesteffort.kinesis.producer.KinesisRecordProducer;
import org.anhonesteffort.p25.kinesis.KinesisDataUnitSink;
import org.anhonesteffort.p25.model.ChannelId;
import org.anhonesteffort.p25.protocol.frame.DataUnit;

public class GroupTrafficChannelCapture extends KinesisDataUnitSink {

  private final ListenableFuture<Void> future;

  public GroupTrafficChannelCapture(ListenableFuture<Void> future,
                                    KinesisRecordProducer  sender,
                                    ChannelId              channelId)
  {
    super(sender, channelId);
    this.future = future;
  }

  @Override
  public void consume(DataUnit element) {
    super.consume(element);
    if (!element.isIntact())
      return;

    switch (element.getNid().getDuid().getId()) {
      case Duid.ID_TERMINATOR_W_LINK:
      case Duid.ID_TERMINATOR_WO_LINK:
        future.cancel(true);
        break;
    }
  }

}
