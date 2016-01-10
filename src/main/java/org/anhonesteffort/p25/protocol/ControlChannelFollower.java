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
import org.anhonesteffort.p25.model.GroupChannelId;
import org.anhonesteffort.p25.monitor.DataUnitCounter;
import org.anhonesteffort.p25.model.FollowRequest;
import org.anhonesteffort.p25.model.GroupCaptureRequest;
import org.anhonesteffort.p25.protocol.frame.DataUnit;
import org.anhonesteffort.p25.protocol.frame.Duid;
import org.anhonesteffort.p25.protocol.frame.TrunkSignalDataUnit;
import org.anhonesteffort.p25.protocol.frame.tsbk.GroupVoiceChannelGrant;
import org.anhonesteffort.p25.protocol.frame.tsbk.IdUpdateBlock;
import org.anhonesteffort.p25.protocol.frame.tsbk.TrunkSignalBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.concurrent.Future;

public class ControlChannelFollower implements Sink<DataUnit>, DataUnitCounter {

  private static final Logger log = LoggerFactory.getLogger(ControlChannelFollower.class);

  private final ChannelIdUpdateBlockMap channelIdMap = new ChannelIdUpdateBlockMap();
  private final FollowRequest followRequest;
  private final WebTarget trafficTarget;

  private Integer dataUnitCount = 0;

  public ControlChannelFollower(FollowRequest followRequest, WebTarget trafficTarget) {
    this.followRequest = followRequest;
    this.trafficTarget = trafficTarget;
  }

  private Future<Response> sendRequest(Object request) {
    return trafficTarget.request().async().post(
        Entity.entity(request, MediaType.APPLICATION_JSON_TYPE)
    );
  }

  private GroupChannelId buildChannelId(GroupVoiceChannelGrant grant) {
    return new GroupChannelId(
        followRequest.getChannelId().getWacn(),
        followRequest.getChannelId().getSystemId(),
        followRequest.getChannelId().getRfSubsystemId(),
        grant.getSourceId(),
        grant.getGroupId()
    );
  }

  private GroupCaptureRequest buildCaptureRequest(GroupVoiceChannelGrant grant, Double frequency) {
    return new GroupCaptureRequest(
        followRequest.getLatitude(),     followRequest.getLongitude(),
        followRequest.getPolarization(), frequency,
        buildChannelId(grant)
    );
  }

  private void followGroupChannelGrant(GroupVoiceChannelGrant grant) {
    Integer                 channelId = grant.getChannelId();
    Optional<IdUpdateBlock> idBlock   = channelIdMap.getBlockForId(channelId);

    if (!idBlock.isPresent()) {
      log.warn(followRequest.getChannelId() + " unable to process voice channel grant, id map missing " + channelId);
    } else {
      sendRequest(buildCaptureRequest(grant, grant.getDownlinkFreq(idBlock.get())));
    }
  }

  @Override
  public void consume(DataUnit dataUnit) {
    if (!dataUnit.isIntact())
      return;

    dataUnitCount++;
    switch (dataUnit.getNid().getDuid().getId()) {
      case Duid.ID_TRUNK_SIGNALING:
        ((TrunkSignalDataUnit) dataUnit).getBlocks().forEach(block -> {
          channelIdMap.consume(block);

          switch (block.getOpCode()) {
            case TrunkSignalBlock.GROUP_VOICE_CHAN_GRANT:
              followGroupChannelGrant((GroupVoiceChannelGrant) block);
              break;
          }
        });
        break;
    }
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
