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

package org.anhonesteffort.p25.chnlzr;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;

public class ChnlzrHostId {

  @NotEmpty private String  hostname;
  @Min(1)   private Integer port;

  @JsonProperty
  public String getHostname() {
    return hostname;
  }

  @JsonProperty
  public Integer getPort() {
    return port;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ChnlzrHostId that = (ChnlzrHostId) o;

    if (!hostname.equals(that.hostname)) return false;
    return port.equals(that.port);
  }

  @Override
  public int hashCode() {
    int result = hostname.hashCode();
    result = 31 * result + port.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return String.format("chnlzr://%s:%d", hostname, port);
  }

}
