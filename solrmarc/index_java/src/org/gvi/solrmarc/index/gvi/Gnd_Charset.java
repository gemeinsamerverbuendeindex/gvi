package org.gvi.solrmarc.index.gvi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.gvi.solrmarc.index.GVIIndexer;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;

public class Gnd_Charset {
   @SuppressWarnings("unused")
   private static final Logger      LOG              = LogManager.getLogger(Gnd_Charset.class);
   private GVIIndexer               main             = null;
   private static final String      gndLineSeperator = "!_#_!";
   private Map<String, Set<String>> cached880Fields  = new HashMap<>();
   private Record                   cachedRecord;

   public Gnd_Charset(GVIIndexer callback) {
      main = callback;
   }

   /**
    * Wrapper to {@link #expandGndTool(Record, String...)}<br>
    * (SolrMarc's reflection can't resolve varargs)
    * 
    * @param record The current data record
    * @param tagStr1 The (sub)fields to expand
    * @param tagStr2 Additional (sub)fields to expand.<br>
    *           Used for individual handling of marc:689
    * @return The found expansions
    */
   public Set<String> expandGnd(Record record, String tagStr1, String tagStr2) {
      return expandGndTool(record, tagStr1, tagStr2);
   }

   /**
    * Wrapper to {@link #expandGndTool(Record, String...)}<br>
    * (SolrMarc's reflection can't resolve varargs)
    * 
    * @param record The current data record
    * @param tagStr The (sub)fields to expand
    * @return The found expansions
    */
   public Set<String> expandGnd(Record record, String tagStr) {
      return expandGndTool(record, tagStr);
   }

   /**
    * Extent the category tags with their version in original writing
    * 
    * @param record The current marc record
    * @param tagString List of (sub)fields to examine (see {@link #getFieldList(Record, String)}
    * @return The result of {@link #getFieldList(Record, String)} extended by original writing.
    */
   public Set<String> getAllCharSets(Record record, String tagString) {
      Set<String> resultList = main.getFieldList(record, tagString);
      synchronized (cached880Fields) { // don't get screwed by parallelism
         if (record != cachedRecord) { // Fetch the marc:880 fields only once
            cached880Fields = get880Fields(record); // Fetch
            cachedRecord = record; // set reference
         }
         for (String catId : expandTagString(tagString)) {
            Set<String> originalWriting = cached880Fields.get(catId);
            if (originalWriting != null) {
               resultList.addAll(originalWriting);
            }
         }
      }
      return resultList;
   }

   /**
    * Checks if the requested field(s) are starting with the given prefix.<br>
    * E.g. Look in relation fields for GND_IDs (starting with "DE-588")
    * 
    * @param record Marc data
    * @param tagStr Marc fields to check
    * @param prefixStr Needed prefix
    * @param keepPrefixStr If FALSE remove the prefix in the response
    * @return The found values.
    */
   public Set<String> getTermID(Record record, String tagStr, String prefixStr, String keepPrefixStr) {
      boolean keepPrefix = Boolean.parseBoolean(keepPrefixStr);
      Set<String> candidates = main.getFieldList(record, tagStr);
      Set<String> result = new HashSet<>();
      for (String candidate : candidates) {
         if (candidate.contains(prefixStr)) {
            result.add(keepPrefix ? candidate : candidate.substring(prefixStr.length() + 2));
         }
      }
      return result;
   }

   /**
    * Expand GND synonyms from provided finder<br>
    *
    * @param record The current data record
    * @param tagArr The (sub)fields to expand
    * @return The found expansions
    */
   private Set<String> expandGndTool(Record record, String... tagArr) {
      Set<String> alreadyProcessed = new HashSet<>();
      Set<String> result = new HashSet<>();
      for (String tagStr : tagArr) {
         if (tagStr.length() < 4) tagStr += "0"; // if needed add subfield '0'
         for (String testId : main.getFieldList(record, tagStr)) {
            if (!testId.startsWith("(DE-588)")) continue; // nur GND nutzen
            if (alreadyProcessed.contains(testId)) continue; // only once
            alreadyProcessed.add(testId);
            String normData = Init.gndSynonymMap.getProperty(testId);
            if (normData == null) { // wenn es keinen passenden Normdatensatz gibt, dann weiter
               if (LOG.isDebugEnabled()) LOG.debug(testId); 
               continue;
            }
            if (tagStr.startsWith("689")) { // workaround for RSWK
               // TODO "continue" if the type (subfield 'D') isn't 's'
            }
            String[] normDataParts = normData.split(gndLineSeperator);
            result.add(normDataParts[0]); // Bevorzugte Benennung übernehmen
            if (normDataParts.length > 1) { // Synonyme übernehmen
               for (int i = 1; i < normDataParts.length; i++) {
                  result.add(normDataParts[i]);
               }
            }
         }
      }
      return result;
   }

   /**
    * The 'tagString' of SolrMarc is a List of elements separated by a colon. Each of the elements is the number of a marc field followed by one or more subfield codes.<br>
    * This method inflates the list by normalizing tags with more codes to multiple tags with only on code.<br>
    * Example: "111a:222bc" --> "111a:222b:222c"
    * 
    * @param tagString The unchanged tag string from the call in 'index.properties'
    * @return A inflated, but semantically identical set of normalized tags
    */
   private Set<String> expandTagString(String tagString) {
      Set<String> tagSet = new HashSet<>();
      for (String rawTag : tagString.split(":")) {
         int length = rawTag.length();
         if (length == 4) tagSet.add(rawTag); // simple case
         else if (length > 4) {
            String fieldId = rawTag.substring(0, 3);
            for (int pos = 3; pos < length; pos++) {
               tagSet.add(fieldId + rawTag.charAt(pos));
            }
         }
         else if (length == 3) tagSet.add(rawTag + 'a'); // add most common subfield code
      }
      return tagSet;
   }

   /**
    * Get all all original writing data from this record. The method sorts all marc:880$a fields as value into the result {@link Map}.<br>
    * The key of the map are the first three chars of marc:880$6, which is the id of the related marc field.<br>
    * Since the marc:880 fields are repeatable for one reference the value has to be a {@link Set}
    * 
    * @param record The current marc record
    * @return A map<reference, related originale writings> or NULL if no marc:880 is found.
    */
   private Map<java.lang.String, Set<java.lang.String>> get880Fields(Record record) {
      Map<String, Set<String>> originalWriting = new HashMap<>();
      List<VariableField> originalFields = record.getVariableFields("880");
      if ((originalFields == null) || originalFields.isEmpty()) return originalWriting;
      for (VariableField v_field : originalFields) {
         DataField field = (DataField) v_field;
         Subfield ref = field.getSubfield('6');
         if (ref == null) continue; // skip corrupted data
         String fieldId = ref.getData();
         if (fieldId.length() < 3) continue; // skip corrupted data
         fieldId = fieldId.substring(0, 3);

         for (Subfield subfield : field.getSubfields()) {
            char subFieldCode = subfield.getCode();
            if (subFieldCode == '6') continue;
            String subFieldData = subfield.getData();
            if (subFieldData == null) continue; // skip corrupted data
            String key = fieldId + subFieldCode;
            Set<String> dataSet = originalWriting.get(key);
            if (dataSet == null) { // for the first entry put a new Set
               dataSet = new HashSet<>();
               originalWriting.put(key, dataSet);
            }
            dataSet.add(subFieldData); // add data
         }
      }
      return originalWriting;
   }

}
