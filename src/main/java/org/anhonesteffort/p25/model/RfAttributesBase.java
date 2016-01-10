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

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public abstract class RfAttributesBase {

  @NotNull protected Double  latitude;
  @NotNull protected Double  longitude;
  @Min(0)  protected Integer polarization;
  @Min(1)  protected Double  frequency;

  protected RfAttributesBase() { }

  protected RfAttributesBase(Double latitude, Double longitude, Integer polarization, Double frequency) {
    this.latitude     = latitude;
    this.longitude    = longitude;
    this.polarization = polarization;
    this.frequency    = frequency;
  }

  @JsonProperty
  public Double getLatitude() {
    return latitude;
  }

  @JsonProperty
  public Double getLongitude() {
    return longitude;
  }

  @JsonProperty
  public Integer getPolarization() {
    return polarization;
  }

  @JsonProperty
  public Double getFrequency() {
    return frequency;
  }

}
