package org.gvi.solrmarc.index;

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
public class Basic extends JunitHelper {
   private static final Logger LOG = LogManager.getLogger(Basic.class);
   private Record testRecord = readMarcFromFile("K10plus_012653829.xml");
   
   /**
    * 
    */
   @Test
   public void consortium() {
      Set<String> isils = indexer.getConsortium(testRecord);
      LOG.debug("ISILs: " + isils);
      assertTrue("Only one ISIL is expected.", (isils.size() == 1));
      assertTrue("The consortium should be  GBV \"DE-601\"", isils.contains("DE-601"));
   }
}
