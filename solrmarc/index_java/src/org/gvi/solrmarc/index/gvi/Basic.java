package org.gvi.solrmarc.index.gvi;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.gvi.solrmarc.index.GVIIndexer;
import org.gvi.solrmarc.index.gvi.enums.IllFlag;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;
import org.solrmarc.tools.DataUtil;

public class Basic {

   private static final Logger LOG                     = LogManager.getLogger(Basic.class);
   private GVIIndexer          main                    = null;
   private String              myCatalog               = "";
   private Record              myCatalogIsFor          = null;
   private String              collectionFromParameter = System.getProperty("data.collection", "UNDEFINED");

   public Basic(GVIIndexer callback) {
      main = callback;
   }

   /**
    * Get the number of 924 fields.<br>
    * <dl>
    * <dt>History</dt>
    * <dd>uh, 2021-07-13, initial version</dd>
    * </dl>
    * 
    * @param record
    * @return
    */
   public String getHoldingCount(Record record) {
      List<VariableField> fields = record.getVariableFields("924");
      if (fields == null) {
         return "0";
      }
      return String.valueOf(fields.size());
   }

   /**
    * Collect distinct the indicator2 values for the fields marc:856<br>
    * Second Indicator: Relationship:
    * <dl>
    * <dd># - No information provided</dd>
    * <dd>0 - Resource</dd>
    * <dd>1 - Version of resource</dd>
    * <dd>2 - Related resource</dd>
    * <dd>8 - No display constant generated</dd>
    * </dl>
    * 
    * @param record
    * @return the different found indicators.
    */
   public Set<String> getEnrichmentTypes(Record record) {
      Set<String> result = new HashSet<>();
      List<VariableField> fields_856 = record.getVariableFields("856");
      if (fields_856 == null) return result;
      for (VariableField field : fields_856) {
         DataField dataField = (DataField) field;
         result.add(String.valueOf(dataField.getIndicator2()));
      }
      return result;
   }

   /**
    * Build the GVI-ID for the Record.<br>
    * 
    * @param record
    * @return The composed id: The ISIL of the catalog in brackets and the local id. "(DE-601)123456"
    */
   public String getRecordId(final Record record) {
      String localId = null;
      String catalogId = getCatalogId(record);
      String collection = getCollection(record);
      if ("AT-OBV".equals(catalogId)) {
         localId = main.getFirstFieldVal(record, "009");
      }
      if ("DE-605".equals(catalogId) && !"HBZFIX".equals(collection)) {
          List<VariableField> fields_035 = record.getVariableFields("035");
          for (VariableField field: fields_035) {
              DataField dataField = (DataField) field;
              String id = dataField.getSubfield('a').getData();
              if (id.startsWith("(DE-605)")) {
                  localId = id.substring(8);
                  break;
              }
          }
          if (localId == null || localId.isEmpty()) {
            for (VariableField field: fields_035) {
                DataField dataField = (DataField) field;
                String id = dataField.getSubfield('a').getData();
                if (id.startsWith("(DE-600)")) {
                    localId = id;
                    break;
                }
            }
          }
          if (localId == null || localId.isEmpty()) {
            localId = "(UUID)"+UUID.randomUUID().toString().replace("-", "");
          }
      }
      if ((localId == null) || localId.isEmpty()) {
         localId = main.getFirstFieldVal(record, "001");
      }
      return "(" + catalogId + ")" + localId;
   }

   /**
    * Get the catalog's id for this record<br>
    * 
    * @param record
    * @return
    */
   public String getCatalogId(final Record record) {
      if ((record != myCatalogIsFor) || (myCatalog == null)) synchronized (myCatalog) {
         myCatalogIsFor = record;
         myCatalog = findCatalog(record);
      }
      return myCatalog;
   }

   /**
    * The collection is defined externally, by passing the parameter 'data.collection' to the JVM.<br>
    * This system parameter is represented as static field of this class.
    * 
    * @param record
    * @return The mnemomic of the current collection.
    */
   public String getCollection(final Record record) {
      return collectionFromParameter;
   }

   /**
    * Get set of other_ids. For HBZ (DE-605) add (DE-605)$001. This is used for deletion. <br>
    * @param record
    * @param tagStr List of fields
    * @return Set of other_ids
    */
   public  Set<String> getOtherId(final Record record, String tagStr) {
      Set<String> result = new HashSet<>();
      Set<String> fieldList = main.getFieldList(record, tagStr);
      for (String str : fieldList) {
         result.add(str);
      }
      // For HBZ (DE-605) add (DE-605)$001. This is used for deletion.
      String catalogId = getCatalogId(record);
      if ("DE-605".equals(catalogId)) {
          String localId = main.getFirstFieldVal(record, "001");
          result.add("(" + catalogId + ")" + localId);
      }
      return result;
   }
   /**
    * Get value(s) of selected classification schema
    * 
    * @param record The title data
    * @param notationField The marc field to inspect
    * @param schemaName The name of the classification schema
    * @return value(s) of subfield 'a' of the given field if subfield '2' matches the schemaName.<br>
    *         If no entry was found, an empty list will be returned
    */
   public Set<String> getClassification(Record record, String notationField, String schemaName) {
      Set<String> ret = new HashSet<>();
      List<VariableField> fields = record.getVariableFields(notationField);
      if (fields != null) {
         for (VariableField candidat : fields) {
            Subfield schemaSubField = ((DataField) candidat).getSubfield('2');
            if (schemaSubField == null) continue;
            if (schemaName.equals(schemaSubField.getData())) {
               List<Subfield> notationSubFields = ((DataField) candidat).getSubfields('a');
               for (Subfield notationSubField : notationSubFields) {
                  if (notationSubField == null) continue;
                  String notation = notationSubField.getData();
                  if ((notation == null) || notation.isEmpty()) continue;
                  ret.add(notation);
               }
            }
         }
      }
      return ret;
   }

   /**
    * Determine ILL (Inter Library Loan) Flag<br>
    * The values are defined in {@link IllFlag}
    * <dl>
    * <dt>History</dt>
    * <dd>tk, 2021-07-13, initial version</dd>
    * <dd>uh, 2021-07-13, redesign with new codes</dd>
    * </dl>
    * 
    * @param record
    * @return Set ill facets
    */

   public Set<String> getIllFlag(Record record) {
      // 1. Die angegebenenen Ausleihindikatoren in HashSet sammeln.
      Set<Character> bucket = new HashSet<>();
      List<VariableField> fields = record.getVariableFields("924");
      if (fields != null) {
         Iterator<VariableField> iterator = fields.iterator();
         while (iterator.hasNext()) {
            DataField field = (DataField) iterator.next();
            if (null != field.getSubfield('d')) {
               String data = field.getSubfield('d').getData();
               if ((data == null) || data.isEmpty()) continue;
               char ausleihindikator = data.toLowerCase().charAt(0);
               if ((ausleihindikator >= 'a') && (ausleihindikator <= 'e')) { // nur definierte Indikatoren.
                  bucket.add(ausleihindikator);
               }
            }
         }
      }
      // 2. Die gefundenen Ausleihindikatoren in Text übersetzen.
      Set<String> ret = new HashSet<>();
      if (bucket.isEmpty()) { // keine marc:924d gefunden.
         ret.add(IllFlag.Undefined.toString());
      }
      else if ((bucket.size() == 1) && bucket.contains('d')) {
         // wenn nur der indikator 'd' (keine Fernleihe) gefunden wurde
         ret.add(IllFlag.None.toString());
      }
      else { // die verbleibenden Codierungen auswerten
         if (bucket.contains('a')) { // Ausleihe von Bänden möglich, keine Kopien
            ret.add(IllFlag.Loan.toString());
         }
         if (bucket.contains('b')) { // Keine Ausleihe von Bänden, nur Papierkopien werden versandt
            ret.add(IllFlag.Copy.toString());
         }
         if (bucket.contains('c')) { // Uneingeschränkte Fernleihe
            ret.add(IllFlag.Loan.toString());
            ret.add(IllFlag.Copy.toString());
            // 'e' wird von Bibliothekaren als noch nicht eingschlossener Sonderfall angesehen
            // ret.add(IllFlag.Ecopy.toString());
         }
         if (bucket.contains('e')) { // Keine Ausleihe von Bänden, der Endnutzer erhält eine elektronische Kopie
            ret.add(IllFlag.Ecopy.toString());
         }
      }
      return ret;
   }

   /**
    * Try to extract the ZDB id of the document (journal)
    * 
    * @param record
    * @return
    */
   public Set<String> getZdbId(Record record) {
      Set<String> result = new LinkedHashSet<>();
      // 016 - National Bibliographic Agency Control Number
      List<VariableField> fields = record.getVariableFields("016");
      if (fields != null) {
         Iterator<VariableField> iterator = fields.iterator();
         while (iterator.hasNext()) {
            DataField field = (DataField) iterator.next();
            Subfield dataSource = field.getSubfield('2');
            if (dataSource == null) continue;
            if (dataSource.getData() == null) continue;
            if (dataSource.getData().isEmpty()) continue;
            if (dataSource.getData().equals("DE-600")) {
               Subfield number = field.getSubfield('a');
               if (number == null) continue;
               String zdbId = number.getData();
               if (zdbId == null) continue;
               if (zdbId.isEmpty()) continue;
               result.add(zdbId);
               return result; // There can only be one!
            }
         }
      }
      return result;
   }

   /**
    * Return the date in 260c/264c as a string
    *
    * @param record - the marc record object
    * @return 260c/264c, "cleaned" per org.solrmarc.tools.Utils.cleanDate()
    */
   public String getDate26xc(Record record) {
      String date260c = main.getFieldVals(record, "260c", ", ");
      String date264c = main.getFieldVals(record, "264c", ", ");
      String date = null;
      if (date260c != null && date260c.length() > 0) {
         date = date260c;
      }
      else if (date264c != null && date264c.length() > 0) {
         date = date264c;
      }
      if (date == null || date.length() == 0) {
         return (null);
      }
      return DataUtil.cleanDate(date);
   }

   /**
    * Stub more advanced version of getDate that looks in the 008 field as well as the 260c field this routine does some simple sanity checking to ensure that the date to return makes sense.
    *
    * @param record - the marc record object
    * @return 260c or 008[7-10] or 008[11-14], "cleaned" per org.solrmarc.tools.Utils.cleanDate()
    */
   public String getPublicationDate(final Record record) {
      String field008 = main.getFirstFieldVal(record, "008");
      String pubDate26xc = getDate26xc(record);
      String pubDate26xcJustDigits = null;

      if (pubDate26xc != null) {
         pubDate26xcJustDigits = pubDate26xc.replaceAll("[^0-9]", "");
      }

      if (field008 == null || field008.length() < 16) {
         return (pubDate26xc);
      }

      String field008_d1 = field008.substring(7, 11);
      String field008_d2 = field008.substring(11, 15);

      String retVal = null;
      char dateType = field008.charAt(6);
      if (dateType == 'r' && field008_d2.equals(pubDate26xc)) {
         retVal = field008_d2;
      }
      else if (field008_d1.equals(pubDate26xc)) {
         retVal = field008_d1;
      }
      else if (field008_d2.equals(pubDate26xc)) {
         retVal = field008_d2;
      }
      else if (pubDate26xcJustDigits != null && pubDate26xcJustDigits.length() == 4 && pubDate26xc != null && pubDate26xc.matches("(20|19|18|17|16|15)[0-9][0-9]")) {
         retVal = pubDate26xc;
      }
      else if (field008_d1.matches("(20|1[98765432])[0-9][0-9]")) {
         retVal = field008_d1;
      }
      else if (field008_d2.matches("(20|1[98765432])[0-9][0-9]")) {
         retVal = field008_d2;
      }
      else {
         retVal = pubDate26xc;
      }
      return (retVal);
   }

   /**
    * Get the licensed year(s) of products from marc:912b<br>
    * * Aug. 2021 only used at K10plus.<br>
    * * Definition is still on discussion at the AG-KVA
    * 
    * @param record
    * @return The year or all years of date ranges
    */
   public Set<String> getProductYear(final Record record) {
      Set<String> productYears = new HashSet<>();
      Set<String> values912b = main.getFieldList(record, "912b");
      if (values912b == null) {
         return productYears;
      }
      for (String yearEntry : values912b) {
         String yearExpressions[] = yearEntry.split(",");
         for (String expression : yearExpressions) {
            String dateRange[] = expression.split("-");
            switch (dateRange.length) {
               case 1:
                  addDate(productYears, dateRange[0]);
                  break;
               case 2:
                  String anfang = DataUtil.cleanDate(dateRange[0]);
                  if (anfang == null) break;
                  String ende = DataUtil.cleanDate(dateRange[1]);
                  if (ende == null) break;
                  for (int year = Integer.valueOf(anfang); year <= Integer.valueOf(ende); year++) {
                     addDate(productYears, String.valueOf(year));
                  }
                  break;
               default:
                  LOG.warn("Fehlerhafte Angaben zu Produktjahr (912b) in Titel: " + getRecordId(record));
            }
         }
      }
      return productYears;
   }

   /**
    * Helper to {@link #getProductYear(Record)}<br>
    * 
    * @param years Set to add the validated year from 'rawDate'
    * @param rawDate Year to be validated and cleaned
    * @return The probably modified set 'years'
    */
   private Set<String> addDate(Set<String> years, String rawDate) {
      String year = DataUtil.cleanDate(rawDate);
      if (year != null) {
         years.add(year);
      }
      return years;
   }

   // String yearExprs[] = yearExpr.replaceAll("[^0-9,^\\-,^\\,]", "").split(",");

   /**
    * Extension for VuFind clients.<br>
    * The interpretation of perfect MARC may be depend on the source.<br>
    * Having a own 'recordtyp' may help to select a tailored record driver.<br>
    * See GVI-87
    * 
    * @param record
    * @return The concatenation of "GviMarc_" and the ISIL of the source.
    */
   public String getMarcTypByConsortium(final Record record) {
      String catalogId = getCatalogId(record);
      if ((catalogId == null) || (catalogId.length() < 4)) return "GviMarcUnknown";
      if ((catalogId.equals("AT-OBV"))) return "GviMarcATOBV";
      return "GviMarcDE" + catalogId.substring(3);
   }

   /**
    * Find the ISIL of the Consortium.<br>
    * 
    * @param record
    * @return
    */
   public Set<String> getConsortium(final Record record) {
      String catalogId = getCatalogId(record);
      Set<String> consortiumSet = new HashSet<>();
      switch (catalogId) {
         case "DE-101": // DNB
            if ("ZDB".equals(collectionFromParameter)) {
               consortiumSet.add("DE-600");
            }
            else {
               consortiumSet.add(catalogId);
            }
            break;
         case "AT-OBV":
         case "DE-576": // SWB
         case "DE-600": // ZDB
         case "DE-601": // GBV+KOBV+ZDB
         case "DE-602": // KOBV
         case "DE-603": // HEBIS
         case "DE-604": // BVB
         case "DE-605": // HBZ
            consortiumSet.add(catalogId);
            break;
         case "DE-627": // K10Plus
            Set<String> regions = main.getFieldList(record, "924c");
            if (regions.contains("GBV")) consortiumSet.add("DE-601");
            if (regions.contains("BSZ")) consortiumSet.add("DE-576");
            if (consortiumSet.isEmpty()) consortiumSet.add("DE-627");
            break;
         default:
            consortiumSet.add("UNDEFINED");
            break;
      }
      return consortiumSet;
   }

   /**
    * Split string with comma separated List of entries into a set of (string)entries<br>
    * 
    * @param record
    * @param tagStr List of fields
    * @return
    */
   public Set<String> splitSubfield(Record record, String tagStr) {
      Set<String> result = new HashSet<>();
      Set<String> fieldList = main.getFieldList(record, tagStr);
      for (String str : fieldList) {
         String[] subStr = str.split(",");
         for (int i = 0; i < subStr.length; i++) {
            String term = subStr[i].trim();
            if (term.length() > 0) {
               result.add(term);
            }
         }
      }
      return result;
   }

   /**
    * Set a new collection id<br>
    * The cached catalog id is invalidated too. So the next call of {@link #getCatalogId(Record)} has to call {@link #findCatalog(Record)}.<br>
    * This method is intended for Junit tests only.
    */
   public void setCollection(String collectionId) {
      // patch the value
      collectionFromParameter = collectionId;
      // Invalidate cached catalog Id
      myCatalogIsFor = null;
   }

   /**
    * Helper to {@link #findDateIn26x(Record, String)}
    * 
    * @param record
    * @param fieldName
    * @return
    */
   String findDateInX(Record record, String fieldName) {
      String date = main.getFieldVals(record, fieldName, ", ");
      return (date == null) ? null : DataUtil.cleanDate(date);
   }

   String findCatalog(final Record record) {
      String f001 = main.getFirstFieldVal(record, "001");
      // guess catalogId
      String field003 = main.getFirstFieldVal(record, "003");
      String field040a = main.getFirstFieldVal(record, "040a");
      if (collectionFromParameter.equals("ZDB")) {
         return "DE-600";
      }
      if (collectionFromParameter.equals("HBZFIX")) {
         return "DE-605";
      }
      if (collectionFromParameter.equals("OBV")) {
         return "AT-OBV";
      }
      if (field003 != null) {
         if (field003.length() > 6) {
            field003 = field003.substring(0, 6);
         }
         return field003; // default case
      }
      if (field040a != null) {
         return field040a; //
      }
      if (f001 != null && f001.startsWith("BV")) {
         return "DE-604";
      }
      return "UNDEFINED";
   }

}
