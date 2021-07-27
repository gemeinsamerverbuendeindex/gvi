package org.gvi.solrmarc.index;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

/**
 * Validate the 'init' process
 * 
 * @author uwe
 *
 */
public class Initialisierung extends JunitHelper {
   private static final Logger LOG = LogManager.getLogger(Initialisierung.class);
   private String initDataDir = pathToData +  "big_property_files";

   /**
    * Don't read any data
    */
   @Test
   public void skipAll() {
      System.setProperty("GviIndexer.skipSynonyms", "true");
      System.setProperty("GviIndexer.skipClusterMap", "true");
      System.setProperty("GviIndexer.skipCultureGraph", "true");
      indexer.isInitialized = false;
      assertTrue("Any GND synonyms should exist, because the reading of the file was skipped.", GVIIndexer.gndSynonymMap.isEmpty());
      assertTrue("Any Kobv cluster infomations should exist, because the reading of the file was skipped.", GVIIndexer.gndSynonymMap.isEmpty());
      assertTrue("Any CultureGraph cluster infomations should exist, because the reading of the file was skipped.", GVIIndexer.gndSynonymMap.isEmpty());
   }

   /**
    * Read dummy data into the KOBV cluster map
    */
   @Test
   public void readKobvClusterData() {
      System.setProperty("gnd.configdir", initDataDir);
      System.setProperty("GviIndexer.skipSynonyms", "true");
      System.setProperty("GviIndexer.skipClusterMap", "false");
      System.setProperty("GviIndexer.skipCultureGraph", "true");
      indexer.isInitialized = false;
      indexer.init();
      assertFalse("Kobv cluster infomations should exist.", GVIIndexer.kobvClusterMap.isEmpty());
   }

   /**
    * Check reaction of a missing file
    */
   @Test
   public void ignoreMissingFile() {
      System.setProperty("gnd.configdir", initDataDir);
      System.setProperty("GviIndexer.skipSynonyms", "true");
      System.setProperty("GviIndexer.skipClusterMap", "true");
      System.clearProperty("GviIndexer.skipCultureGraph");
      indexer.isInitialized = false;
         indexer.init();
         assertTrue("Any CultureGraph cluster infomations should exist, because the file is missing.", GVIIndexer.cutureGraphClusterMap.isEmpty());
   }

}
