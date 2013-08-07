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

import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;

/**
 * A book on the txtr beagle
 */
public class BeagleBook {
	private Charset utf8 = Charset.forName("UTF-8");

	String id;
	int firstPage;
	int lastPage;
	int currentPage;
	String author;
	String title;

	public BeagleBook(String id, String author, String title) {
		this.id = id;
		this.author = author;
		this.title = title;
	}

	public BeagleBook() {

	}

	private String base64_decode(String text) {
		return new String(Base64.decodeBase64(text), utf8);
	}

	private String base64_encode(String text) {
		return Base64.encodeBase64String(text.getBytes(utf8));
	}

	public String getAuthor() {
		return author;
	}

	public String getAuthorBase64() {
		return base64_encode(author);
	}

	public int getCurrentPage() {
		return currentPage;
	}

	public int getFirstPage() {
		return firstPage;
	}

	public String getId() {
		return id;
	}

	public int getLastPage() {
		return lastPage;
	}

	public String getTitle() {
		return title;
	}

	public String getTitleBase64() {
		return base64_encode(title);
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void setAuthorBase64(String author) {
		this.author = base64_decode(author);
	}

	public void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
	}

	public void setFirstPage(int firstPage) {
		this.firstPage = firstPage;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setLastPage(int lastPage) {
		this.lastPage = lastPage;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setTitleBase64(String title) {
		this.title = base64_decode(title);
	}

	@Override
	public String toString() {
		return getAuthor() == null ? getTitle()
				: (getAuthor() + ": " + getTitle());
	}

}
