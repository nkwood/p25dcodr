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

package org.anhonesteffort.p25;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import org.anhonesteffort.p25.chnlzr.ChnlzrConfig;
import org.anhonesteffort.p25.kinesis.KinesisConfig;
import org.anhonesteffort.p25.metric.CloudWatchConfig;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class P25DcodrConfig extends Configuration {

  private final P25Config p25Config = new P25Config();

  @Valid
  @NotNull
  private JerseyClientConfiguration httpClient = new JerseyClientConfiguration();

  @Valid
  @NotNull
  private ChnlzrConfig chnlzr;

  @Valid
  @NotNull
  private KinesisConfig kinesis;

  @Valid
  @NotNull
  private CloudWatchConfig cloudWatch;

  @Min(1)   private Integer dspPoolSize;
  @Min(1)   private Long    channelRequestTimeoutMs;
  @Min(1)   private Long    channelQualifyTimeMs;
  @NotNull  private Double  minControlDataUnitRate;
  @NotNull  private Double  minTrafficDataUnitRate;
  @Min(0)   private Integer controlChannelRetryCount;
  @Min(0)   private Long    controlChannelRetryDelayMs;

  public P25Config getP25Config() {
    return p25Config;
  }

  public JerseyClientConfiguration getJerseyConfig() {
    return httpClient;
  }

  @JsonProperty
  public KinesisConfig getKinesis() {
    return kinesis;
  }

  @JsonProperty
  public CloudWatchConfig getCloudWatch() {
    return cloudWatch;
  }

  @JsonProperty
  public Integer getDspPoolSize() {
    return dspPoolSize;
  }

  @JsonProperty
  public Long getChannelRequestTimeoutMs() {
    return channelRequestTimeoutMs;
  }

  @JsonProperty
  public Long getChannelQualifyTimeMs() {
    return channelQualifyTimeMs;
  }

  @JsonProperty
  public Double getMinControlDataUnitRate() {
    return minControlDataUnitRate;
  }

  @JsonProperty
  public Double getMinTrafficDataUnitRate() {
    return minTrafficDataUnitRate;
  }

  @JsonProperty
  public Integer getControlChannelRetryCount() {
    return controlChannelRetryCount;
  }

  @JsonProperty
  public Long getControlChannelRetryDelayMs() {
    return controlChannelRetryDelayMs;
  }

}
