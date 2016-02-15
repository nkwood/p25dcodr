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

package org.anhonesteffort.p25.metric;

import com.blacklocus.metrics.CloudWatchReporterBuilder;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

import java.util.concurrent.TimeUnit;

public class P25DcodrMetrics {

  private static P25DcodrMetrics instance;
  private final MetricRegistry registry;
  private final String instanceId;

  protected P25DcodrMetrics(CloudWatchConfig config, MetricRegistry registry) {
    this.registry = registry;
    instanceId    = config.getInstanceId();

    System.getProperties().setProperty("aws.accessKeyId", config.getAccessKeyId());
    System.getProperties().setProperty("aws.secretKey", config.getSecretKey());

    new CloudWatchReporterBuilder()
        .withNamespace(P25DcodrMetrics.class.getSimpleName())
        .withRegistry(registry)
        .build()
        .start(config.getReportingIntervalMinutes(), TimeUnit.MINUTES);
  }

  protected static void mock(P25DcodrMetrics mock) {
    if (instance == null) {
      instance = mock;
    }
  }

  public static void init(CloudWatchConfig config, MetricRegistry registry) {
    if (instance == null) {
      instance = new P25DcodrMetrics(config, registry);
    }
  }

  public static P25DcodrMetrics getInstance() {
    return instance;
  }

  public void chnlzrConnectSuccess() {
    registry.counter("chnlzrConnectSuccess instance=" + instanceId + "*").inc();
  }

  public void chnlzrConnectFailure() {
    registry.counter("chnlzrConnectFailure instance=" + instanceId + "*").inc();
  }

  public void chnlzrRequest(double frequency) {
    registry.histogram("chnlzrRequest instance=" + instanceId + "*").update((long)frequency);
  }

  public void chnlzrRequestSuccess() {
    registry.counter("chnlzrRequestSuccess instance=" + instanceId + "*").inc();
  }

  public void chnlzrRequestFailure() {
    registry.counter("chnlzrRequestFailure instance=" + instanceId + "*").inc();
  }

  public void chnlzrRequestDenied(int code) {
    registry.counter("chnlzrRequestDenied instance=" + instanceId + "*" + " code=" + code + "*").inc();
  }

  public void chnlzrStreamClosed(int code) {
    registry.counter("chnlzrStreamClosed instance=" + instanceId + "*" + " code=" + code + "*").inc();
  }

  public void registerChannelMonitor(Gauge<Integer> gauge) {
    registry.register("channelMonitor", gauge);
  }

  public void channelInactive() {
    registry.counter("channelInactive instance=" + instanceId + "*").inc();
  }

  public void groupCaptureRequest() {
    registry.counter("groupCaptureRequest instance=" + instanceId + "*").inc();
  }

  public void groupCaptureSuccess() {
    registry.counter("groupCaptureSuccess instance=" + instanceId + "*").inc();
  }

  public void dataUnitCorrupted() {
    registry.counter("dataUnitCorrupted instance=" + instanceId + "*").inc();
  }

  public void dataUnitIntact() {
    registry.counter("dataUnitIntact instance=" + instanceId + "*").inc();
  }

  public void kinesisRecordPutSuccess() {
    registry.counter("kinesisRecordPutSuccess instance=" + instanceId + "*").inc();
  }

  public void kinesisRecordPutFailure() {
    registry.counter("kinesisRecordPutFailure instance=" + instanceId + "*").inc();
  }

}
