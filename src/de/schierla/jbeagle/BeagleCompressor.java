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
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.jcraft.jzlib.Deflater;
import com.jcraft.jzlib.DeflaterOutputStream;
import com.jcraft.jzlib.JZlib;

public class BeagleCompressor {

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

		int[] pixels = page.getRGB(0, 0, 600, 800, null, 0, 600);
		for (int i = 0; i < 800 * 600 / 2; i++) {
			int gray1 = four_bits((pixels[2 * i] >> 16) & 0xff,
					(pixels[2 * i] >> 8) & 0xff, (pixels[2 * i]) & 0xff);
			int gray2 = four_bits((pixels[2 * i + 1] >> 16) & 0xff,
					(pixels[2 * i + 1] >> 8) & 0xff, (pixels[2 * i + 1]) & 0xff);
			raw[i] = (byte) (gray2 | gray1 * 0x10);
		}
		for (int i = 800 * 600 / 2; i < 800 * 600 / 2 + 64; i++) {
			raw[i] = 0;
		}
		return raw;
	}

	public static byte[] encodeImage(BufferedImage page) throws IOException {
		if (page.getWidth() != 600 || page.getHeight() != 800)
			throw new IllegalArgumentException("The image has to be 600x800.");
		byte[] rawBuffer = createRawBuffer(page);
		return compressBuffer(rawBuffer);
	}

	private static int four_bits(int r, int g, int b) {
		int gray = (int) (r * 0.2126 + g * 0.7152 + b * 0.0722);
		return (gray & 0xE0) / 0x10;
	}
}
