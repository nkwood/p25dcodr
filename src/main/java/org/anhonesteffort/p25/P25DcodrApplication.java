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

package org.anhonesteffort.p25;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.SimpleServerFactory;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.anhonesteffort.chnlzr.CapnpUtil;
import org.anhonesteffort.chnlzr.ChnlzrConfig;
import org.anhonesteffort.kinesis.KinesisClientFactory;
import org.anhonesteffort.p25.chnlbrkr.ChnlBrkrConnectionFactory;
import org.anhonesteffort.p25.chnlbrkr.ChnlBrkrController;
import org.anhonesteffort.p25.health.DumbCheck;
import org.anhonesteffort.p25.kinesis.KinesisRecordSenderFactory;
import org.anhonesteffort.p25.monitor.ChannelMonitor;
import org.anhonesteffort.p25.monitor.RetryingControlChannelMonitor;
import org.anhonesteffort.p25.resource.ControlChannelFollowingResource;
import org.anhonesteffort.p25.resource.ControlChannelQualifyingResource;
import org.anhonesteffort.p25.resource.TrafficChannelCaptureResource;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.anhonesteffort.chnlzr.Proto.HostId;

public class P25DcodrApplication extends Application<P25DcodrConfig> {

  private final ChnlzrConfig chnlzrConfig;

  public P25DcodrApplication() throws IOException {
    chnlzrConfig = new ChnlzrConfig();
  }

  @Override
  public String getName() {
    return "p25dcodr";
  }

  private Client buildClient(P25DcodrConfig config, Environment environment) {
    config.getJerseyConfig().setTimeout(Duration.milliseconds(
        config.getChannelRequestTimeoutMs() + config.getChannelQualifyTimeMs() + 2500l)
    );
    return new JerseyClientBuilder(environment).using(config.getJerseyConfig())
                                               .build("jersey-client");
  }

  private String getServerUri(P25DcodrConfig config) {
    SimpleServerFactory  serverFactory = (SimpleServerFactory) config.getServerFactory();
    HttpConnectorFactory connector     = (HttpConnectorFactory) serverFactory.getConnector();

    if (connector.getClass().isAssignableFrom(HttpConnectorFactory.class)) {
      return "http://localhost:" + connector.getPort();
    } else {
      return null;
    }
  }

  @Override
  public void run(P25DcodrConfig config, Environment environment) throws Exception {
    EventLoopGroup           nettyPool   = new NioEventLoopGroup();
    ListeningExecutorService dspPool     = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(config.getDspPoolSize()));
    ExecutorService          kinesisPool = Executors.newFixedThreadPool(config.getKinesis().getSenderPoolSize());

    Client    jerseyClient  = buildClient(config, environment);
    String    serverUri     = getServerUri(config);
    WebTarget qualifyTarget = jerseyClient.target(serverUri).path("qualify");
    WebTarget followTarget  = jerseyClient.target(serverUri).path("channels/control");
    WebTarget trafficTarget = jerseyClient.target(serverUri).path("channels/traffic/group");

    ChnlBrkrConnectionFactory brkrConnections = new ChnlBrkrConnectionFactory(chnlzrConfig, NioSocketChannel.class, nettyPool);
    HostId.Reader             brkrHost        = CapnpUtil.hostId(config.getBrkrHostname(), config.getBrkrPort());
    ChnlBrkrController        chnlBrkr        = new ChnlBrkrController(brkrHost, brkrConnections);
    ChannelMonitor            channelMonitor  = new RetryingControlChannelMonitor(config, qualifyTarget, followTarget);

    KinesisClientFactory       kinesisClients = new KinesisClientFactory(config.getKinesis(), kinesisPool);
    KinesisRecordSenderFactory kinesisSenders = new KinesisRecordSenderFactory(config.getKinesis(), kinesisClients);

    environment.healthChecks().register("dumb", new DumbCheck());
    environment.jersey().register(new ControlChannelQualifyingResource(config, chnlBrkr, dspPool));
    environment.jersey().register(new ControlChannelFollowingResource(config, chnlBrkr, channelMonitor, kinesisSenders, trafficTarget, dspPool));
    environment.jersey().register(new TrafficChannelCaptureResource(config, chnlBrkr, channelMonitor, kinesisSenders, dspPool));
  }

  public static void main(String[] args) throws Exception {
    new P25DcodrApplication().run(args);
  }

}
