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

public class GroupChannelId extends TrafficChannelId {

  @NotNull private Integer groupId;

  public GroupChannelId() { }

  public GroupChannelId(
      Integer wacn, Integer systemId, Integer rfSubsystemId, Integer sourceId, Integer groupId
  ) {
    super(wacn, systemId, rfSubsystemId, sourceId);
    this.groupId = groupId;
  }

  @Override
  public Type getType() {
    return Type.TRAFFIC_GROUP;
  }

  @JsonProperty
  public Integer getGroupId() {
    return groupId;
  }

  @Override
  public String toString() {
    return super.toString() + ":" + groupId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)                               return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o))                        return false;

    GroupChannelId other = (GroupChannelId) o;
    return groupId.equals(other.groupId);
  }

  @Override
  public int hashCode() {
    return (super.hashCode() * 31) + groupId.hashCode();
  }

}
