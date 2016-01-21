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

import org.junit.Test;

public class ControlChannelIdTest {

  @Test
  public void test() {
    final ControlChannelId ID1 = new ControlChannelId(1, 2, 3, 4);

    assert ID1.getType()          == ChannelId.Type.CONTROL;
    assert ID1.getWacn()          == 1;
    assert ID1.getSystemId()      == 2;
    assert ID1.getRfSubsystemId() == 3;
    assert ID1.getSiteId()        == 4;

    final ControlChannelId ID2 = new ControlChannelId(1, 2, 3, 4);
    final ControlChannelId ID3 = new ControlChannelId(2, 2, 3, 4);
    final ControlChannelId ID4 = new ControlChannelId(1, 3, 3, 4);
    final ControlChannelId ID5 = new ControlChannelId(1, 2, 4, 4);
    final ControlChannelId ID6 = new ControlChannelId(1, 2, 3, 5);

    assert ID1.equals(ID2);
    assert ID1.hashCode() == ID2.hashCode();

    assert !ID1.equals(ID3);
    assert ID1.hashCode() != ID3.hashCode();

    assert !ID1.equals(ID4);
    assert ID1.hashCode() != ID4.hashCode();

    assert !ID1.equals(ID5);
    assert ID1.hashCode() != ID5.hashCode();

    assert !ID1.equals(ID6);
    assert ID1.hashCode() != ID6.hashCode();
  }

}
