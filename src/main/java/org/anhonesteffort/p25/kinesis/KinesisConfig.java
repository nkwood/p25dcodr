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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

public class KinesisConfig {

  @NotEmpty private String region;
  @NotEmpty private String stream;
  @NotEmpty private String accessKeyId;
  @NotEmpty private String secretKey;
  @NotEmpty private String appName;
  @NotEmpty private String appVersion;

  public KinesisConfig() { }

  @JsonProperty
  public String getRegion() {
    return region;
  }

  @JsonProperty
  public String getStream() {
    return stream;
  }

  @JsonProperty
  public String getAccessKeyId() {
    return accessKeyId;
  }

  @JsonProperty
  public String getSecretKey() {
    return secretKey;
  }

  @JsonProperty
  public String getAppName() {
    return appName;
  }

  @JsonProperty
  public String getAppVersion() {
    return appVersion;
  }

}
