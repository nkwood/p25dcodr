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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.anhonesteffort.p25.model.ChannelId;
import org.anhonesteffort.p25.monitor.Identifiable;
import org.anhonesteffort.p25.protocol.frame.DataUnit;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Base64;

public class P25KinesisRecord extends KinesisRecord implements Identifiable {

  @NotNull @Valid private ChannelId channelId;
  @NotEmpty       private String    dataUnitB64;

  public P25KinesisRecord() { }

  public P25KinesisRecord(Long timestamp, ChannelId channelId, String dataUnitB64) {
    super(TYPE_P25_DATA_UNIT, timestamp);
    this.channelId   = channelId;
    this.dataUnitB64 = dataUnitB64;
  }

  public P25KinesisRecord(Long timestamp, ChannelId channelId, DataUnit dataUnit) {
    this(timestamp, channelId, new String(Base64.getEncoder().encode(dataUnit.getBytes()).array()));
  }

  public P25KinesisRecord(ChannelId channelId, DataUnit dataUnit) {
    this(System.currentTimeMillis(), channelId, dataUnit);
  }

  @Override
  @JsonProperty
  public ChannelId getChannelId() {
    return channelId;
  }

  @JsonProperty
  public String getDataUnitB64() {
    return dataUnitB64;
  }

}