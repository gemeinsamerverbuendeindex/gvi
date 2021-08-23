package org.gvi.solrmarc.index;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gvi.solrmarc.index.gvi.Init;
import org.junit.Test;

/**
 * Validate the 'init' process
 * 
 * @author uwe
 *
 */
public class Initialisierung extends JunitHelper {
   @SuppressWarnings("unused")
   private static final Logger LOG         = LogManager.getLogger(Initialisierung.class);
   static String               initDataDir = pathToData + "big_property_files";

   /**
    * Don't read any data
    */
   @Test
   public void skipAll() {
      reloadPropertyFiles(indexer, initDataDir, true, true, true);
      assertTrue("Any GND synonyms should exist, because the reading of the file was skipped.", Init.gndSynonymMap.isEmpty());
      assertTrue("Any Kobv cluster infomations should exist, because the reading of the file was skipped.", Init.gndSynonymMap.isEmpty());
      assertTrue("Any CultureGraph cluster infomations should exist, because the reading of the file was skipped.", Init.gndSynonymMap.isEmpty());
   }

   /**
    * Read dummy data into the KOBV cluster map
    */
   @Test
   public void readKobvClusterData() {
      reloadPropertyFiles(indexer, initDataDir, true, false, true);
      assertFalse("Kobv cluster infomations should exist.", Init.kobvClusterMap.isEmpty());
   }

   /**
    * Check reaction of a missing file
    */
   @Test
   public void ignoreMissingFile() {
      reloadPropertyFiles(indexer, initDataDir, true, true, false);
      assertTrue("Any CultureGraph cluster infomations should exist, because the file is missing.", Init.cultureGraphClusterMap.isEmpty());
   }

   static void reloadPropertyFiles(GVIIndexer indexer, String initDataDir, boolean skipSynonyms, boolean skipClusterMap, boolean skipCultureGraph) {
      System.setProperty("gnd.configdir", initDataDir);
      System.setProperty("GviIndexer.skipSynonyms", String.valueOf(skipSynonyms));
      System.setProperty("GviIndexer.skipClusterMap", String.valueOf(skipClusterMap));
      System.setProperty("GviIndexer.skipCultureGraph", String.valueOf(skipCultureGraph));
      indexer.isInitialized = false;
      indexer.init();
   }

}
