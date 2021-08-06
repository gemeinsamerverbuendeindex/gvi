package org.gvi.solrmarc.index;

import static org.junit.Assert.assertEquals;

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
   private static final Logger LOG        = LogManager.getLogger(Matchkey.class);
   private Record              testRecord = readMarcFromFile("matchKey_01.xml");

   /**
    * Validate the material as minimal match key TODO test more types
    */
   @Test
   public void material() {
      String matchKey = indexer.matchkeyMaterial(testRecord);
      LOG.debug("Key: " + matchKey);
      assertEquals("Wrong Material: ", "book", matchKey);
   }

   /**
    * Validate 'material' + 'author' + 'title' as match key
    */
   @Test
   public void materialAuthorTitle() {
      String matchKey = indexer.matchkeyMaterialAuthorTitle(testRecord);
      LOG.warn("Key: " + matchKey);
      assertEquals("Wrong key: ", "book:ullenboom:javaistaucheineinsel", matchKey);
   }

   /**
    * Validate 'material' + 'author' + 'title' + 'publish date' as match key
    */
   @Test
   public void materialAuthorTitleYear() {
      String matchKey = indexer.matchkeyMaterialAuthorTitleYear(testRecord);
      LOG.warn("Key: " + matchKey);
      assertEquals("Wrong key: ", "book:ullenboom:javaistaucheineinsel:2012", matchKey);
   }

   /**
    * Validate 'material' + 'title' + 'ISBN' as match key
    */
   @Test
   public void matchkeyMaterialISBNYear() {
      String matchKey = indexer.matchkeyMaterialISBNYear(testRecord);
      LOG.warn("Key: " + matchKey);
      assertEquals("Wrong key: ", "book:9783836218023:2012", matchKey);
   }

}
