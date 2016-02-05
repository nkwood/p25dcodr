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

public class GroupChannelIdTest {

  @Test
  public void test() {
    final GroupChannelId ID1 = new GroupChannelId(1, 2, 3, 4, 5, 6d);

    assert ID1.getType()          == ChannelId.Type.TRAFFIC_GROUP;
    assert ID1.getWacn()          == 1;
    assert ID1.getSystemId()      == 2;
    assert ID1.getRfSubsystemId() == 3;
    assert ID1.getSourceId()      == 4;
    assert ID1.getGroupId()       == 5;
    assert ID1.getFrequency()     == 6d;

    final GroupChannelId ID2 = new GroupChannelId(1, 2, 3, 4, 5, 6d);
    final GroupChannelId ID3 = new GroupChannelId(2, 2, 3, 4, 5, 6d);
    final GroupChannelId ID4 = new GroupChannelId(1, 3, 3, 4, 5, 6d);
    final GroupChannelId ID5 = new GroupChannelId(1, 2, 4, 4, 5, 6d);
    final GroupChannelId ID6 = new GroupChannelId(1, 2, 3, 5, 5, 6d);
    final GroupChannelId ID7 = new GroupChannelId(1, 2, 3, 4, 6, 6d);
    final GroupChannelId ID8 = new GroupChannelId(1, 2, 3, 4, 5, 7d);

    assert ID1.equals(ID2);
    assert ID1.hashCode() == ID2.hashCode();

    assert !ID1.equals(ID3);
    assert ID1.hashCode() != ID3.hashCode();

    assert !ID1.equals(ID4);
    assert ID1.hashCode() != ID4.hashCode();

    assert !ID1.equals(ID5);
    assert ID1.hashCode() != ID5.hashCode();

    assert ID1.equals(ID6);
    assert ID1.hashCode() == ID6.hashCode();

    assert !ID1.equals(ID7);
    assert ID1.hashCode() != ID7.hashCode();

    assert !ID1.equals(ID8);
    assert ID1.hashCode() != ID8.hashCode();
  }

  @Test
  public void testSourceIdIgnored() {
    final GroupChannelId ID1 = new GroupChannelId(1, 2, 3, 4, 5, 6d);
    final GroupChannelId ID2 = new GroupChannelId(1, 2, 3, 5, 5, 6d);
    final GroupChannelId ID3 = new GroupChannelId(1, 2, 3, 4, 5, 7d);
    final GroupChannelId ID4 = new GroupChannelId(1, 2, 3, 4, 5, 8d);

    assert ID1.equals(ID2);
    assert ID2.equals(ID1);
    assert ID1.hashCode() == ID2.hashCode();

    assert !ID3.equals(ID4);
    assert !ID4.equals(ID3);
    assert ID3.hashCode() != ID4.hashCode();
  }

}
