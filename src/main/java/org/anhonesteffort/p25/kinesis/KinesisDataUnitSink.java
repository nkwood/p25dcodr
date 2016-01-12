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
import org.anhonesteffort.kinesis.KinesisRecordSender;
import org.anhonesteffort.kinesis.pack.MessagePackingException;
import org.anhonesteffort.kinesis.proto.ProtoP25;
import org.anhonesteffort.kinesis.proto.ProtoP25Factory;
import org.anhonesteffort.p25.model.ChannelId;
import org.anhonesteffort.p25.model.ControlChannelId;
import org.anhonesteffort.p25.model.DirectChannelId;
import org.anhonesteffort.p25.model.GroupChannelId;
import org.anhonesteffort.p25.monitor.DataUnitCounter;
import org.anhonesteffort.p25.protocol.frame.DataUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.anhonesteffort.kinesis.proto.ProtoP25.P25DataUnit;
import static org.anhonesteffort.kinesis.proto.ProtoP25.RfSubsystemId;

public class KinesisDataUnitSink implements Sink<DataUnit>, DataUnitCounter {

  private static final Logger log = LoggerFactory.getLogger(KinesisDataUnitSink.class);
  private final ProtoP25Factory protocol = new ProtoP25Factory();

  private final KinesisRecordSender                        sender;
  private final Optional<ProtoP25.ControlChannelId.Reader> controlId;
  private final Optional<ProtoP25.DirectChannelId.Reader>  directId;
  private final Optional<ProtoP25.GroupChannelId.Reader>   groupId;

  private Integer dataUnitCount = 0;

  public KinesisDataUnitSink(KinesisRecordSender sender, ChannelId channelId) {
    this.sender = sender;

    switch (channelId.getType()) {
      case ChannelId.TYPE_CONTROL:
        controlId = Optional.of(control((ControlChannelId) channelId));
        directId  = Optional.empty();
        groupId   = Optional.empty();
        break;

      case ChannelId.TYPE_TRAFFIC_DIRECT:
        controlId = Optional.empty();
        directId  = Optional.of(direct((DirectChannelId) channelId));
        groupId   = Optional.empty();
        break;

      case ChannelId.TYPE_TRAFFIC_GROUP:
        controlId = Optional.empty();
        directId  = Optional.empty();
        groupId   = Optional.of(group((GroupChannelId) channelId));
        break;

      default:
        throw new IllegalArgumentException("unknown channel type " + channelId.getType());
    }
  }

  private RfSubsystemId.Reader rfss(ChannelId id) {
    return protocol.rfSubsystemId(id.getWacn(), id.getSystemId(), id.getRfSubsystemId());
  }

  private ProtoP25.ControlChannelId.Reader control(ControlChannelId id) {
    return protocol.controlChannelId(rfss(id), id.getSiteId());
  }

  private ProtoP25.DirectChannelId.Reader direct(DirectChannelId id) {
    return protocol.directChannelId(rfss(id), id.getSourceId(), id.getDestinationId());
  }

  private ProtoP25.GroupChannelId.Reader group(GroupChannelId id) {
    return protocol.groupChannelId(rfss(id), id.getSourceId(), id.getGroupId());
  }

  @Override
  public void consume(DataUnit element) {
    if (!element.isIntact()) {
      return;
    } else {
      dataUnitCount++;
    }

    P25DataUnit.Reader message = null;

    if (controlId.isPresent()) {
      message = protocol.controlDataUnit(controlId.get(), element.getBytes().array());
    } else if (directId.isPresent()) {
      message = protocol.directDataUnit(directId.get(), element.getBytes().array());
    } else if (groupId.isPresent()) {
      message = protocol.groupDataUnit(groupId.get(), element.getBytes().array());
    }

    try {

      if (!sender.queue(protocol.p25DataUnit(System.currentTimeMillis(), message))) {
        log.warn("sender queue has overflowed, data units have been lost");
      }

    } catch (MessagePackingException e) {
      log.error("error packing message for send", e);
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
