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

package org.anhonesteffort.p25.multi;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import java.util.List;

public class ChnlzrConfig {

  @Min(0)   private Integer connectionTimeoutMs;
  @Min(0)   private Integer idleStateThresholdMs;
  @NotEmpty private List<ChnlzrHostId> hosts;

  @JsonProperty
  public Integer getConnectionTimeoutMs() {
    return connectionTimeoutMs;
  }

  @JsonProperty
  public Integer getIdleStateThresholdMs() {
    return idleStateThresholdMs;
  }

  @JsonProperty
  public List<ChnlzrHostId> getHosts() {
    return hosts;
  }

}
