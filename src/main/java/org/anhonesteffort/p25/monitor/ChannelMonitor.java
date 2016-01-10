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
import org.anhonesteffort.p25.model.ChannelId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class ChannelMonitor {

  private   static final Logger log   = LoggerFactory.getLogger(ChannelMonitor.class);
  protected static final Timer  timer = new Timer(true);

  private   final Map<ChannelId, MonitorRecord> channels = new ConcurrentHashMap<>();
  private   final Object                        txnLock  = new Object();
  protected final P25DcodrConfig                config;

  private TimerTask timerTask;

  public ChannelMonitor(P25DcodrConfig config) {
    this.config = config;
    scheduleNewTask();
  }

  private void scheduleNewTask() {
    timerTask = new MonitorTask();
    timer.scheduleAtFixedRate(
        timerTask,
        (long) (1000l / config.getMinDataUnitRate()),
        (long) (1000l / config.getMinDataUnitRate())
    );
  }

  public boolean contains(ChannelId channelId) {
    return channels.containsKey(channelId);
  }

  public List<Identifiable> getMonitored() {
    return channels.keySet()
                   .stream()
                   .map(key -> channels.get(key).reference)
                   .collect(Collectors.toList());
  }

  public boolean monitor(Identifiable reference, Future channelFuture, DataUnitCounter counter) {
    synchronized (txnLock) {
      if (contains(reference.getChannelId())) {
        return false;
      } else {
        timerTask.cancel();
        channels.put(reference.getChannelId(), new MonitorRecord(reference, channelFuture, counter));
        scheduleNewTask();
        return true;
      }
    }
  }

  public void cancel(ChannelId channelId) {
    Optional<MonitorRecord> record = Optional.ofNullable(channels.remove(channelId));
    if (record.isPresent()) {
      record.get().future.cancel(true);
    }
  }

  protected void removeInactive(MonitorRecord record) {
    channels.remove(record.reference.getChannelId());
  }

  private class MonitorTask extends TimerTask {
    @Override
    public void run() {
      channels.keySet().forEach(key -> {
        MonitorRecord record = channels.get(key);
        if (record.counter.getDataUnitCount() > 0) {
          record.counter.resetDataUnitCount();
        } else {
          log.info(record.reference.getChannelId() + " hit inactive threshold, cancelling");
          record.future.cancel(true);
          removeInactive(record);
        }
      });
    }
  }

  protected static class MonitorRecord {
    protected final Identifiable    reference;
    protected final Future          future;
    protected final DataUnitCounter counter;

    protected MonitorRecord(Identifiable reference, Future future, DataUnitCounter counter) {
      this.reference = reference;
      this.future    = future;
      this.counter   = counter;
    }
  }

}
