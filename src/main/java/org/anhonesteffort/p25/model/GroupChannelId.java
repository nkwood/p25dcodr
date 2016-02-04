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
import org.anhonesteffort.p25.P25Config;

import javax.validation.constraints.NotNull;

public class GroupChannelId extends ChannelId {

  /*
  notice:
    we have to keep track of frequency here because sourceId
    is not always immediately known, such is the case of 'group
    voice channel update' and 'group voice channel update
    explicit'. frequency is used to evaluate equality in the
    case of unknown sourceId.
   */

  @NotNull private Integer sourceId;
  @NotNull private Integer groupId;
  @NotNull private Double  frequency;

  public GroupChannelId() { }

  public GroupChannelId(
      Integer wacn, Integer systemId, Integer rfSubsystemId,
      Integer sourceId, Integer groupId, Double frequency
  ) {
    super(wacn, systemId, rfSubsystemId);

    this.sourceId  = sourceId;
    this.groupId   = groupId;
    this.frequency = frequency;
  }

  @Override
  public Type getType() {
    return Type.TRAFFIC_GROUP;
  }

  @JsonProperty
  public Integer getSourceId() {
    return sourceId;
  }

  @JsonProperty
  public Integer getGroupId() {
    return groupId;
  }

  @JsonProperty
  public Double getFrequency() {
    return frequency;
  }

  @Override
  public String toString() {
    return super.toString() + ":" + sourceId + ":" + groupId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)                               return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o))                        return false;

    GroupChannelId other = (GroupChannelId) o;

    if (!groupId.equals(other.groupId)) {
      return false;
    }

    if (sourceId.equals(other.sourceId)) {
      return true;
    } else if (sourceId == P25Config.UNIT_ID_NONE) {
      return frequency.equals(other.frequency);
    } else if (other.sourceId == P25Config.UNIT_ID_NONE) {
      return frequency.equals(other.frequency);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return (((((super.hashCode() * 31) + sourceId) * 31) + groupId.hashCode()) * 31) + frequency.hashCode();
  }

}
