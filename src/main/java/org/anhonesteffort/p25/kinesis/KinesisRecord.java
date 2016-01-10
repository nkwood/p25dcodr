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

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class KinesisRecord {

  public static int TYPE_P25_DATA_UNIT = 1;

  @NotNull private Integer type;
  @Min(0)  private Long    timestamp;

  public KinesisRecord() { }

  public KinesisRecord(Integer type, Long timestamp) {
    this.type      = type;
    this.timestamp = timestamp;
  }

  @JsonProperty
  public Integer getType() {
    return type;
  }

  @JsonProperty
  public Long getTimestamp() {
    return timestamp;
  }

}
