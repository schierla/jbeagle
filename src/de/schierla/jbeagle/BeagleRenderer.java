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
import java.util.ArrayList;
import java.util.List;

import org.jpedal.PdfDecoder;
import org.jpedal.exception.PdfException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class BeagleRenderer {

	private PdfDecoder decoder;
	private String author;
	private String title;
	private int pages;
	private List<Integer> bookmarks;

	public BeagleRenderer(PdfDecoder decoder, String author, String title) {
		this.decoder = decoder;
		this.author = author;
		this.title = title;
		this.pages = decoder.getPageCount();
		this.bookmarks = extractBookmarks(decoder);
	}

	public int getPageCount() {
		return this.pages;
	}

	/**
	 * Renders the specified page
	 * 
	 * @param nr
	 *            number of page to render (0-based)
	 * @param titelPage
	 *            draw the given page as title page
	 * @return Image of the page
	 */
	public BufferedImage render(int nr, boolean titlePage) {
		BufferedImage im = new BufferedImage(600, 800,
				BufferedImage.TYPE_4BYTE_ABGR);
		try {
			decoder.setPageParameters(1, -1);
			BufferedImage page = decoder.getPageAsImage(nr + 1);
			float scaleX = 600f / page.getWidth();
			float scaleY = 800f / page.getHeight();
			decoder.setPageParameters(Math.min(scaleX, scaleY), -1);
			page = decoder.getPageAsImage(nr + 1);
			if (titlePage) {
				drawTitlePage(author, title, page, im.getGraphics());
			} else {
				drawBookPage((pages - 1), nr, page, im.getGraphics(), bookmarks);
			}
		} catch (PdfException e) {
			e.printStackTrace();
		}
		return im;
	}

	private List<Integer> extractBookmarks(PdfDecoder decoder) {
		Document outline = decoder.getOutlineAsXML();
		final List<Integer> bookmarks = new ArrayList<Integer>();
		if (outline != null) {
			NodeList titles = outline.getElementsByTagName("title");
			for (int i = 0; i < titles.getLength(); i++) {
				Node pageAttribute = titles.item(i).getAttributes()
						.getNamedItem("page");
				if (pageAttribute != null) {
					bookmarks.add(Integer.parseInt(pageAttribute
							.getTextContent()));
				}
			}
		}
		return bookmarks;
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
			Graphics output, List<Integer> bookmarks) {
		output.setColor(Color.white);
		output.fillRect(0, 0, 600, 800);
		float width = page.getWidth(), height = page.getHeight();
		float scaleX = 600 / width, scaleY = 800 / height;
		float scale = Math.min(scaleX, scaleY);
		width = width * scale;
		height = height * scale;
		float x = (600 - width) / 2, y = (800 - height) / 2;

		output.drawImage(page, (int) x, (int) y, (int) width, (int) height,
				null);
		output.setColor(Color.GRAY);
		output.fillRect(0, 795, (600 * i / pages), 5);
		output.setColor(Color.BLACK);
		for (Integer nr : bookmarks) {
			output.drawLine(600 * (nr - 1) / pages, 797,
					600 * (nr - 1) / pages, 799);
		}
	}
}
