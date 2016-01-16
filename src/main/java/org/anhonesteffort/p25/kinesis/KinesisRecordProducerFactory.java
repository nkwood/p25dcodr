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

import org.anhonesteffort.kinesis.producer.KinesisClientFactory;
import org.anhonesteffort.kinesis.producer.KinesisRecordProducer;
import org.anhonesteffort.kinesis.producer.PutRecordTaskFactory;
import org.anhonesteffort.p25.model.ChannelId;

public class KinesisRecordProducerFactory {

  private final KinesisConfig        config;
  private final KinesisClientFactory clients;

  public KinesisRecordProducerFactory(KinesisConfig config, KinesisClientFactory clients) {
    this.config  = config;
    this.clients = clients;
  }

  public KinesisRecordProducer create(ChannelId channelId) {
    String partitionKey = channelId.toString();
    Long   messageDelay;

    switch (channelId.getType()) {
      case CONTROL:
        messageDelay = config.getControlDelayMaxMs();
        break;

      case TRAFFIC_DIRECT:
      case TRAFFIC_GROUP:
        messageDelay = config.getTrafficDelayMaxMs();
        break;

      default:
        throw new IllegalArgumentException("unknown channel type " + channelId.getType());
    }

    return new KinesisRecordProducer(
        new PutRecordTaskFactory(config, clients.create()),
        partitionKey, messageDelay, config.getSenderQueueSize()
    );
  }

}
