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
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import javax.microedition.io.StreamConnection;

import com.jcraft.jzlib.Deflater;
import com.jcraft.jzlib.DeflaterOutputStream;
import com.jcraft.jzlib.JZlib;

/**
 * Class handling the communication with the txtr beagle
 */
public class Beagle {

	private static byte[] compressBuffer(byte[] rawBuffer) throws IOException {
		Deflater deflater = new Deflater();
		deflater.init(JZlib.Z_DEFAULT_COMPRESSION, 27, 9);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DeflaterOutputStream dos = new DeflaterOutputStream(out, deflater);
		dos.write(rawBuffer);
		dos.flush();
		dos.close();
		return out.toByteArray();
	}

	private static byte[] createRawBuffer(BufferedImage page) {
		byte[] raw = new byte[800 * 600 / 2 + 64];
		for (int y = 0; y < 800; y++) {
			for (int x = 0; x < 600; x += 2) {
				raw[y * 300 + x / 2] = (byte) (four_bits(page.getRGB(x + 1, y)) | four_bits(page
						.getRGB(x, y)) * 0x10);
			}
		}
		for (int i = 800 * 600 / 2; i < 800 * 600 / 2 + 64; i++) {
			raw[i] = -127;
		}
		return raw;
	}

	private static byte[] encodeImage(BufferedImage page) throws IOException {
		if (page.getWidth() != 600 || page.getHeight() != 800)
			throw new IllegalArgumentException("The image has to be 600x800.");
		byte[] rawBuffer = createRawBuffer(page);
		return compressBuffer(rawBuffer);
	}

	private static int four_bits(int rgb) {
		int r = (rgb & 0x00ff0000) / 0x00010000;
		int g = (rgb & 0x0000ff00) / 0x00000100;
		int b = (rgb & 0x000000ff) / 0x00000001;
		int gray = (int) (r * 0.2126 + g * 0.7152 + b * 0.0722);
		return (gray & 0xE0) / 0x10;
	}

	private BufferedReader input;

	private OutputStreamWriter output;

	private OutputStream outputStream;

	public Beagle(StreamConnection connection) throws IOException {
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

	/**
	 * Uploads a book with the given pages
	 * 
	 * @param id
	 *            book id (16 bytes, hex)
	 * @param title
	 *            book title
	 * @param author
	 *            book author
	 * @param pages
	 *            number of pages to upload
	 * @param queue
	 *            pages of the book (the first is used on the book selection
	 *            screen)
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void uploadBook(String id, String title, String author, int pages,
			BlockingQueue<BufferedImage> queue) throws IOException,
			InterruptedException {
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
		;
		if (!"AUTHOROK".equals(line = read()))
			throw new IOException("Invalid response " + line);

		for (int i = 0; i < pages; i++) {
			write("PAGE " + i);
			write(encodeImage(queue.take()));
			if (!"PAGEOK".equals(line = read()))
				throw new IOException("Invalid response " + line);
		}
		write("ENDBOOK");
		if (!"ENDBOOKOK".equals(line = read()))
			throw new IOException("Invalid response " + line);
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
