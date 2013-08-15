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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import org.jpedal.PdfDecoder;
import org.jpedal.exception.PdfException;
import org.jpedal.objects.PdfFileInformation;

import de.schierla.jbeagle.ui.PagePreview;

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
	public static BeagleConnector searchForBeagle() throws IOException {
		DiscoveryAgent da = LocalDevice.getLocalDevice().getDiscoveryAgent();
		RemoteDevice[] devices = da.retrieveDevices(DiscoveryAgent.PREKNOWN);
		if (devices == null)
			return null;
		for (RemoteDevice r : devices) {
			if ("Beagle".equals(r.getFriendlyName(false))) {
				try {
					StreamConnection connection = (StreamConnection) Connector
							.open("btspp://"
									+ r.getBluetoothAddress()
									+ ":1;authenticate=false;encrypt=false;master=false");
					return new BeagleConnector(connection);
				} catch (IOException e) {
				}
			}
		}
		return null;
	}

	/**
	 * Progress listener for PDF upload
	 */
	public interface ProgressListener {
		/**
		 * Called when a page has been prepared for the beagle
		 * 
		 * @param page
		 *            current page number
		 * @param count
		 *            total page count
		 */
		void progressChanged(int page, int count);
	}

	/**
	 * Uploads a pdf document to the txtr beagle
	 * 
	 * @param beagle
	 *            the beagle to upload to
	 * @param file
	 *            file to upload
	 * @param progress
	 *            progress listener (may be null)
	 * @throws IOException
	 *             if an error occurs
	 */
	public static void uploadPDF(BeagleConnector beagle, File file,
			final ProgressListener progress) throws IOException {
		try {
			final PdfDecoder decoder = new PdfDecoder();
			decoder.openPdfFile(file.getAbsolutePath());

			String name = file.getName().replace('_', ' ');
			if (name.toLowerCase().endsWith(".pdf"))
				name = name.substring(0, name.length() - 4);

			final String author = getMetadata(decoder, "Author", "No Author");
			final String title = getMetadata(decoder, "Title", name);
			final int pages = decoder.getPageCount();

			final long id = (((long) Math.abs(author.hashCode())) << 16)
					+ Math.abs(title.hashCode());
			String uuid = Long.toHexString(Math.abs(id)).toUpperCase();

			final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<byte[]>(
					3);
			final PagePreview preview = null; // new PagePreview(title);
			new Thread(new Runnable() {
				@Override
				public void run() {
					BeagleRenderer renderer = new BeagleRenderer(decoder,
							author, title);
					try {
						queue.put(BeagleCompressor.encodeImage(renderer.render(
								0, true)));
						for (int i = 0; i < pages; i++) {
							BufferedImage image = renderer.render(i, false);
							if (preview != null)
								preview.showPage(image);
							queue.put(BeagleCompressor.encodeImage(image));
						}
					} catch (InterruptedException | IOException e) {
						e.printStackTrace();
					}
				}
			}).start();

			beagle.uploadBook(uuid, title, author);
			for (int i = 0; i <= pages; i++) {
				beagle.uploadPage(i, queue.take());
				progress.progressChanged(i, pages);
			}
			beagle.endBook();

		} catch (PdfException e) {
			throw new IOException(e);
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	}

	private static String getMetadata(PdfDecoder decoder, String key,
			String defaultValue) {
		String[] fieldNames = PdfFileInformation.getFieldNames();
		String[] metadata = decoder.getFileInformationData().getFieldValues();

		for (int i = 0; i < fieldNames.length; i++) {
			if (key.equals(fieldNames[i])) {
				String value = metadata[i];
				if (value == null || value.isEmpty())
					return defaultValue;
				return value;
			}
		}
		return defaultValue;
	}

}
