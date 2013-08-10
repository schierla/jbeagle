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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.jpedal.PdfDecoder;
import org.jpedal.exception.PdfException;
import org.jpedal.objects.PdfFileInformation;

/**
 * Helper class for uploading PDF documents to the txtr beagle
 */
public class BeaglePDF {

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
	public static void uploadPDF(Beagle beagle, File file,
			final ProgressListener progress) throws IOException {
		try {
			final PdfDecoder decoder = new PdfDecoder();
			decoder.openPdfFile(file.getAbsolutePath());

			Map<String, String> metadata = new HashMap<String, String>();
			decoder.getFileInformationData();
			for (int i = 0; i < PdfFileInformation.getFieldNames().length; i++) {
				metadata.put(PdfFileInformation.getFieldNames()[i], decoder
						.getFileInformationData().getFieldValues()[i]);
			}
			int pc = decoder.getPageCount();
			// if(pc > 100) pc = 100;
			final int pages = pc;

			String a = metadata.get("Author");
			if (a == null || a.isEmpty())
				a = "No author";
			final String author = a;

			String name = file.getName();
			if (name.toLowerCase().endsWith(".pdf"))
				name = name.substring(0, name.length() - 4);
			name = name.replace('_', ' ');
			String t = metadata.get("Title");
			if (t == null || t.isEmpty())
				t = name;
			final String title = t;

			long id = (((long) Math.abs(author.hashCode())) << 16)
					+ Math.abs(title.hashCode());
			final BlockingQueue<BufferedImage> queue = new ArrayBlockingQueue<BufferedImage>(
					5);

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						decoder.setPageParameters(5, -1);

						for (int i = 1; i <= pages; i++) {
							BufferedImage page = decoder.getPageAsImage(i);
							BufferedImage im = new BufferedImage(600, 800,
									BufferedImage.TYPE_4BYTE_ABGR);
							Graphics graphics = im.getGraphics();
							if (i == 1) {
								drawTitlePage(author, title, page, graphics);
							} else {
								drawBookPage((pages - 1), (i - 1), page,
										graphics);
							}
							queue.put(im);
							if (progress != null)
								progress.progressChanged(i, pages);
						}
					} catch (PdfException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}).start();
			String uuid = Long.toHexString(Math.abs(id)).toUpperCase();
			beagle.uploadBook(uuid, title, author, pages, queue);
		} catch (InterruptedException e) {
			throw new IOException(e);
		} catch (PdfException e) {
			throw new IOException(e);
		}
	}

	private static void drawTitlePage(final String author, final String title,
			BufferedImage page, Graphics output) {
		output.setColor(Color.white);
		output.fillRect(0, 0, 600, 800);
		output.setColor(Color.black);
		output.drawImage(page, 150, 200, 300, 400, null);
		output.setFont(new Font("SansSerif", Font.BOLD, 30));
		Rectangle2D bounds = output.getFontMetrics().getStringBounds(title,
				output);
		output.drawString(title, (int) (600 - bounds.getWidth()) / 2,
				800 - 5 - (int) bounds.getMaxY());
		bounds = output.getFontMetrics().getStringBounds(author, output);
		output.drawString(author, (int) (600 - bounds.getWidth()) / 2,
				5 - (int) bounds.getMinY());
	}

	private static void drawBookPage(int pages, int i, BufferedImage page,
			Graphics output) {
		output.drawImage(page, 0, 0, 600, 800, null);
		output.setColor(Color.GRAY);
		output.fillRect(0, 795, (600 * i / pages), 5);
	}
}
