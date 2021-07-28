package org.gvi.solrmarc.index;

import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.marc4j.marc.Record;

public class Cluster extends MatchKey {
   private static final Logger LOG = LogManager.getLogger(Cluster.class);
   public static Properties                         kobvClusterMap              = null;
   public static Properties                         cutureGraphClusterMap       = null;

   public Cluster(String indexingPropsFile, String[] propertyDirs) {
      super(indexingPropsFile, propertyDirs);
   }
   
   /**
    * Lookup to get the duplicate key to the record's id
    * 
    * @param record The current data record
    * @return If found the duplicate key else the own id.
    */
   public String getDupId(Record record) {
      String id = getRecordID(record);
      return kobvClusterMap.getProperty(id, id);
   }
}
