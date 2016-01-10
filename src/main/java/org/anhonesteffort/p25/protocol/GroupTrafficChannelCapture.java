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

package org.anhonesteffort.p25.protocol;

import org.anhonesteffort.dsp.Sink;
import org.anhonesteffort.p25.monitor.DataUnitCounter;
import org.anhonesteffort.p25.protocol.frame.DataUnit;

public class GroupTrafficChannelCapture implements Sink<DataUnit>, DataUnitCounter {

  private Integer dataUnitCount = 0;

  @Override
  public void consume(DataUnit element) {
    if (element.isIntact())
      dataUnitCount++;
  }

  @Override
  public Integer getDataUnitCount() {
    return dataUnitCount;
  }

  @Override
  public void resetDataUnitCount() {
    dataUnitCount = 0;
  }

}
