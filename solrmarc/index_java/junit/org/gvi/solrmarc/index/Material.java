package org.gvi.solrmarc.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.marc4j.marc.Record;

/**
 * Tests for matchKey methods
 * 
 * @author uwe
 *
 */
public class Material extends JunitHelper {
   @SuppressWarnings("unused")
   private static final Logger LOG        = LogManager.getLogger(Material.class);
   private Record              testRecord_1 = readMarcFromFile("K10plus_012653829.xml");
   private Record              testRecord_2 = readMarcFromFile("ZDB_1003673171.xml");

   /**
    * Validate the material as minimal match key TODO test more types
    */
   @Test
   public void Journalvolume() {
      String flag = indexer.isJournalVolume(testRecord_1);
      assertEquals("This title schould be marked as 'JournalVolume'.", "true", flag);
      flag = indexer.isJournalVolume(testRecord_2);
      assertNotEquals("This title schould not be marked as 'JournalVolume'.", "true", flag);
   }

   /**
    * Validate the identifaication of access types<br>
    * Possible values are ("Physical"|"Online"|"Online Kostenfrei")
    */
   @Test
   public void accessType() {
      Set<String> accessTypes = indexer.getMaterialAccess(testRecord_1);
      LOG.warn(accessTypes);
      accessTypes = indexer.getMaterialAccess(testRecord_2);
      LOG.warn(accessTypes);
   }
}
