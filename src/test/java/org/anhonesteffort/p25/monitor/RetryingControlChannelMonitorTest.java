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

import org.anhonesteffort.p25.P25DcodrConfig;
import org.anhonesteffort.p25.model.ControlChannelId;
import org.anhonesteffort.p25.model.ControlChannelQualities;
import org.anhonesteffort.p25.model.FollowRequest;
import org.anhonesteffort.p25.model.GroupCaptureRequest;
import org.anhonesteffort.p25.model.GroupChannelId;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import java.util.concurrent.Future;

public class RetryingControlChannelMonitorTest {

  private P25DcodrConfig config() {
    P25DcodrConfig config = Mockito.mock(P25DcodrConfig.class);

    Mockito.when(config.getMinDataUnitRate()).thenReturn(1d);
    Mockito.when(config.getControlChannelRetryCount()).thenReturn(1);
    Mockito.when(config.getControlChannelRetryDelayMs()).thenReturn(0l);

    return config;
  }

  @Test
  public void testContains() {
    final WebTarget       QUALIFY = Mockito.mock(WebTarget.class);
    final WebTarget       FOLLOW  = Mockito.mock(WebTarget.class);
    final ChannelMonitor  MONITOR = new RetryingControlChannelMonitor(config(), QUALIFY, FOLLOW);
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
    final WebTarget       QUALIFY = Mockito.mock(WebTarget.class);
    final WebTarget       FOLLOW  = Mockito.mock(WebTarget.class);
    final ChannelMonitor  MONITOR = new RetryingControlChannelMonitor(config(), QUALIFY, FOLLOW);
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
    final WebTarget       QUALIFY = Mockito.mock(WebTarget.class);
    final WebTarget       FOLLOW  = Mockito.mock(WebTarget.class);
    final ChannelMonitor  MONITOR = new RetryingControlChannelMonitor(config(), QUALIFY, FOLLOW);
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
    final WebTarget       QUALIFY = Mockito.mock(WebTarget.class);
    final WebTarget       FOLLOW  = Mockito.mock(WebTarget.class);
    final ChannelMonitor  MONITOR = new RetryingControlChannelMonitor(config(), QUALIFY, FOLLOW);
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
  public void testInactiveTrafficCanceled() throws InterruptedException {
    final WebTarget       QUALIFY = Mockito.mock(WebTarget.class);
    final WebTarget       FOLLOW  = Mockito.mock(WebTarget.class);
    final ChannelMonitor  MONITOR = new RetryingControlChannelMonitor(config(), QUALIFY, FOLLOW);
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

  @Test
  public void testInactiveControlRetriedOnceAndFails() throws InterruptedException {
    final WebTarget          QUALIFY_TARGET  = Mockito.mock(WebTarget.class);
    final WebTarget          FOLLOW_TARGET   = Mockito.mock(WebTarget.class);
    final Invocation.Builder QUALIFY_BUILDER = Mockito.mock(Invocation.Builder.class);
    final AsyncInvoker       QUALIFY_ASYNC   = Mockito.mock(AsyncInvoker.class);

    Mockito.when(QUALIFY_TARGET.request()).thenReturn(QUALIFY_BUILDER);
    Mockito.when(QUALIFY_BUILDER.async()).thenReturn(QUALIFY_ASYNC);

    final ChannelMonitor  MONITOR        = new RetryingControlChannelMonitor(config(), QUALIFY_TARGET, FOLLOW_TARGET);
    final Future          CAPTURE_FUTURE = Mockito.mock(Future.class);
    final DataUnitCounter COUNTER        = Mockito.mock(DataUnitCounter.class);

    Mockito.when(COUNTER.getDataUnitCount()).thenReturn(0);

    final ControlChannelId id      = new ControlChannelId(10, 20, 30, 40);
    final Identifiable     capture = new FollowRequest(10d, 20d, 0, 1337d, id);

    assert MONITOR.monitor(capture, CAPTURE_FUTURE, COUNTER);
    assert MONITOR.contains(id);
    Mockito.verify(CAPTURE_FUTURE, Mockito.never()).cancel(Mockito.anyBoolean());

    Thread.sleep(1100);

    Mockito.verify(CAPTURE_FUTURE, Mockito.times(1)).cancel(Mockito.anyBoolean());
    assert MONITOR.contains(id);

    ArgumentCaptor<InvocationCallback> QUALIFY_CALLBACK = ArgumentCaptor.forClass(InvocationCallback.class);
    Mockito.verify(QUALIFY_ASYNC).post(Mockito.any(Entity.class), QUALIFY_CALLBACK.capture());
    QUALIFY_CALLBACK.getValue().failed(new Exception("lolwut"));

    assert !MONITOR.contains(id);
  }

  @Test
  public void testInactiveControlRetriedOnceAndSucceeds() throws InterruptedException {
    final WebTarget          QUALIFY_TARGET  = Mockito.mock(WebTarget.class);
    final WebTarget          FOLLOW_TARGET   = Mockito.mock(WebTarget.class);
    final Invocation.Builder QUALIFY_BUILDER = Mockito.mock(Invocation.Builder.class);
    final AsyncInvoker       QUALIFY_ASYNC   = Mockito.mock(AsyncInvoker.class);
    final Invocation.Builder FOLLOW_BUILDER  = Mockito.mock(Invocation.Builder.class);
    final AsyncInvoker       FOLLOW_ASYNC    = Mockito.mock(AsyncInvoker.class);

    Mockito.when(QUALIFY_TARGET.request()).thenReturn(QUALIFY_BUILDER);
    Mockito.when(QUALIFY_BUILDER.async()).thenReturn(QUALIFY_ASYNC);
    Mockito.when(FOLLOW_TARGET.request()).thenReturn(FOLLOW_BUILDER);
    Mockito.when(FOLLOW_BUILDER.async()).thenReturn(FOLLOW_ASYNC);

    final ChannelMonitor  MONITOR        = new RetryingControlChannelMonitor(config(), QUALIFY_TARGET, FOLLOW_TARGET);
    final Future          CAPTURE_FUTURE = Mockito.mock(Future.class);
    final DataUnitCounter COUNTER        = Mockito.mock(DataUnitCounter.class);

    Mockito.when(COUNTER.getDataUnitCount()).thenReturn(0);

    final ControlChannelId id      = new ControlChannelId(10, 20, 30, 40);
    final Identifiable     capture = new FollowRequest(10d, 20d, 0, 1337d, id);

    assert MONITOR.monitor(capture, CAPTURE_FUTURE, COUNTER);
    assert MONITOR.contains(id);
    Mockito.verify(CAPTURE_FUTURE, Mockito.never()).cancel(Mockito.anyBoolean());

    Thread.sleep(1100);

    Mockito.verify(CAPTURE_FUTURE, Mockito.times(1)).cancel(Mockito.anyBoolean());
    assert MONITOR.contains(id);

    ArgumentCaptor<InvocationCallback> QUALIFY_CALLBACK = ArgumentCaptor.forClass(InvocationCallback.class);
    Mockito.verify(QUALIFY_ASYNC).post(Mockito.any(Entity.class), QUALIFY_CALLBACK.capture());

    ControlChannelQualities QUALITIES = Mockito.mock(ControlChannelQualities.class);
    Mockito.when(QUALITIES.getWacn()).thenReturn(id.getWacn());
    Mockito.when(QUALITIES.getSystemId()).thenReturn(id.getSystemId());
    Mockito.when(QUALITIES.getRfSubsystemId()).thenReturn(id.getRfSubsystemId());
    Mockito.when(QUALITIES.getSiteId()).thenReturn(id.getSiteId());

    QUALIFY_CALLBACK.getValue().completed(QUALITIES);

    assert !MONITOR.contains(id);
    Mockito.verify(FOLLOW_ASYNC, Mockito.times(1)).post(Mockito.any(Entity.class), Mockito.any(InvocationCallback.class));
  }

}
