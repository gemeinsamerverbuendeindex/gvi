package org.gvi.solrmarc.index;

import static org.junit.Assert.assertEquals;
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
public class Gnd_Charset extends JunitHelper {
   @SuppressWarnings("unused")
   private static final Logger LOG        = LogManager.getLogger(Gnd_Charset.class);
   private Record              testRecord_2 = readMarcFromFile("ZDB_1003673171.xml");

   /**
    * Validate the material as minimal match key TODO test more types
    */
   @Test
   public void termId() {
      Set<String> gndIds = indexer.getTermID(testRecord_2, "1000:1100:1110:7000:7100:7110", "DE-588", "false");
      assertTrue("Two ids should be found. " + gndIds, (gndIds.size() == 2));
      assertTrue("The id '1692-5' should exist.", gndIds.contains("1692-5"));
      gndIds = indexer.getTermID(testRecord_2, "1000:1100:1110:7000:7100:7110", "DE-588", "true");
      assertTrue("Two ids should be found. " + gndIds, (gndIds.size() == 2));
      assertTrue("The id '(DE-588)1692-5' should exist.", gndIds.contains("(DE-588)1692-5"));
   }

   /**
    * Validate the enriched result set<br>
    */
   @Test
   public void expandGnd() {
      Initialisierung.reloadPropertyFiles(indexer, Initialisierung.initDataDir, false, true, true);
      Set<String> gndIds = indexer.expandGnd(testRecord_2, "1000:1100:1110:7000:7100:7110");
      assertEquals("Wrong Number of Entries: ", 8, gndIds.size());
      assertTrue("Entry \"Solid-State Circuits Society\" expected.", gndIds.contains("Solid-State Circuits Society"));
      // TODO this test does not evaluate the processing for marc:689 fields
      gndIds = indexer.expandGnd(testRecord_2, "6000:6100:6110:6300:6480:6500:6510:6550", "6890");
      assertEquals("Wrong Number of Entries: ", 7, gndIds.size());
      assertTrue("Entry \"Yearbooks\" expected.", gndIds.contains("Yearbooks"));
   }

   /**
    * Validate the enriched result set<br>
    */
   @Test
   public void charSets() {
      Set<String> charSets = indexer.getAllCharSets(readMarcFromFile("hebis_288108418.xml"), "130a:240a:242a:245abnp:246a:247ab:490a:730a:830a");
      LOG.warn(charSets);
      assertEquals("Wrong Number of Entries: ", 5, charSets.size());
      assertTrue("Entry \"חרגול פלוס\" expected.", charSets.contains("חרגול פלוס"));
   }
   
}
