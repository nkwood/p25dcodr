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
import org.anhonesteffort.p25.model.ControlChannelQualities;
import org.anhonesteffort.p25.protocol.frame.DataUnit;
import org.anhonesteffort.p25.protocol.frame.Duid;
import org.anhonesteffort.p25.protocol.frame.TrunkSignalDataUnit;
import org.anhonesteffort.p25.protocol.frame.tsbk.IdUpdateBlock;
import org.anhonesteffort.p25.protocol.frame.tsbk.NetworkStatusBroadcastMessage;
import org.anhonesteffort.p25.protocol.frame.tsbk.RfssStatusBroadcastMessage;
import org.anhonesteffort.p25.protocol.frame.tsbk.TrunkSignalBlock;

import java.util.Optional;

public class ControlChannelQualifier implements Sink<DataUnit> {

  private final ChannelIdUpdateBlockMap channelIdMap = new ChannelIdUpdateBlockMap();

  private Optional<RfssStatusBroadcastMessage> status = Optional.empty();
  private Optional<Double> frequency = Optional.empty();
  private Optional<Integer> wacn = Optional.empty();
  private Integer dataUnitCount = 0;

  private void processSystemStatus(NetworkStatusBroadcastMessage statusMessage) {
    Optional<IdUpdateBlock> idUpdate = channelIdMap.getBlockForId(statusMessage.getChannelId());
                            wacn     = Optional.of(statusMessage.getWacn());

    if (idUpdate.isPresent()) {
      frequency = Optional.of(statusMessage.getDownlinkFreq(idUpdate.get()));
    }
  }

  private void processSiteStatus(RfssStatusBroadcastMessage statusMessage) {
    Optional<IdUpdateBlock> idUpdate = channelIdMap.getBlockForId(statusMessage.getChannelId());
                            status   = Optional.of(statusMessage);

    if (idUpdate.isPresent()) {
      frequency = Optional.of(statusMessage.getDownlinkFreq(idUpdate.get()));
    }
  }

  @Override
  public void consume(DataUnit dataUnit) {
    if (!dataUnit.isIntact()) {
      return;
    }

    dataUnitCount++;
    if (dataUnit.getNid().getDuid().getId() == Duid.ID_TRUNK_SIGNALING) {
      TrunkSignalDataUnit        trunkSignal  = (TrunkSignalDataUnit) dataUnit;
      Optional<TrunkSignalBlock> systemStatus = trunkSignal.getFirstOf(TrunkSignalBlock.NETWORK_STATUS);
      Optional<TrunkSignalBlock> siteStatus   = trunkSignal.getFirstOf(TrunkSignalBlock.RFSS_STATUS_BROADCAST);

      trunkSignal.getBlocks().forEach(channelIdMap::consume);

      if (systemStatus.isPresent()) {
        processSystemStatus((NetworkStatusBroadcastMessage) systemStatus.get());
      }
      if (siteStatus.isPresent()) {
        processSiteStatus((RfssStatusBroadcastMessage) siteStatus.get());
      }
    }
  }

  public Optional<ControlChannelQualities> getQualities() {
    if (wacn.isPresent() && status.isPresent() && frequency.isPresent()) {
      return Optional.of(new ControlChannelQualities(
          wacn.get(), status.get(), frequency.get(), dataUnitCount
      ));
    } else {
      return Optional.empty();
    }
  }

}
