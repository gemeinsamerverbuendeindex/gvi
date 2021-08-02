package org.gvi.solrmarc.index.gvi;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.gvi.solrmarc.index.GVIIndexer;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

public class Material {

   private static final Logger LOG  = LogManager.getLogger(Material.class);
   private GVIIndexer          main = null;

   public Material(GVIIndexer callback) {
      main = callback;
   }

   /**
    * Determine medium of material
    *
    * @param record
    * @return Set material medium of record
    */
   @Deprecated // code w/o real function
   public Set<String> getMaterialMedium(Record record) {
      Set<String> result = new LinkedHashSet<>();

      if (result.isEmpty()) {
         result.add("UNDEFINED");
      }
      return result;
   }

   /**
    * Determine type of material
    *
    * @param record
    * @return Set material type of record
    */
   public Set<String> getMaterialType(Record record) {
      Set<String> result = new LinkedHashSet<>();
      char materialTypeCode = record.getLeader().getTypeOfRecord();
      String materialType = "material_type." + materialTypeCode;
      result.add(materialType);
      return result;
   }

   /**
    * Determine access method of material (physical, online)
    *
    * @param record
    * @return Set access record
    */
   public Set<String> getMaterialAccess(Record record) {
      Set<String> result = new HashSet<>();
      // material_access.Online = 007[01]=cr OR has 856 field with indicator 40
      ControlField field007 = ((ControlField) record.getVariableField("007"));
      if (field007 != null) {
         // System.out.println("DEBUG "+field007.getData());
         String accessCode = field007.getData();
         DataField data856 = (DataField) record.getVariableField("856");

         if (accessCode.length() > 1 && "cr".equals(accessCode.substring(0, 2)) || (data856 != null && data856.getIndicator1() == '4' && data856.getIndicator1() == '0')) {
            result.add("Online");
            // check 856 field again
            if (data856 != null) {
               Subfield noteField = data856.getSubfield('z');
               if (noteField != null) {
                  String note = noteField.getData();
                  if (note != null && note.matches("[Kk]ostenfrei")) {
                     result.add("Online Kostenfrei");
                  }
               }
            }
         }
      }

      if (result.isEmpty()) {
         result.add("Physical");
      }

      return result;
   }

}
