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
 * Tests for multi items (Bandliste)
 * 
 * @author uwe
 *
 */
public class Dummy extends JunitHelper {
   private static final Logger LOG = LogManager.getLogger(Dummy.class);

   @Test
   public void test() {
      Record testRecord = buildTestRecord();
      Set<String> ret = indexer.getAllCharSets(testRecord, "100a:245a:710abcd");
      for (String retret : ret) {
         System.out.println("TEST: " + retret + "#");
      }

   }

}
