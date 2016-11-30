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

package org.anhonesteffort.p25.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.anhonesteffort.p25.protocol.frame.tsbk.RfssStatusBroadcastMessage;

import javax.validation.constraints.NotNull;

public class ControlChannelQualities {

  // todo: modulation (CQPSK || C4FM)

  @NotNull private Integer wacn;
  @NotNull private Integer systemId;
  @NotNull private Integer localRegArea;
  @NotNull private Integer rfSubsystemId;
  @NotNull private Integer siteId;
  @NotNull private Integer manufactureId;
  @NotNull private Boolean isFssConnected;
  @NotNull private Integer systemServices;
  @NotNull private Double  frequency;
  @NotNull private Integer dataUnitCount;

  public ControlChannelQualities() { }

  public ControlChannelQualities(
      Integer wacn, RfssStatusBroadcastMessage status, Double frequency, Integer dataUnitCount
  ) {
    this.wacn          = wacn;
    systemId           = status.getSystemId();
    localRegArea       = status.getLra();
    rfSubsystemId      = status.getRfSubSystemId();
    siteId             = status.getSiteId();
    manufactureId      = status.getManufacturerId();
    isFssConnected     = status.isFssConnected();
    systemServices     = status.getSystemServiceClass();
    this.frequency     = frequency;
    this.dataUnitCount = dataUnitCount;
  }

  @JsonProperty
  public Integer getWacn() {
    return wacn;
  }

  @JsonProperty
  public Integer getSystemId() {
    return systemId;
  }

  @JsonProperty
  public Integer getLocalRegArea() {
    return localRegArea;
  }

  @JsonProperty
  public Integer getRfSubsystemId() {
    return rfSubsystemId;
  }

  @JsonProperty
  public Integer getSiteId() {
    return siteId;
  }

  @JsonProperty
  public Integer getManufactureId() {
    return manufactureId;
  }

  @JsonProperty
  public Boolean getIsFssConnected() {
    return isFssConnected;
  }

  @JsonProperty
  public Integer getSystemServices() {
    return systemServices;
  }

  @JsonProperty
  public Double getFrequency() {
    return frequency;
  }

  @JsonProperty
  public Integer getDataUnitCount() {
    return dataUnitCount;
  }

  @Override
  public String toString() {
    return "[" + "wacn:"             + wacn           + ", " +
                 "system id: "       + systemId       + ", " +
                 "rfss id: "         + rfSubsystemId  + ", " +
                 "site id: "         + siteId         + ", " +
                 "make: "            + manufactureId  + ", " +
                 "fss conn:"         + isFssConnected + ", " +
                 "services: "        + systemServices + ", " +
                 "frequency: "       + frequency      + ", " +
                 "data unit count: " + dataUnitCount  + "]";
  }

}
