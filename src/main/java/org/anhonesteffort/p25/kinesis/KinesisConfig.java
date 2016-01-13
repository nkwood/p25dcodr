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

import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.anhonesteffort.kinesis.producer.KinesisProducerConfig;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;

public class KinesisConfig implements KinesisProducerConfig {

  @NotEmpty  private String  region;
  @NotEmpty  private String  streamName;
  @NotEmpty  private String  accessKeyId;
  @NotEmpty  private String  secretKey;
  @NotEmpty  private String  appName;
  @NotEmpty  private String  appVersion;
  @Min(2048) private Integer messageSizeMax;
  @Min(1)    private Integer payloadsPerRecordMax;
  @Min(1)    private Integer senderPoolSize;
  @Min(1)    private Integer senderQueueSize;
  @Min(1000) private Long    controlDelayMaxMs;
  @Min(1000) private Long    trafficDelayMaxMs;

  public KinesisConfig() { }

  @Override
  @JsonProperty
  public Region getRegion() {
    return RegionUtils.getRegion(region);
  }

  @Override
  @JsonProperty
  public String getStreamName() {
    return streamName;
  }

  @Override
  @JsonProperty
  public String getAccessKeyId() {
    return accessKeyId;
  }

  @Override
  @JsonProperty
  public String getSecretKey() {
    return secretKey;
  }

  @Override
  @JsonProperty
  public String getAppName() {
    return appName;
  }

  @Override
  @JsonProperty
  public String getAppVersion() {
    return appVersion;
  }

  @Override
  @JsonProperty
  public Integer getMessageSizeMax() {
    return messageSizeMax;
  }

  @Override
  @JsonProperty
  public Integer getPayloadsPerRecordMax() {
    return payloadsPerRecordMax;
  }

  @JsonProperty
  public Integer getSenderPoolSize() {
    return senderPoolSize;
  }

  @JsonProperty
  public Integer getSenderQueueSize() {
    return senderQueueSize;
  }

  @JsonProperty
  public Long getControlDelayMaxMs() {
    return controlDelayMaxMs;
  }

  @JsonProperty
  public Long getTrafficDelayMaxMs() {
    return trafficDelayMaxMs;
  }

}
