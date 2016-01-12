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

public class ControlChannelId extends ChannelId {

  @NotNull private Integer siteId;

  public ControlChannelId() { }

  public ControlChannelId(Integer wacn, Integer systemId, Integer rfSubsystemId, Integer siteId) {
    super(TYPE_CONTROL, wacn, systemId, rfSubsystemId);
    this.siteId = siteId;
  }

  @JsonProperty
  public Integer getSiteId() {
    return siteId;
  }

  @Override
  public String toString() {
    return super.toString() + ":" + siteId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)                               return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o))                        return false;

    ControlChannelId other = (ControlChannelId) o;
    return siteId.equals(other.siteId);
  }

  @Override
  public int hashCode() {
    return (super.hashCode() * 31) + siteId.hashCode();
  }

}
