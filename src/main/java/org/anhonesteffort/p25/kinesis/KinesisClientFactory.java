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

package org.anhonesteffort.p25.kinesis;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;

import java.util.Optional;

public class KinesisClientFactory {

  private final KinesisConfig       config;
  private final AWSCredentials      credentials;
  private final ClientConfiguration clientConfig;

  public KinesisClientFactory(KinesisConfig config) {
    this.config  = config;
    credentials  = new BasicAWSCredentials(config.getAccessKeyId(), config.getSecretKey());
    clientConfig = clientConfig();
  }

  private ClientConfiguration clientConfig() {
    ClientConfiguration clientConfig = new ClientConfiguration();
    StringBuilder       userAgent    = new StringBuilder(ClientConfiguration.DEFAULT_USER_AGENT);

    userAgent.append(" ");
    userAgent.append(config.getAppName());
    userAgent.append(" ");
    userAgent.append(config.getAppVersion());

    clientConfig.setUserAgent(userAgent.toString());
    clientConfig.setMaxConnections(1);

    /*
    todo:
      play with timeouts, retry policy, gzip, keep-alive
     */

    return clientConfig;
  }

  public Optional<AmazonKinesis> get() {
    AmazonKinesis client = new AmazonKinesisClient(credentials, clientConfig);
    Region        region = RegionUtils.getRegion(config.getRegion());

    if (region == null) {
      return Optional.empty();
    } else {
      client.setRegion(region);
      return Optional.of(client);
    }
  }

}
