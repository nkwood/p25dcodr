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

package org.anhonesteffort.p25.kinesis;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.kinesis.AmazonKinesisAsync;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.anhonesteffort.dsp.Sink;
import org.anhonesteffort.p25.model.ChannelId;
import org.anhonesteffort.p25.protocol.frame.DataUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class KinesisSink implements Sink<DataUnit> {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Logger       log  = LoggerFactory.getLogger(KinesisSink.class);

  static {
    JSON.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  private final KinesisConfig      config;
  private final AmazonKinesisAsync client;
  private final ChannelId          channelId;

  public KinesisSink(KinesisConfig config, AmazonKinesisAsync client, ChannelId channelId) {
    this.config    = config;
    this.client    = client;
    this.channelId = channelId;
  }

  @Override
  public void consume(DataUnit dataUnit) {
    P25KinesisRecord record     = new P25KinesisRecord(channelId, dataUnit);
    PutRecordRequest putRequest = new PutRecordRequest();

    putRequest.setStreamName(config.getStream());
    putRequest.setPartitionKey(channelId.toString());

    try {

      putRequest.setData(ByteBuffer.wrap(JSON.writeValueAsBytes(record)));
      client.putRecord(putRequest);

    } catch (JsonProcessingException e) {

    } catch (AmazonClientException e) {

    }
  }

}
