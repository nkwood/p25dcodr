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
import org.anhonesteffort.p25.model.ControlChannelId;
import org.anhonesteffort.p25.model.ControlChannelQualities;
import org.anhonesteffort.p25.model.FollowRequest;
import org.anhonesteffort.p25.model.QualifyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class RetryingControlChannelMonitor extends ChannelMonitor {

  private static final Logger log = LoggerFactory.getLogger(RetryingControlChannelMonitor.class);

  private final FollowCallback                   followBack = new FollowCallback();
  private final Map<ChannelId, QualifyTask>      delayed    = new ConcurrentHashMap<>();
  private final Map<ChannelId, QualifyingRecord> qualifying = new ConcurrentHashMap<>();
  private final Object                           txnLock    = new Object();

  private final WebTarget qualifyTarget;
  private final WebTarget followTarget;

  public RetryingControlChannelMonitor(P25DcodrConfig config,
                                       WebTarget      qualifyTarget,
                                       WebTarget      followTarget)
  {
    super(config);
    this.qualifyTarget = qualifyTarget;
    this.followTarget  = followTarget;
  }

  @Override
  public boolean contains(ChannelId channelId) {
    synchronized (txnLock) {
      return super.contains(channelId) ||
             delayed.containsKey(channelId) ||
             qualifying.containsKey(channelId);
    }
  }

  @Override
  public List<Identifiable> getMonitored() {
    synchronized (txnLock) {
      List<Identifiable> monitored = super.getMonitored();

      monitored.addAll(delayed.keySet()
               .stream()
               .map(key -> delayed.get(key).request)
               .collect(Collectors.toList()));

      monitored.addAll(qualifying.keySet()
               .stream()
               .map(key -> qualifying.get(key).request)
               .collect(Collectors.toList()));

      return monitored;
    }
  }

  @Override
  public void cancel(ChannelId channelId) {
    synchronized (txnLock) {
      super.cancel(channelId);

      Optional<QualifyTask>      delayedTask      = Optional.ofNullable(delayed.remove(channelId));
      Optional<QualifyingRecord> qualifyingRecord = Optional.ofNullable(qualifying.remove(channelId));

      if (delayedTask.isPresent()) {
        delayedTask.get().cancel();
      }
      if (qualifyingRecord.isPresent()) {
        qualifyingRecord.get().qualifyingFuture.cancel(true);
      }
    }
  }

  @Override
  protected void removeInactive(ChannelMonitor.MonitorRecord record) {
    synchronized (txnLock) {
      if (record.reference instanceof FollowRequest && config.getControlChannelRetryCount() > 0) {
        log.info(record.reference.getChannelId() + " verifying inactivity with /qualify");

        QualifyTask qualifyTask = new QualifyTask(
            (FollowRequest) record.reference, config.getControlChannelRetryCount()
        );

        delayed.put(record.reference.getChannelId(), qualifyTask);
        timer.schedule(qualifyTask, config.getControlChannelRetryDelayMs());
        super.removeInactive(record);
      } else {
        super.removeInactive(record);
      }
    }
  }

  private class QualifyTask extends TimerTask {
    private final FollowRequest request;
    private final Integer       retryCount;

    public QualifyTask(FollowRequest request, Integer retryCount) {
      this.request    = request;
      this.retryCount = retryCount;
    }

    private QualifyRequest translate(FollowRequest request) {
      return new QualifyRequest(
          request.getLatitude(),     request.getLongitude(),
          request.getPolarization(), request.getFrequency()
      );
    }

    private Future<ControlChannelQualities> sendRequest(QualifyRequest request, QualifyCallback callback) {
      return qualifyTarget.request().async().post(
          Entity.entity(request, MediaType.APPLICATION_JSON_TYPE), callback
      );
    }

    @Override
    public void run() {
      synchronized (txnLock) {
        QualifyCallback qualifyCallback = new QualifyCallback(request, retryCount);
        Future          qualifyFuture   = sendRequest(translate(request), qualifyCallback);

        delayed.remove(request.getChannelId());
        qualifying.put(request.getChannelId(), new QualifyingRecord(request, qualifyFuture));
      }
    }
  }

  private class QualifyCallback implements InvocationCallback<ControlChannelQualities> {
    private final FollowRequest request;
    private       Integer       retryCount;

    public QualifyCallback(FollowRequest request, Integer retryCount) {
      this.request    = request;
      this.retryCount = retryCount;
    }

    private ControlChannelId transform(ControlChannelQualities qualities) {
      return new ControlChannelId(
          qualities.getWacn(), qualities.getSystemId(),
          qualities.getRfSubsystemId(), qualities.getSiteId()
      );
    }

    @Override
    public void completed(ControlChannelQualities qualities) {
      synchronized (txnLock) {
        if (request.getChannelId().equals(transform(qualities))) {
          log.info(request.getChannelId() + " qualified, posting back to /channels/control");
          qualifying.remove(request.getChannelId());
          followTarget.request().async().post(
              Entity.entity(request, MediaType.APPLICATION_JSON_TYPE), followBack
          );
        } else {
          log.info(request.getChannelId() + " replaced by different site, will not try again");
          qualifying.remove(request.getChannelId());
        }
      }
    }

    @Override
    public void failed(Throwable throwable) {
      synchronized (txnLock) {
        if (--retryCount > 0) {
          log.info(request.getChannelId() + " not qualified, retrying " + retryCount + " more times");
          QualifyTask qualifyTask = new QualifyTask(request, retryCount);
          delayed.put(request.getChannelId(), qualifyTask);
          qualifying.remove(request.getChannelId());
          timer.schedule(qualifyTask, config.getControlChannelRetryDelayMs());
        } else {
          log.info(request.getChannelId() + " not qualified, will not try again");
          qualifying.remove(request.getChannelId());
        }
      }
    }
  }

  private static class FollowCallback implements InvocationCallback<Response> {

    @Override
    public void completed(Response response) {
      response.close();
    }

    @Override
    public void failed(Throwable error) {
      log.error("post to follow target failed", error);
    }
  }

  private static class QualifyingRecord {
    private final FollowRequest request;
    private final Future        qualifyingFuture;

    public QualifyingRecord(FollowRequest request, Future qualifyingFuture) {
      this.request          = request;
      this.qualifyingFuture = qualifyingFuture;
    }
  }

}
