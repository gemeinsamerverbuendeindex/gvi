package org.gvi.solrmarc.index.gvi;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.gvi.solrmarc.index.GVIIndexer;
import org.marc4j.marc.Record;

public class Cluster {
   private static final Logger LOG  = LogManager.getLogger(Cluster.class);
   private GVIIndexer          main = null;

   public Cluster(GVIIndexer callback) {
      main = callback;
   }

   /**
    * Lookup to get the duplicate key to the record's id
    * 
    * @param record The current data record
    * @return If found the duplicate key else the own id.
    */
   public String getDupId(Record record) {
      String id = main.getRecordID(record);
      return Init.kobvClusterMap.getProperty(id, id);
   }
}
