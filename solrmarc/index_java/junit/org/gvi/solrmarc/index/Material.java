package org.gvi.solrmarc.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

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
   private static final Logger LOG          = LogManager.getLogger(Material.class);
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
    * Validate the identification of access types<br>
    * Possible values are ("Physical"|"Online"|"Online Kostenfrei")<br>
    * TODO add tests for marc:8567 and marc:506ind1
    */
   @Test
   public void accessType() {
      Set<String> accessTypes = indexer.getMaterialAccess(testRecord_1);
      assertEquals("Wrong number of entries.", 1, accessTypes.size());
      assertFalse("The access type should not be \"Online\".", accessTypes.contains("Online"));
      assertTrue("The access type should be \"Physical\".", accessTypes.contains("Physical"));
      accessTypes = indexer.getMaterialAccess(testRecord_2);
      assertEquals("Wrong number of entries.", 2, accessTypes.size());
      assertTrue("The access type should be \"Online\".", accessTypes.contains("Online"));
      assertTrue("The access type should be \"Online Kostenfrei\".", accessTypes.contains("Online Kostenfrei"));
   }
}
