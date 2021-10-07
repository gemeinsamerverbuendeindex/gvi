package org.gvi.solrmarc.index;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

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
      noDataLoaded("Skiptest");
   }

   /**
    * Verify resilience against missing properties.  
    */
   @Test
   public void missingProperty() {
      reloadPropertyFiles(indexer, initDataDir + "no_gnd", null, true, true);
      noDataLoaded("NoGndProp");
      reloadPropertyFiles(indexer, initDataDir + "no_kobv", true, null, true);
      noDataLoaded("NoKobvProp");
      reloadPropertyFiles(indexer, initDataDir + "no_culturegraph", true, true, null);
      noDataLoaded("NoCultProp");
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
      reloadPropertyFiles(indexer, initDataDir + "no_culturegraph", true, true, false);
      assertTrue("Any CultureGraph cluster infomations should exist, because the file is missing.", Init.cultureGraphClusterMap.isEmpty());
   }

   static void reloadPropertyFiles(GVIIndexer indexer, String initDataDir, Boolean skipSynonyms, Boolean skipClusterMap, Boolean skipCultureGraph) {
      System.setProperty("gnd.configdir", initDataDir);
      if (skipSynonyms == null) {
         System.clearProperty("GviIndexer.skipSynonyms");
      } else {
         System.setProperty("GviIndexer.skipSynonyms", String.valueOf(skipSynonyms));
      }
      if (skipClusterMap == null) {
         System.clearProperty("GviIndexer.skipClusterMap");
      } else {
         System.setProperty("GviIndexer.skipClusterMap", String.valueOf(skipClusterMap));
      }
      if (skipCultureGraph == null) {
         System.clearProperty("GviIndexer.skipCultureGraph");
      } else {
         System.setProperty("GviIndexer.skipCultureGraph", String.valueOf(skipCultureGraph));
      }
      indexer.isInitialized = false;
      indexer.init();
   }

   /**
    * Assure that none of the big date files is loaded
    */
   private void noDataLoaded(String testId) {
      assertTrue(testId + ": Any GND synonyms should exist, because the reading of the file was skipped.", Init.gndSynonymMap.isEmpty());
      assertTrue(testId + ": Any Kobv cluster infomations should exist, because the reading of the file was skipped.", Init.gndSynonymMap.isEmpty());
      assertTrue(testId + ": Any CultureGraph cluster infomations should exist, because the reading of the file was skipped.", Init.gndSynonymMap.isEmpty());
   }
   

}
