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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.io.StreamConnection;

/**
 * Class handling the communication with the txtr beagle
 */
public class BeagleConnector {

	private BufferedReader input;

	private OutputStreamWriter output;

	private OutputStream outputStream;

	public BeagleConnector(StreamConnection connection) throws IOException {
		input = new BufferedReader(new InputStreamReader(
				connection.openInputStream()));
		outputStream = connection.openOutputStream();
		output = new OutputStreamWriter(outputStream);
	}

	/**
	 * Deletes the book with the given ID
	 * 
	 * @param id
	 *            book id (16 bytes hex)
	 * @throws IOException
	 */
	public void deleteBook(String id) throws IOException {
		write("DELETEBOOK ID=" + id);
		String line = read();
		if ("DELETEBOOKERROR".equals(line))
			throw new IOException("Could not delete book");
		if (!"DELETEBOOKOK".equals(line))
			throw new IOException("Invalid response " + line);
	}

	/**
	 * Retrieves information about the beagle
	 * 
	 * @return information about the beagle (FIRMWARE.BUILDDATE, FIRMWARE.GIT,
	 *         FIRMWARE.IAP, FIRMWARE.ID, FIRMWARE.BLUETOOTH DEVICE.BDADDR,
	 *         DEVICE.SERIAL, DEVICE.DISPLAY, PROTOCOL.VERSION,
	 *         SDCONTENT.REVISION, OPTION.LOWFLASH, OPTION.FFTBT, VCOM.VALUE)
	 * @throws IOException
	 */
	public Map<String, String> getInfo() throws IOException {
		Map<String, String> ret = new HashMap<String, String>();
		write("INFO");
		String line;
		while (!"INFOOK".equals(line = read())) {
			String[] parts = line.split(" ");
			if (line.startsWith("#"))
				continue;
			for (int i = 1; i < parts.length; i++) {
				String key = parts[i].substring(0, parts[i].indexOf("="));
				String value = parts[i].substring(parts[i].indexOf("=") + 1);
				ret.put(parts[0] + "." + key, value);
			}
		}
		return ret;
	}

	/**
	 * Retrieves the partner ID of the beagle
	 * 
	 * @return partner ID (16 bytes, hex)
	 * @throws IOException
	 */
	public String getPartnerId() throws IOException {
		write("GETPARTNER");
		String line = read();
		if ("NOPARTNER".equals(line))
			return null;
		if (!line.startsWith("PARTNER ID="))
			throw new IOException("Invalid response " + line);
		return line.substring(line.indexOf("=") + 1);
	}

	/**
	 * Lists the books present on the beagle
	 * 
	 * @return list of books present
	 * @throws IOException
	 */
	public List<BeagleBook> listBooks() throws IOException {
		List<BeagleBook> ret = new ArrayList<BeagleBook>();

		write("GETBOOKS");
		for (String line = read(); line != null && !"GETBOOKSOK".equals(line); line = read()) {
			String[] parts = line.split(" ");
			if (!"BOOK".equals(parts[0]))
				throw new IOException("Invalid response " + line);

			BeagleBook b = new BeagleBook();
			for (int i = 1; i < parts.length; i++) {
				String part = parts[i];
				int index = part.indexOf("=");
				if (index == -1)
					throw new IOException("Invalid part " + part);
				String key = part.substring(0, index);
				String value = part.substring(index + 1);

				if ("ID".equals(key)) {
					b.setId(value);
				} else if ("FIRSTPAGE".equals(key)) {
					b.setFirstPage(Integer.parseInt(value));
				} else if ("LASTPAGE".equals(key)) {
					b.setLastPage(Integer.parseInt(value));
				} else if ("CURRENTPAGE".equals(key)) {
					b.setCurrentPage(Integer.parseInt(value));
				} else if ("AUTHOR".equals(key)) {
					b.setAuthorBase64(value);
				} else if ("TITLE".equals(key)) {
					b.setTitleBase64(value);
				}
			}
			ret.add(b);
		}
		return ret;
	}

	private String read() throws IOException {
		String line = input.readLine();
		// System.out.println("<< " + line);
		return line;
	}

	/**
	 * Sets the partner ID of the beagle
	 * 
	 * @param partner
	 *            partner id (16 bytes, hex)
	 * @throws IOException
	 */
	public void setPartnerId(String partner) throws IOException {
		write("PARTNER ID=" + partner);
		String line = read();
		if (!"PARTNEROK".equals(line))
			throw new IOException("Invalid response " + line);
	}

	public interface PageSource {
		byte[] getCompressedPage(int nr);
	}

	/**
	 * Starts the upload of a book (upload pages using
	 * {@link #uploadPage(int, byte[])}, conclude the upload using
	 * {@link #endBook()})
	 * 
	 * @param id
	 *            book id (16 bytes, hex)
	 * @param title
	 *            book title
	 * @param author
	 *            book author
	 * @throws IOException
	 */
	public void uploadBook(String id, String title, String author)
			throws IOException {

		String line;
		BeagleBook book = new BeagleBook();
		book.setId(id);
		book.setTitle(title);
		book.setAuthor(author);

		write("BOOK ID=" + book.getId());
		if (!"BOOKOK".equals(line = read()))
			throw new IOException("Invalid response " + line);
		write("TITLE " + book.getTitleBase64());
		if (!"TITLEOK".equals(line = read()))
			throw new IOException("Invalid response " + line);
		write("AUTHOR " + book.getAuthorBase64());

		if (!"AUTHOROK".equals(line = read()))
			throw new IOException("Invalid response " + line);
		uploadingBook = true;
	}

	boolean uploadingBook = false;

	/**
	 * Uploads a page for the current book
	 * 
	 * @param nr
	 *            page number (0-based)
	 * @param compressedImage
	 *            compressed image to upload as page
	 * @throws IOException
	 */
	public void uploadPage(int nr, byte[] compressedImage) throws IOException {
		if (uploadingBook) {
			String line;
			write("PAGE " + nr);
			write(compressedImage);

			if (!"PAGEOK".equals(line = read()))
				throw new IOException("Invalid response " + line);

		}
	}

	/**
	 * Uploads a utility page
	 * 
	 * @param nr
	 *            page number (0-based)
	 * @param compressedImage
	 *            compressed image to upload as page
	 * @throws IOException
	 */
	public void uploadUtilityPage(int nr, byte[] compressedImage)
			throws IOException {
		String line;
		write("UTILITYPAGE " + nr);
		write(compressedImage);

		if (!"PAGEOK".equals(line = read()))
			throw new IOException("Invalid response " + line);

	}

	/**
	 * Conclude the book upload
	 * 
	 * @throws IOException
	 */
	public void endBook() throws IOException {
		if (uploadingBook) {
			uploadingBook = false;
			write("ENDBOOK");
			String line;
			if (!"ENDBOOKOK".equals(line = read()))
				throw new IOException("Invalid response " + line);
		}
	}

	/**
	 * Performs a factory reset on the beagle (deletes all books)
	 * 
	 * @throws IOException
	 */
	public void virgin() throws IOException {
		write("VIRGIN");
		String line = read();
		if (!"VIRGINOK".equals(line))
			throw new IOException("Invalid response " + line);
	}

	private void write(byte[] binaryData) throws IOException {
		// System.out.println(">> " + Arrays.toString(binaryData));
		outputStream.write(binaryData);
		outputStream.flush();
	}

	private void write(String string) throws IOException {
		// System.out.println(">> " + string);
		output.write(string + "\n");
		output.flush();
	}

}
