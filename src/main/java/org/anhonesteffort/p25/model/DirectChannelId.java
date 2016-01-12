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

package org.anhonesteffort.p25.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public class DirectChannelId extends TrafficChannelId {

  @NotNull private Integer destinationId;

  public DirectChannelId() { }

  public DirectChannelId(
      Integer wacn, Integer systemId, Integer rfSubsystemId, Integer sourceId, Integer destinationId
  ) {
    super(TYPE_TRAFFIC_DIRECT, wacn, systemId, rfSubsystemId, sourceId);
    this.destinationId = destinationId;
  }

  @JsonProperty
  public Integer getDestinationId() {
    return destinationId;
  }

  @Override
  public String toString() {
    return super.toString() + ":" + destinationId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)                               return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o))                        return false;

    DirectChannelId other = (DirectChannelId) o;
    return destinationId.equals(other.destinationId);
  }

  @Override
  public int hashCode() {
    return (super.hashCode() * 31) + destinationId.hashCode();
  }

}
