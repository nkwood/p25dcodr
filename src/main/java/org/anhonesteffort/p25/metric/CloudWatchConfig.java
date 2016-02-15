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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class CloudWatchConfig {

  @NotEmpty        private String  accessKeyId;
  @NotEmpty        private String  secretKey;
  @NotEmpty        private String  instanceId;
  @NotNull @Min(1) private Integer reportingIntervalMinutes;

  public CloudWatchConfig() { }

  @JsonProperty
  public String getAccessKeyId() {
    return accessKeyId;
  }

  @JsonProperty
  public String getSecretKey() {
    return secretKey;
  }

  @JsonProperty
  public String getInstanceId() {
    return instanceId;
  }

  @JsonProperty
  public Integer getReportingIntervalMinutes() {
    return reportingIntervalMinutes;
  }

}
