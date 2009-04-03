/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *  
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.media.bundle.mpeg7;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.media.bundle.Mpeg7Catalog;
import org.opencastproject.media.bundle.mpeg7.Mpeg7CatalogImpl;
import org.opencastproject.util.UnknownFileTypeException;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Test class for the dublin core implementation.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class Mpeg7Test {

	/** The catalog name */
	public static String catalogName ="/mpeg7.xml";
	
	/** The test catalog */
	private File catalogFile = null;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		catalogFile = new File(this.getClass().getResource(catalogName).getPath());
		if (!catalogFile.exists() || !catalogFile.canRead())
			throw new Exception("Unable to access mpeg-7 test catalog '" + catalogName + "'");
	}

	/**
	 * Test method for {@link org.opencastproject.media.bundle.mpeg7.Mpeg7CatalogImpl#fromFile(java.io.File)}.
	 */
	@Test
	public void testFromFile() {
		Mpeg7Catalog mpeg7 = parse(catalogFile);
		testContent(mpeg7);
	}

	/**
	 * Test method for {@link org.opencastproject.media.bundle.mpeg7.Mpeg7CatalogImpl#save()}.
	 */
	@Test
	public void testNewInstance() {
		// Read the sample catalog
		Mpeg7Catalog mpeg7Sample = parse(catalogFile);

		// Create a new catalog and fill it with a few fields
		Mpeg7Catalog mpeg7New = null;
		try {
			mpeg7New = Mpeg7CatalogImpl.newInstance();

			// TODO: Add sample tracks to new catalog
			// TODO: Add sample video segments to new catalog
			// TODO: Add sample annotations to new catalog
			
			// Store the catalog
			mpeg7New.save();
		} catch (NoSuchAlgorithmException e) {
			fail("Error verifying the catalog checksum: " + e.getMessage());
		} catch (IOException e) {
			fail("Error creating the catalog: " + e.getMessage());
		} catch (UnknownFileTypeException e) {
			fail("The catalog's mime type is not supported: " + e.getMessage());
		} catch (ParserConfigurationException e) {
			fail("Error creating a parser for the catalog: " + e.getMessage());
		} catch (TransformerException e) {
			fail("Error saving the catalog: " + e.getMessage());
		}
		
		// Re-read the saved catalog and test for its content
		Mpeg7Catalog mpeg7NewFromDisk = parse(mpeg7New.getFile());
		//testContent(mpeg7NewFromDisk);
	}
	
	/**
	 * Tests the contents of the sample catalog mpeg7.xml.
	 */
	@SuppressWarnings("unchecked")
	public void testContent(Mpeg7Catalog mpeg7) {
		// Check presence of content
		assertTrue(mpeg7.hasAudioContent());
		assertTrue(mpeg7.hasVideoContent());
		assertFalse(mpeg7.hasAudioVisualContent());
		
		// Check content size
		assertTrue(mpeg7.getMultimediaContent(MultimediaContent.Type.AudioType).size() == 1);
		assertTrue(mpeg7.getMultimediaContent(MultimediaContent.Type.VideoType).size() == 2);

		// Check tracks
		assertNotNull(mpeg7.getAudioById("track-1"));
		assertNotNull(mpeg7.getVideoById("track-2"));
		assertNotNull(mpeg7.getVideoById("track-3"));

		//
		// Check audio track (track-1)
		//
		
		MultimediaContentType track1 = mpeg7.getAudioById("track-1");
		MediaTime audioMediaTime = track1.getMediaTime();

		// Media locator
		assertEquals(track1.getMediaLocator().getMediaURI(), URI.create("file:tracks/audio.pcm"));
		// Media time point
		assertTrue(audioMediaTime.getMediaTimePoint().getDay() == 0);
		assertTrue(audioMediaTime.getMediaTimePoint().getHour() == 0);
		assertTrue(audioMediaTime.getMediaTimePoint().getMinutes() == 0);
		assertTrue(audioMediaTime.getMediaTimePoint().getSeconds() == 0);
		assertTrue(audioMediaTime.getMediaTimePoint().getFractionsPerSecond() == 25);
		assertTrue(audioMediaTime.getMediaTimePoint().getNFractions() == 0);
		// Media duration
		assertTrue(audioMediaTime.getMediaDuration().getDays() == 0);
		assertTrue(audioMediaTime.getMediaDuration().getHours() == 1);
		assertTrue(audioMediaTime.getMediaDuration().getMinutes() == 30);
		assertTrue(audioMediaTime.getMediaDuration().getSeconds() == 0);
		// Segments
		assertFalse(track1.getTemporalDecomposition().segments().hasNext());
		
		//
		// Check video track (track-2)
		//
		
		MultimediaContentType track2 = mpeg7.getVideoById("track-2");
		MediaTime v1MediaTime = track2.getMediaTime();

		// Media locator
		assertEquals(track2.getMediaLocator().getMediaURI(), URI.create("file:tracks/presentation.mp4"));
		// Media time point
		assertTrue(v1MediaTime.getMediaTimePoint().getDay() == 0);
		assertTrue(v1MediaTime.getMediaTimePoint().getHour() == 0);
		assertTrue(v1MediaTime.getMediaTimePoint().getMinutes() == 0);
		assertTrue(v1MediaTime.getMediaTimePoint().getSeconds() == 0);
		assertTrue(v1MediaTime.getMediaTimePoint().getFractionsPerSecond() == 25);
		assertTrue(v1MediaTime.getMediaTimePoint().getNFractions() == 0);
		// Media duration
		assertTrue(v1MediaTime.getMediaDuration().getDays() == 0);
		assertTrue(v1MediaTime.getMediaDuration().getHours() == 1);
		assertTrue(v1MediaTime.getMediaDuration().getMinutes() == 30);
		assertTrue(v1MediaTime.getMediaDuration().getSeconds() == 0);
		// Segments
		TemporalDecomposition<VideoSegment> v1Decomposition = (TemporalDecomposition<VideoSegment>)track2.getTemporalDecomposition();
		assertFalse(v1Decomposition.hasGap());
		assertFalse(v1Decomposition.isOverlapping());
		assertEquals(v1Decomposition.getCriteria(), TemporalDecomposition.DecompositionCriteria.Temporal);
		assertTrue(v1Decomposition.segments().hasNext());
		// Segment track-2.segment-1
		VideoSegment v1Segment1 = v1Decomposition.getSegmentById("track-2.segment-1");
		assertNotNull(v1Segment1);
		MediaTime segment1MediaTime = v1Segment1.getMediaTime();
		// Media time point
		assertTrue(segment1MediaTime.getMediaTimePoint().getDay() == 0);
		assertTrue(segment1MediaTime.getMediaTimePoint().getHour() == 0);
		assertTrue(segment1MediaTime.getMediaTimePoint().getMinutes() == 0);
		assertTrue(segment1MediaTime.getMediaTimePoint().getSeconds() == 0);
		assertTrue(segment1MediaTime.getMediaTimePoint().getFractionsPerSecond() == 25);
		assertTrue(segment1MediaTime.getMediaTimePoint().getNFractions() == 0);
		// Media duration
		assertTrue(segment1MediaTime.getMediaDuration().getDays() == 0);
		assertTrue(segment1MediaTime.getMediaDuration().getHours() == 1);
		assertTrue(segment1MediaTime.getMediaDuration().getMinutes() == 7);
		assertTrue(segment1MediaTime.getMediaDuration().getSeconds() == 35);
		// Text annotations
		assertTrue(v1Segment1.hasTextAnnotations());
		assertTrue(v1Segment1.hasTextAnnotations(0.4f, 0.5f));
		assertFalse(v1Segment1.hasTextAnnotations(0.8f, 0.8f));
		assertTrue(v1Segment1.hasTextAnnotations("de"));
		assertFalse(v1Segment1.hasTextAnnotations("fr"));
		// Keywords
		TextAnnotation textAnnotation = v1Segment1.textAnnotations().next();
		assertEquals(textAnnotation.keywordAnnotations().next().getKeyword(), "Armin");
		assertEquals(textAnnotation.freeTextAnnotations().next().getText(), "Hint Armin");
		
		//
		// Check video track (track-3)
		//

		MultimediaContentType track3 = mpeg7.getVideoById("track-3");
		MediaTime v2MediaTime = track3.getMediaTime();

		// Media locator
		assertEquals(track3.getMediaLocator().getMediaURI(), URI.create("file:tracks/presenter.mpg"));
		// Media time point
		assertTrue(v2MediaTime.getMediaTimePoint().getDay() == 0);
		assertTrue(v2MediaTime.getMediaTimePoint().getHour() == 0);
		assertTrue(v2MediaTime.getMediaTimePoint().getMinutes() == 0);
		assertTrue(v2MediaTime.getMediaTimePoint().getSeconds() == 0);
		assertTrue(v2MediaTime.getMediaTimePoint().getFractionsPerSecond() == 25);
		assertTrue(v2MediaTime.getMediaTimePoint().getNFractions() == 0);
		// Media duration
		assertTrue(v2MediaTime.getMediaDuration().getDays() == 0);
		assertTrue(v2MediaTime.getMediaDuration().getHours() == 1);
		assertTrue(v2MediaTime.getMediaDuration().getMinutes() == 30);
		assertTrue(v2MediaTime.getMediaDuration().getSeconds() == 0);
		// Segments
		TemporalDecomposition<VideoSegment> v2Decomposition = (TemporalDecomposition<VideoSegment>)track3.getTemporalDecomposition();
		assertFalse(v2Decomposition.segments().hasNext());

	}

	/**
	 * Parses the test catalog.
	 * 
	 * @param file the file containing the catalog
	 * @return the mpeg-7 object representation
	 */
	private Mpeg7Catalog parse(File file) {
		Mpeg7Catalog mpeg7 = null;
		try {
			mpeg7 = Mpeg7CatalogImpl.fromFile(file);
			return mpeg7;
		} catch (NoSuchAlgorithmException e) {
			fail("Error verifying the catalog checksum: " + e.getMessage());
		} catch (IOException e) {
			fail("Error accessing the catalog: " + e.getMessage());
		} catch (UnknownFileTypeException e) {
			fail("The catalog's mime type is not supported: " + e.getMessage());
		} catch (ParserConfigurationException e) {
			fail("Error creating a parser for the catalog: " + e.getMessage());
		} catch (SAXException e) {
			fail("Error parsing the catalog: " + e.getMessage());
		}
		throw new IllegalStateException("Shouldn't get here!");
	}

}