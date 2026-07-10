package org.gvi.solrmarc.index.gvi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.time.LocalDateTime;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Init {
   private static final Logger LOG                     = LogManager.getLogger(Init.class);
   private static Init         me                      = null;
   private static final String kobvClusterFile         = "kobv_clusters.properties";
   private static final String cultureGraphClusterFile = "culturegraph_clusters.properties";
   private static final String gndSynonymFile          = "gnd_synonyms.properties";
   public static Properties    gndSynonymMap           = null;
   public static Properties    kobvClusterMap          = null;
   public static Properties    cultureGraphClusterMap  = null;

   /**
    * Singleton
    */
   private Init() {
   }

   /**
    * Read big property files into the heap memory<br>
    * This allows the fast enrichment of documents while indexing.<br>
    * The loading is controlled by global flags:
    * <dl>
    * <dt>gnd.configdir</dt>
    * <dd>The directory, containing the files "gnd_synonyms.properties", "kobv_clusters.properties" and "culturegraph_clusters.properties"</dd>
    * <dt>GviIndexer.skipSynonyms</dt>
    * <dd>If 'true' skip reading the file gnd_synonyms.properties.</dd>
    * <dt>GviIndexer.skipClusterMap</dt>
    * <dd>If 'true' skip reading the file kobv_clusters.properties.</dd>
    * <dt>GviIndexer.skipCultureGraph</dt>
    * <dd>If 'true' skip reading the file culturegraph_clusters.properties.</dd>
    * <dt>
    * 
    * While loading the data the whole indexing process is locked.
    * @throws Exception
    * @return this singleton
    */
   public static synchronized Init init() {
      if (me != null) return me;
      me = new Init();
      me.loadDataFiles();
      return me;
   }

   /**
    * Reload the big property files like in {@link #init()}<br>
    * !! This method is intended for tests only. 
    * !! Reloading in a production environment may result in inconsistent data.
    */
   public synchronized void reload() {
      loadDataFiles();
   }

   /**
    * Read the big data files<br>
    */
   private void loadDataFiles() {
      String baseDir = System.getProperty("gnd.configdir", ".");
      gndSynonymMap = init_read_big_propertyFiles("skipSynonyms", baseDir, gndSynonymFile);
      kobvClusterMap = init_read_big_propertyFiles("skipClusterMap", baseDir, kobvClusterFile);
      cultureGraphClusterMap = init_read_big_propertyFiles("skipCultureGraph", baseDir, cultureGraphClusterFile);
   }

   /**
    * Helper for {@link #init()}<br>
    * Reads the data from Disk.
    * 
    * @param flagName Abbreviation of the global flag GviIndexer.<flagName>. If true the reading of the file will be skipped.
    * @param dir The directory containing the file
    * @param fileName The name of the File to read
    * @return A new property collection. Empty on error or the skip flag was set.
    */
   private Properties init_read_big_propertyFiles(String flagName, String dir, String fileName) {
      Properties data = new Properties();
      String bigDataFlag = System.getProperty("GviIndexer." + flagName);
      if ((bigDataFlag == null) || bigDataFlag.trim().toLowerCase().equals("true")) {
         LOG.info("GviIndexer." + flagName + " is true, skip loading.");
         return data;
      }
      if (LOG.isInfoEnabled()) {
         LOG.info("Loading of " + fileName + " started at: " + LocalDateTime.now().toString());
         listMem();
      }
      try {
         data.load(new FileInputStream(new File(dir, fileName)));
      }
      catch (IOException e) {
         LOG.warn("Fehler beim Einlesen von Anreicherungsdaten", e);
         return data;
      }
      if (LOG.isInfoEnabled()) {
         LOG.info("Loading of " + fileName + " finished at: " + LocalDateTime.now().toString());
         listMem();
      }
      return data;
   }

   /**
    * Helper for {@link #init_read_big_propertyFiles()}<br>
    * Prints the amount of free heap memory to log.
    */
   private void listMem() {
      for (MemoryPoolMXBean mpBean : ManagementFactory.getMemoryPoolMXBeans()) {
         if (mpBean.getType() == MemoryType.HEAP) {
            LOG.info(String.format("Name: %s max_used: %2.2fG now_used: %2.2fG (max_avail: %2.2fG)\n", mpBean.getName(), toGb(mpBean.getPeakUsage().getUsed()), toGb(mpBean.getUsage().getUsed()), toGb(mpBean.getUsage().getMax())));
         }
      }
   }

   /**
    * Helper for {@link #listMem()}<br>
    * Divide by 1024³
    * 
    * @param num
    * @return
    */
   private float toGb(long num) {
      float ret = num / 1024;
      return ret / 1024 / 1024;
   }

}
