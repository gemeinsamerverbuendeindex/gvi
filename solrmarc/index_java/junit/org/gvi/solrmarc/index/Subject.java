package org.gvi.solrmarc.index;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.marc4j.marc.Record;

/**
 * Tests for the 'topic' methods<br>
 * TODO validate the field lists because they are not disjunct.
 * 
 * @author uwe
 *
 */
public class Subject extends JunitHelper {
   private static final Logger LOG          = LogManager.getLogger(Subject.class);
   private Record              testRecord_1 = readMarcFromFile("K10plus_529989859_subject.xml");

    /**
    * Validate the extraction of a chronological term as subject. 
    */
   @Test
   public void subjectCorporateName() {
      LOG.warn(indexer.getSubjectCorporateName(testRecord_1, "610a"));
   }

   /**
    * Validate the extraction of a meeting name as subject.
    */
   @Test
   public void subjectMeetingName() {
      LOG.warn(indexer.getSubjectMeetingName(testRecord_1, "611a"));
   }

   /**
    * Validate the extraction of a personal name as subject. 
    */
   @Test
   public void subjectPersonalName() {
      Set<String> topics = indexer.getSubjectPersonalName(testRecord_1, "600a");
      LOG.warn(topics);
      assertTrue("Vier Einträge erwartet: " + topics, (topics.size() == 4));
   }

   /**
    * Validate the extraction of a uniform title as subject. 
    */
   @Test
   public void subjectUniformTitle() {
      Set<String> topics = indexer.getSubjectUniformTitle(testRecord_1, "630a");
      assertTrue("Nur ein Eintrag erwartet.", (topics.size() == 1));
   }

   /**
    * Validate the extraction of a chronological term as subject.
    */
   @Test
   public void subjectChronological() {
      LOG.warn(indexer.getSubjectChronologicalTerm(testRecord_1, "600d:610y:611y:630y:648a:648y:650y:651y:655y"));
   }

   /**
    * Validate the extraction of a genre form  as subject. 
    */
   @Test
   public void subjectGenreForm() {
      LOG.warn(indexer.getSubjectGenreForm(testRecord_1, "600v:610v:611v:630v:648v:650v:651v:655a:655v"));
   }

   /**
    * Validate the extraction of a geographical name as subject.
    */
   @Test
   public void subjectGeographicalName() {
      LOG.warn(indexer.getSubjectGeographicalName(testRecord_1, "600z:610z:611z:630z:648z:650z:651a:651z:655z"));
   }

    /**
    * Validate the extraction of a topical term  as subject. 
    */
   @Test
   public void subjectTopicalTerm() {
      LOG.warn(indexer.getSubjectTopicalTerm(testRecord_1, "600a:600x:610x:611x:630x:648x:650a9:650x:651x:655x:938a"));
   }

}