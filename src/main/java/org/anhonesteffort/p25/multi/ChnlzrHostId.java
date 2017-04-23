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

public class ChnlzrHostId {

  private final String  hostname;
  private final Integer port;

  public ChnlzrHostId(String hostname, Integer port) {
    this.hostname = hostname;
    this.port     = port;
  }

  public String getHostname() {
    return hostname;
  }

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

}
