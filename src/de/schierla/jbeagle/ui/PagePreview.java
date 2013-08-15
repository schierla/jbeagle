package de.schierla.jbeagle.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class PagePreview extends JFrame {
	private static final long serialVersionUID = -6978851161012882305L;
	BufferedImage im = null;

	public PagePreview(String title) {
		super(title);
		add(new JPanel() {
			private static final long serialVersionUID = -2593570511235565561L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(600, 800);
			}

			@Override
			public void paint(Graphics g) {
				super.paint(g);
				if (im != null)
					g.drawImage(im, 0, 0, null);
			}
		});
		pack();
		setResizable(false);
		setVisible(true);
	}

	public void showPage(BufferedImage page) {
		this.im = page;
		int[] pixels = im.getRGB(0, 0, 600, 800, null, 0, 600);
		for (int i = 0; i < pixels.length; i++) {
			int gray = four_bits((pixels[i] >> 16) & 0xff,
					(pixels[i] >> 8) & 0xff, (pixels[i]) & 0xff);
			pixels[i] = gray * 0x111111 + 0xFF000000;
		}
		im.setRGB(0, 0, 600, 800, pixels, 0, 600);
		repaint();
	}

	private static int four_bits(int r, int g, int b) {
		int gray = (int) (r * 0.2126 + g * 0.7152 + b * 0.0722);
		return (gray & 0xE0) / 0x10;
	}

}
