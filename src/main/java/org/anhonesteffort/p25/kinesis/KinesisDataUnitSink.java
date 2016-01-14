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

package org.anhonesteffort.p25.kinesis;

import org.anhonesteffort.dsp.Sink;
import org.anhonesteffort.kinesis.pack.MessagePackingException;
import org.anhonesteffort.kinesis.producer.KinesisRecordProducer;
import org.anhonesteffort.kinesis.proto.ProtoP25Factory;
import org.anhonesteffort.p25.model.ChannelId;
import org.anhonesteffort.p25.model.ControlChannelId;
import org.anhonesteffort.p25.model.DirectChannelId;
import org.anhonesteffort.p25.model.GroupChannelId;
import org.anhonesteffort.p25.monitor.DataUnitCounter;
import org.anhonesteffort.p25.protocol.frame.DataUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.anhonesteffort.kinesis.proto.ProtoP25.P25DataUnit;
import static org.anhonesteffort.kinesis.proto.ProtoP25.P25ChannelId;

public class KinesisDataUnitSink implements Sink<DataUnit>, DataUnitCounter {

  private static final Logger log = LoggerFactory.getLogger(KinesisDataUnitSink.class);
  private final ProtoP25Factory protocol = new ProtoP25Factory();

  private final KinesisRecordProducer sender;
  private final P25ChannelId.Reader   channelId;

  private Integer dataUnitCount = 0;

  public KinesisDataUnitSink(KinesisRecordProducer sender, ChannelId channelId) {
    this.sender = sender;

    switch (channelId.getType()) {
      case CONTROL:
        this.channelId = translate((ControlChannelId) channelId);
        break;

      case TRAFFIC_DIRECT:
        this.channelId = translate((DirectChannelId) channelId);
        break;

      case TRAFFIC_GROUP:
        this.channelId = translate((GroupChannelId) channelId);
        break;

      default:
        throw new IllegalArgumentException("unknown channel type " + channelId.getType());
    }
  }

  private P25ChannelId.Reader translate(ControlChannelId id) {
    return protocol.controlId(
        id.getWacn(), id.getSystemId(), id.getRfSubsystemId(), id.getSiteId()
    );
  }

  private P25ChannelId.Reader translate(DirectChannelId id) {
    return protocol.directId(
        id.getWacn(), id.getSystemId(), id.getRfSubsystemId(), id.getSourceId(), id.getDestinationId()
    );
  }

  private P25ChannelId.Reader translate(GroupChannelId id) {
    return protocol.groupId(
        id.getWacn(), id.getSystemId(), id.getRfSubsystemId(), id.getSourceId(), id.getGroupId()
    );
  }

  @Override
  public void consume(DataUnit element) {
    if (!element.isIntact()) {
      return;
    } else {
      dataUnitCount++;
    }

    P25DataUnit.Reader dataUnit = protocol.dataUnit(
        channelId, element.getNid().getNac(),
        element.getNid().getDuid().getId(), element.getBuffer().array()
    );

    try {

      if (!sender.queue(protocol.message(System.currentTimeMillis(), dataUnit))) {
        log.warn(channelId + " sender queue has overflowed, data units lost");
      }

    } catch (MessagePackingException e) {
      log.error(channelId + " error packing message for send", e);
    }
  }

  @Override
  public Integer getDataUnitCount() {
    return dataUnitCount;
  }

  @Override
  public void resetDataUnitCount() {
    dataUnitCount = 0;
  }

}
