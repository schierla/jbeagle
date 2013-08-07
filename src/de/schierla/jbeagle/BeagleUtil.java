/*
 * jBeagle - application for managing the txtr beagle
 * Copyright 2013 Andreas Schierl
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
package de.schierla.jbeagle;

import java.io.IOException;

import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

/**
 * Helper class for bluetooth device search
 */
public class BeagleUtil {

	/**
	 * Try to connect to a txtr beagle
	 * 
	 * @return the beagle, if found; null, if not found
	 * @throws IOException
	 *             if an error occurs
	 */
	public static Beagle searchForBeagle() throws IOException {
		DiscoveryAgent da = LocalDevice.getLocalDevice().getDiscoveryAgent();
		RemoteDevice[] devices = da.retrieveDevices(DiscoveryAgent.PREKNOWN);
		for (RemoteDevice r : devices) {
			if ("Beagle".equals(r.getFriendlyName(false))) {
				try {
					StreamConnection connection = (StreamConnection) Connector
							.open("btspp://"
									+ r.getBluetoothAddress()
									+ ":1;authenticate=false;encrypt=false;master=false");
					return new Beagle(connection);
				} catch (IOException e) {
				}
			}
		}
		return null;
	}

}
