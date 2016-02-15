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

package org.anhonesteffort.p25.monitor;

import org.anhonesteffort.p25.metric.MockMetrics;
import org.anhonesteffort.p25.metric.P25DcodrMetrics;
import org.anhonesteffort.p25.model.GroupCaptureRequest;
import org.anhonesteffort.p25.model.GroupChannelId;
import org.anhonesteffort.p25.P25DcodrConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.Future;

public class ChannelMonitorTest {

  private P25DcodrConfig config() {
    P25DcodrConfig config = Mockito.mock(P25DcodrConfig.class);
    Mockito.when(config.getMinDataUnitRate()).thenReturn(1d);
    return config;
  }

  @Before
  public void mockMetrics() {
    final P25DcodrMetrics mock = Mockito.mock(P25DcodrMetrics.class);
    MockMetrics.mockWith(mock);
  }

  @Test
  public void testContains() {
    final ChannelMonitor  MONITOR = new ChannelMonitor(config());
    final Future          FUTURE  = Mockito.mock(Future.class);
    final DataUnitCounter COUNTER = Mockito.mock(DataUnitCounter.class);

    Mockito.when(COUNTER.getDataUnitCount()).thenReturn(1337);

    final GroupChannelId id      = new GroupChannelId(10, 20, 30, 40, 50, 60d);
    final GroupChannelId idCopy  = new GroupChannelId(10, 20, 30, 40, 50, 60d);
    final GroupChannelId notId   = new GroupChannelId(10, 20, 30, 40, 51, 60d);
    final Identifiable   capture = new GroupCaptureRequest(10d, 20d, 0, 1337d, id);

    assert MONITOR.monitor(capture, FUTURE, COUNTER);
    assert MONITOR.contains(idCopy);
    assert !MONITOR.contains(notId);
  }

  @Test
  public void testGetMonitored() {
    final ChannelMonitor  MONITOR = new ChannelMonitor(config());
    final Future          FUTURE  = Mockito.mock(Future.class);
    final DataUnitCounter COUNTER = Mockito.mock(DataUnitCounter.class);

    Mockito.when(COUNTER.getDataUnitCount()).thenReturn(1337);

    final GroupChannelId id      = new GroupChannelId(10, 20, 30, 40, 50, 60d);
    final Identifiable   capture = new GroupCaptureRequest(10d, 20d, 0, 1337d, id);

    assert MONITOR.monitor(capture, FUTURE, COUNTER);
    assert MONITOR.getMonitored().size() == 1;
    assert MONITOR.getMonitored().get(0).getChannelId().equals(id);
  }

  @Test
  public void testCancel() {
    final ChannelMonitor  MONITOR = new ChannelMonitor(config());
    final Future          FUTURE  = Mockito.mock(Future.class);
    final DataUnitCounter COUNTER = Mockito.mock(DataUnitCounter.class);

    Mockito.when(COUNTER.getDataUnitCount()).thenReturn(1337);

    final GroupChannelId id      = new GroupChannelId(10, 20, 30, 40, 50, 60d);
    final Identifiable   capture = new GroupCaptureRequest(10d, 20d, 0, 1337d, id);

    assert MONITOR.monitor(capture, FUTURE, COUNTER);
    assert MONITOR.contains(id);
    Mockito.verify(FUTURE, Mockito.never()).cancel(Mockito.anyBoolean());

    MONITOR.cancel(id);

    Mockito.verify(FUTURE, Mockito.times(1)).cancel(Mockito.anyBoolean());
    assert !MONITOR.contains(id);
  }

  @Test
  public void testActiveNotCanceled() throws InterruptedException {
    final ChannelMonitor  MONITOR = new ChannelMonitor(config());
    final Future          FUTURE  = Mockito.mock(Future.class);
    final DataUnitCounter COUNTER = Mockito.mock(DataUnitCounter.class);

    Mockito.when(COUNTER.getDataUnitCount()).thenReturn(1337);

    final GroupChannelId id      = new GroupChannelId(10, 20, 30, 40, 50, 60d);
    final Identifiable   capture = new GroupCaptureRequest(10d, 20d, 0, 1337d, id);

    assert MONITOR.monitor(capture, FUTURE, COUNTER);
    assert MONITOR.contains(id);

    Thread.sleep(1100);

    assert MONITOR.contains(id);
    Mockito.verify(FUTURE, Mockito.never()).cancel(Mockito.anyBoolean());
  }

  @Test
  public void testInactiveCanceled() throws InterruptedException {
    final ChannelMonitor  MONITOR = new ChannelMonitor(config());
    final Future          FUTURE  = Mockito.mock(Future.class);
    final DataUnitCounter COUNTER = Mockito.mock(DataUnitCounter.class);

    Mockito.when(COUNTER.getDataUnitCount()).thenReturn(0);

    final GroupChannelId id      = new GroupChannelId(10, 20, 30, 40, 50, 60d);
    final Identifiable   capture = new GroupCaptureRequest(10d, 20d, 0, 1337d, id);

    assert MONITOR.monitor(capture, FUTURE, COUNTER);
    assert MONITOR.contains(id);
    Mockito.verify(FUTURE, Mockito.never()).cancel(Mockito.anyBoolean());

    Thread.sleep(1100);

    Mockito.verify(FUTURE, Mockito.times(1)).cancel(Mockito.anyBoolean());
    assert !MONITOR.contains(id);
  }

}
