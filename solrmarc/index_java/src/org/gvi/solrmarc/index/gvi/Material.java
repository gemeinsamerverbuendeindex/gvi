package org.gvi.solrmarc.index.gvi;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;

public class Material {

   @SuppressWarnings("unused")
   private static final Logger LOG  = LogManager.getLogger(Material.class);

   /**
    * Determine medium of material<br>
    * TODO Is this method obsolete?
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
    * Determine type of material<br>
    * TODO Is this method obsolete?
    * 
    * @param record
    * @return Set material type of record
    */
   @Deprecated // never used
   public Set<String> getMaterialType(Record record) {
      Set<String> result = new LinkedHashSet<>();
      char materialTypeCode = record.getLeader().getTypeOfRecord();
      String materialType = "material_type." + materialTypeCode;
      result.add(materialType);
      return result;
   }

   /**
    * Detects convolutes of journal issues<br>
    * The criteria (according to GVI-217) are:<br>
    * marc:951p == "JV" AND marc:245p is undefined<br>
    * 
    * @param record
    * @return ("true"|"false")
    */
   public String isJournalVolume(Record record) {
      DataField field951 = (DataField) record.getVariableField("951");
      if (field951 == null) return "false";
      Subfield materialcode = field951.getSubfield('a');
      if (materialcode == null) return "false";
      if ("JV".equals(materialcode.getData())) {
         DataField field245 = (DataField) record.getVariableField("245");
         if (field245 != null) {
            if (field245.getSubfield('p') != null) {
               return "false";
            }
         }
      }
      return "true";

   }

   /**
    * Determine access methods of material<br>
    * ("Physical", "Online", "Online Kostenfrei")
    *
    * @param record
    * @return
    */
   public Set<String> getMaterialAccess(Record record) {
      Set<String> result = getAccessTypeBy007(record);
      result.addAll(getAccessTypeBy856(record));
      if (result.isEmpty()) {
         result.add("Physical");
      }
      return result;
   }

   /**
    * Detect the online availability of the resource.<br>
    * The title is online available when at least one control field marc:007 starts with "cr".
    * 
    * @param record
    * @return An empty set or the result of {@link #getOnlineTypes(Record)}
    */
   // material_access.Online = 007[01]=cr
   private Set<String> getAccessTypeBy007(Record record) {
      Set<String> result = new HashSet<>();
      List<VariableField> fields007 = record.getVariableFields("007");
      if (fields007 == null) {
         return result;
      }
      for (VariableField field : fields007) {
         ControlField data007 = (ControlField) field;
         String accessCode = data007.getData();
         if ((accessCode.length() > 1) && "cr".equals(accessCode.substring(0, 2))) {
            result.addAll(getOnlineTypes(record));
            return result;
         }
      }
      return result;
   }

   /**
    * Detect the online availability of the resource.<br>
    * The title is online available when marked with marc:856 i1='4' i2='0'
    * 
    * @param record
    * @return An empty set or the result of {@link #getOnlineTypes(Record)}
    */
   private Set<String> getAccessTypeBy856(Record record) {
      Set<String> result = new HashSet<>();
      List<VariableField> fields856 = record.getVariableFields("856");
      if (fields856 == null) {
         return result;
      }
      for (VariableField field : fields856) {
         DataField data856 = (DataField) field;
         if ((data856 != null) && (data856.getIndicator1() == '4') && (data856.getIndicator1() == '0')) {
            result.addAll(getOnlineTypes(record));
         }
      }
      return result;
   }

   /**
    * Detect if the online resource is free<br>
    * Evaluates the values of:<br>
    * - subfield marc:8567 - subfield marc:856z - first indicator marc:506
    * 
    * @param record
    * @return ["Online"] or ["Online","Online Kostenfrei"]
    */
   private Set<String> getOnlineTypes(Record record) {
      Set<String> result = new HashSet<>();
      result.add("Online");
      List<VariableField> fields856 = record.getVariableFields("856");
      if (fields856 == null) {
         return result;
      }
      for (VariableField field : fields856) {
         DataField data856 = (DataField) field;
         Subfield accessStatusText = data856.getSubfield('z');
         if ((accessStatusText != null) && accessStatusText.getData().toLowerCase().contains("kostenfrei")) {
            return kostenfrei(result);
         }
         Subfield accessStatus = data856.getSubfield('7'); // Currently not used in D-A-CH
         if ((accessStatus != null) && "0".equals(accessStatus.getData())) {
            return kostenfrei(result);
         }
      }
      List<VariableField> fields506 = record.getVariableFields("506");
      if (fields506 == null) {
         return result;
      }
      for (VariableField field : fields506) {
         DataField data506 = (DataField) field;
         if (data506.getIndicator1() == '0') {
            return kostenfrei(result);
         }
      }
      return result;
   }

   private Set<String> kostenfrei(Set<String> result) {
      result.add("Online Kostenfrei");
      return result;
   }

}
