package org.gvi.solrmarc.index;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
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
public class Matchkey extends JunitHelper {
   private static final Logger LOG = LogManager.getLogger(Matchkey.class);
   private Record testRecord = readMarcFromFile("matchKey_01.xml");
   
   /**
    * Validate ISBN as minimal match key
    */
   @Test
   public void matchISBN() {
      String matchKey = indexer.matchkeyISBN(testRecord);
      LOG.debug("Key: " + matchKey);
      assertTrue("Wrong match key (ISBN)", "9783836218023".equals(matchKey));
   }
   
   /**
    * Validate ISBN as minimal match key
    */
   @Test
   public void matchMaterial() {
      String matchKey = indexer.matchkeyMaterial(testRecord);
      LOG.debug("Key: " + matchKey);
      assertTrue("Wrong Material: not 'book'", "book".equals(matchKey));
   }
   
   /**
    * Validate author as minimal match key
    */
   @Test
   public void matchAuthor() {
      String matchKey = indexer.matchkeyAuthor(testRecord);
      LOG.warn("Key: " + matchKey);
      assertTrue("Wrong author: not 'ullenboom'", "ullenboom".equals(matchKey));
   }
   
   


}
