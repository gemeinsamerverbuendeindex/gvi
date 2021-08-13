package org.gvi.solrmarc.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.marc4j.marc.Record;

/**
 * Tests for matchKey methods
 * 
 * @author uwe
 *
 */
public class Cluster extends JunitHelper {
   @SuppressWarnings("unused")
   private static final Logger LOG        = LogManager.getLogger(Cluster.class);
   private Record              testRecord_1 = readMarcFromFile("K10plus_012653829.xml");
   private Record              testRecord_2 = readMarcFromFile("ZDB_1003673171.xml");

   /**
    * Validate the  KOBV cluster lookup
    */
   @Test
   public void checkKobvDublettes() {
      Initialisierung.reloadPropertyFiles(indexer, Initialisierung.initDataDir, true, false, true);
      String originalId = indexer.getRecordId(testRecord_1);
      String clusterId = indexer.getDupId(testRecord_1);
      assertNotEquals("The record is dublette. A difference is expected", originalId, clusterId);
       originalId = indexer.getRecordId(testRecord_2);
       clusterId = indexer.getDupId(testRecord_2);
      assertEquals("The record is unique. No difference is expected", originalId, clusterId);
   }

   /**
    * Validate the CultureGraph cluster lookup
    */
   @Test
   @Ignore
   public void checkCgDublettes() {
      Initialisierung.reloadPropertyFiles(indexer, Initialisierung.initDataDir, true, true, false);
      String originalId = indexer.getRecordId(testRecord_1);
      String clusterId = indexer.getCultureGraphClusterId(testRecord_1);
      assertNotEquals("The record is dublette. A difference is expected", originalId, clusterId);
       originalId = indexer.getRecordId(testRecord_2);
       clusterId = indexer.getCultureGraphClusterId(testRecord_2);
      assertEquals("The record is unique. No difference is expected", originalId, clusterId);
   }
 
}
