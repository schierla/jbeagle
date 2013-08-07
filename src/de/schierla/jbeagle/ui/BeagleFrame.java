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
package de.schierla.jbeagle.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import de.schierla.jbeagle.Beagle;
import de.schierla.jbeagle.BeagleBook;
import de.schierla.jbeagle.BeaglePDF;
import de.schierla.jbeagle.BeaglePDF.ProgressListener;
import de.schierla.jbeagle.BeagleUtil;

public class BeagleFrame extends JFrame {
	private static final long serialVersionUID = -3803590812601005122L;
	JLabel title, author, page, id;
	JButton upload, delete;
	BeagleBook book;
	private JList<BeagleBook> books;
	private DefaultListModel<BeagleBook> beagleBooks = new DefaultListModel<BeagleBook>();
	private Beagle beagle;
	private JLabel progress;
	private JPanel panel;

	ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES,
			new ArrayBlockingQueue<Runnable>(10));
	private JScrollPane scrollPane;
	private JPanel detail;

	public BeagleFrame() {
		setTitle("jBeagle");
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		getContentPane().setLayout(new BorderLayout(0, 0));

		panel = new JPanel();
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));

		progress = new JLabel("Loading...");
		panel.add(progress);

		books = new JList<BeagleBook>(beagleBooks);
		books.setBorder(new EmptyBorder(5, 5, 5, 5));
		books.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				if (books.getSelectedIndex() == -1)
					book = null;
				else
					book = beagleBooks.get(books.getSelectedIndex());

				selectBook(book);
			}
		});
		scrollPane = new JScrollPane(books);
		scrollPane.setPreferredSize(new Dimension(500, 150));
		scrollPane
				.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		detail = new JPanel();
		detail.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(detail, BorderLayout.SOUTH);
		GridBagLayout gbl_detail = new GridBagLayout();
		gbl_detail.columnWidths = new int[] { 0, 0 };
		gbl_detail.rowHeights = new int[] { 0, 0, 0, 0, 0, 0 };
		gbl_detail.columnWeights = new double[] { 1.0, 1.0 };
		gbl_detail.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0,
				Double.MIN_VALUE };
		detail.setLayout(gbl_detail);
		JLabel label = new JLabel("ID: ");
		GridBagConstraints gbc_label = new GridBagConstraints();
		gbc_label.fill = GridBagConstraints.HORIZONTAL;
		gbc_label.insets = new Insets(0, 0, 5, 5);
		gbc_label.gridx = 0;
		gbc_label.gridy = 0;
		detail.add(label, gbc_label);

		id = new JLabel();
		id.setText("<id>");
		GridBagConstraints gbc_id = new GridBagConstraints();
		gbc_id.fill = GridBagConstraints.HORIZONTAL;
		gbc_id.insets = new Insets(0, 0, 5, 5);
		gbc_id.gridx = 1;
		gbc_id.gridy = 0;
		detail.add(id, gbc_id);
		JLabel label_1 = new JLabel("Title: ");
		GridBagConstraints gbc_label_1 = new GridBagConstraints();
		gbc_label_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_label_1.insets = new Insets(0, 0, 5, 5);
		gbc_label_1.gridx = 0;
		gbc_label_1.gridy = 1;
		detail.add(label_1, gbc_label_1);
		title = new JLabel("<title>");
		GridBagConstraints gbc_title = new GridBagConstraints();
		gbc_title.fill = GridBagConstraints.HORIZONTAL;
		gbc_title.insets = new Insets(0, 0, 5, 5);
		gbc_title.gridx = 1;
		gbc_title.gridy = 1;
		detail.add(title, gbc_title);
		JLabel label_2 = new JLabel("Author: ");
		GridBagConstraints gbc_label_2 = new GridBagConstraints();
		gbc_label_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_label_2.insets = new Insets(0, 0, 5, 5);
		gbc_label_2.gridx = 0;
		gbc_label_2.gridy = 2;
		detail.add(label_2, gbc_label_2);
		author = new JLabel("<author>");
		GridBagConstraints gbc_author = new GridBagConstraints();
		gbc_author.fill = GridBagConstraints.HORIZONTAL;
		gbc_author.insets = new Insets(0, 0, 5, 5);
		gbc_author.gridx = 1;
		gbc_author.gridy = 2;
		detail.add(author, gbc_author);
		JLabel label_3 = new JLabel("Page: ");
		GridBagConstraints gbc_label_3 = new GridBagConstraints();
		gbc_label_3.fill = GridBagConstraints.HORIZONTAL;
		gbc_label_3.insets = new Insets(0, 0, 5, 5);
		gbc_label_3.gridx = 0;
		gbc_label_3.gridy = 3;
		detail.add(label_3, gbc_label_3);
		page = new JLabel("<page>");
		GridBagConstraints gbc_page = new GridBagConstraints();
		gbc_page.fill = GridBagConstraints.HORIZONTAL;
		gbc_page.insets = new Insets(0, 0, 5, 5);
		gbc_page.gridx = 1;
		gbc_page.gridy = 3;
		detail.add(page, gbc_page);
		upload = new JButton("Upload");
		GridBagConstraints gbc_upload = new GridBagConstraints();
		gbc_upload.fill = GridBagConstraints.HORIZONTAL;
		gbc_upload.insets = new Insets(0, 0, 0, 5);
		gbc_upload.gridx = 0;
		gbc_upload.gridy = 4;
		detail.add(upload, gbc_upload);
		delete = new JButton("Delete");
		GridBagConstraints gbc_delete = new GridBagConstraints();
		gbc_delete.fill = GridBagConstraints.HORIZONTAL;
		gbc_delete.insets = new Insets(0, 0, 0, 5);
		gbc_delete.gridx = 1;
		gbc_delete.gridy = 4;
		detail.add(delete, gbc_delete);

		upload.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				uploadBook();
			}
		});
		delete.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				deleteBook();
			}
		});

		selectBook(null);

		pack();
		setVisible(true);

		showProgress("Searching for beagle...");
		searchForBeagleAsync();
	}

	private void searchForBeagleAsync() {
		pool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					while (beagle == null) {
						beagle = BeagleUtil.searchForBeagle();
					}
					if (beagle.getPartnerId() == null) {
						showProgress("Setting partner id...");
						beagle.setPartnerId(Long.toHexString(
								new Random().nextLong()).toUpperCase());
					}
					updateBooks();
				} catch (IOException e) {
					showProgress("Error: " + e.getMessage());
				}
			}
		});
	}

	protected void uploadBook() {
		if (beagle == null)
			return;
		final JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new FileFilter() {
			@Override
			public String getDescription() {
				return "PDF documents";
			}

			@Override
			public boolean accept(File file) {
				return file.isDirectory()
						|| file.getName().toLowerCase().endsWith(".pdf");
			}
		});

		if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(this)) {
			uploadPDFAsync(fc.getSelectedFile());
		}
	}

	private void uploadPDFAsync(final File file) {
		showProgress("Uploading book...");
		pool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					BeaglePDF.uploadPDF(beagle, file, new ProgressListener() {
						@Override
						public void progressChanged(int page, int count) {
							showProgress("Uploading page " + page + " of "
									+ count + "...");
						}
					});
					updateBooks();
				} catch (IOException ex) {
					showProgress("Error: " + ex.getMessage());
					ex.printStackTrace();
				}
			}
		});
	}

	protected void deleteBook() {
		if (beagle == null)
			return;
		if (book == null)
			return;
		deleteBookAsync(book.getId());
	}

	private void deleteBookAsync(final String id) {
		showProgress("Deleting book...");

		pool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					beagle.deleteBook(id);
					updateBooks();
				} catch (IOException e) {
					showProgress("Error: " + e.getMessage());
					e.printStackTrace();
				}
			}
		});
	}

	private void updateBooks() {
		try {
			showProgress("Retrieving books...");
			setBooks(beagle.listBooks());
			showProgress("Done");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void showProgress(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				progress.setText(text);
			}
		});
	}

	private void setBooks(java.util.List<BeagleBook> books) {
		beagleBooks.clear();
		for (BeagleBook book : books)
			beagleBooks.addElement(book);
		selectBook(null);
	}

	private void selectBook(BeagleBook book) {
		if (book == null)
			book = new BeagleBook();
		id.setText(book.getId());
		title.setText(book.getTitle());
		author.setText(book.getAuthor());
		page.setText(book.getCurrentPage() + " (" + book.getFirstPage() + " - "
				+ book.getLastPage() + ")");
	}

}
