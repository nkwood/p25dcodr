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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public abstract class ChannelId {

  @NotNull private Integer wacn;
  @NotNull private Integer systemId;
  @NotNull private Integer rfSubsystemId;

  public enum Type {
    CONTROL, TRAFFIC_DIRECT, TRAFFIC_GROUP, QUALIFY
  }

  protected ChannelId() { }

  protected ChannelId(Integer wacn, Integer systemId, Integer rfSubsystemId) {
    this.wacn          = wacn;
    this.systemId      = systemId;
    this.rfSubsystemId = rfSubsystemId;
  }

  @JsonIgnore
  public abstract Type getType();

  @JsonProperty
  public Integer getWacn() {
    return wacn;
  }

  @JsonProperty
  public Integer getSystemId() {
    return systemId;
  }

  @JsonProperty
  public Integer getRfSubsystemId() {
    return rfSubsystemId;
  }

  @Override
  public String toString() {
    return getType().name() + ":" + wacn + ":" + systemId + ":" + rfSubsystemId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ChannelId other = (ChannelId) o;

    return getType().equals(other.getType()) &&
           wacn.equals(other.wacn)           &&
           systemId.equals(other.systemId)   &&
           rfSubsystemId.equals(other.rfSubsystemId);
  }

  @Override
  public int hashCode() {
    int result = getType().hashCode();
        result = 31 * result + wacn.hashCode();
        result = 31 * result + systemId.hashCode();
        result = 31 * result + rfSubsystemId.hashCode();

    return result;
  }

}
