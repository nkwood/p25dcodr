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
import org.anhonesteffort.p25.monitor.Identifiable;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class FollowRequest extends RfAttributesBase implements Identifiable {

  @NotNull @Valid private ControlChannelId channelId;

  public FollowRequest() { }

  public FollowRequest(Double           latitude,
                       Double           longitude,
                       Integer          polarization,
                       Double           frequency,
                       ControlChannelId channelId)
  {
    super(latitude, longitude, polarization, frequency);
    this.channelId = channelId;
  }

  @Override
  @JsonProperty
  public ControlChannelId getChannelId() {
    return channelId;
  }

}
