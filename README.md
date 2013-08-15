jBeagle
=======

Java application for managing the txtr beagle

jBeagle supports
- *Listing* the books present on the beagle
- *Deleting* books
- *Uploading PDF* documents as ebooks
 
jBeagle is based on
- *bluecove* http://bluecove.org/ for bluetooth support
- *jzlib* http://www.jcraft.com/jzlib/ for image compression
- *JPedal* http://sourceforge.net/projects/jpedal/ for PDF rendering
- the txtr beagle analysis of Florian Echtler http://floe.butterbrot.org/matrix/hacking/txtr/ 
- and Ligius http://hackcorellation.blogspot.de/2013/07/txtr-beagle-part-two-software.html

jBeagle is released under the GNU GPL.


How to use
==========

1. Pair the beagle with your PC using the operating system mechanisms
2. Switch the beagle into bluetooth mode (switch on, then hold the power key until the light flashes blue)
3. Start jBeagle

jBeagle then shows a list of books present on the beagle. You can select a book and click Delete to remove the book.
Click Upload and select a PDF file to upload it as a new book (You can use Calibre http://calibre-ebook.com/ to convert EPUB to PDF).


How to run
==========

Option 1:
- Import the project into Eclipse (Java + M2E)
- Run the class de.schierla.jbeagle.ui.JBeagle as a Java application

Option 2:
- compile project using *mvn compile assembly:single*
- Execute the resulting file *target/jbeagle-0.1-SNAPSHOT-jar-with-dependencies.jar*
