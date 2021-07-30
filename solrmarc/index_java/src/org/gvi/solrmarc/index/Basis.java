package org.gvi.solrmarc.index;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.gvi.solrmarc.index.enums.IllFlag;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;
import org.solrmarc.index.SolrIndexer;
import org.solrmarc.tools.DataUtil;

public class Basis extends SolrIndexer {

   private static final Logger LOG                     = LogManager.getLogger(Basis.class);
   private String              collectionFromParameter = System.getProperty("data.collection", "UNDEFINED");
   private String              myCatalog               = null;
   private Record              myCatalogIsFor          = null;

   public Basis(String indexingPropsFile, String[] propertyDirs) {
      super(indexingPropsFile, propertyDirs);
   }

   /**
    * Count the number of 924 fields.<br>
    * <dl>
    * <dt>History</dt>
    * <dd>uh, 2021-07-13, initial version</dd>
    * </dl>
    * 
    * @param record
    * @return
    */
   public int countHoldingLibraries(Record record) {
      int count = 0;
      List<VariableField> fields = record.getVariableFields("924");
      if (fields != null) {
         Iterator<VariableField> iterator = fields.iterator();
         while (iterator.hasNext()) {
            count++;
         }
      }
      return count;
   }

   /**
    * Return always TRUE<br>
    * Use his method, when just the presence of a marc field is the information.
    * 
    * @param record
    * @return
    */
   public boolean detectLinkToEnrichment(Record record) {
      return true;
   }

   public String getRecordID(final Record record) {
      String catalogId = getCatalogId(record);
      String localId = getFirstFieldVal(record, "001");
      if (catalogId.equals("AT-OBV")) {
         localId = getFirstFieldVal(record, "009");
         if (localId == null) localId = getFirstFieldVal(record, "001");
         else if (localId.length() == 0) localId = getFirstFieldVal(record, "001");
      }
      return "(" + catalogId + ")" + localId;
   }

   /**
    * Get the catalog's id fore the record<br>
    * The id is cached evaluated only once per record by {@link #findCatalog(Record)}
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
    * Stub more advanced version of getDate that looks in the 008 field as well as the 260c field this routine does some simple sanity checking to ensure that the date to return makes sense.
    *
    * @param record - the marc record object
    * @return 260c or 008[7-10] or 008[11-14], "cleaned" per org.solrmarc.tools.Utils.cleanDate()
    */
   public String getPublicationDate008or26xc(final Record record) {
      String field008 = getFirstFieldVal(record, "008");
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
    * Get the licenced year(s) Feld 912, Kennzeichnungen für Nationallizenzen und digitale Sammlungen TODO Bei Bereichen auch die dazwischen liegenden Jahre berücksichtigen?
    * 
    * @param record
    * @return
    */
   public Set<String> getProductYear(final Record record) {
      Set<String> values912b = getFieldList(record, "912b");
      Set<String> productYears = new HashSet<>();
      for (String yearExpr : values912b) {
         String yearExprs[] = yearExpr.replaceAll("[^0-9,^\\-,^\\,]", "").split(",");
         for (String y : yearExprs) {
            String range[] = y.split("\\-", -1);
            String year = DataUtil.cleanDate(range[0]);
            if (year != null) {
               productYears.add(year);
            }

            if (range.length > 1) {
               year = DataUtil.cleanDate(range[1]);
               if (year != null) {
                  productYears.add(year);
               }
            }
         }
      }
      return productYears;
   }

   /**
    * Extension for VuFind clients.<br>
    * The interpretation of perfect MARC may be depend on the source.<br>
    * Having a own 'recordtyp' may help to select a tailored record driver.<br>
    * See GVI-87
    * 
    * @param record
    * @return The concatination of "GviMarc_" and the isil of the source.
    */
   public String getMarcTypByConsortium(final Record record) {
      String catalogId = getCatalogId(record);
      if ((catalogId == null) || (catalogId.length() < 4)) return "GviMarcUnknown";
      if ((catalogId.equals("AT-OBV"))) return "GviMarcATOBV";
      return "GviMarcDE" + catalogId.substring(3);
   }

   public Set<String> getConsortium(final Record record) {
      return findConsortium(record, getCatalogId(record));
   }

   /**
    * Split string with comma separated List of entries into a set of (string)entries<br>
    * 
    * @param record
    * @param tagStr
    * @return
    */
   public Set<String> splitSubfield(Record record, String tagStr) {
      Set<String> result = new HashSet<>();
      Set<String> fieldList = getFieldList(record, tagStr);
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
    * Return the date in 260c/264c as a string
    *
    * @param record - the marc record object
    * @return 260c/264c, "cleaned" per org.solrmarc.tools.Utils.cleanDate()
    */
   private String getDate26xc(Record record) {
      String date260c = getFieldVals(record, "260c", ", ");
      String date264c = getFieldVals(record, "264c", ", ");
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

   private String findCatalog(final Record record) {
      String f001 = getFirstFieldVal(record, "001");
      String catalogId = "UNSET";
      // guess catalogId
      String field003 = getFirstFieldVal(record, "003");
      String field040a = getFirstFieldVal(record, "040a");
      if (collectionFromParameter.equals("ZDB")) {
         catalogId = "DE-600";
      }
      else if (collectionFromParameter.equals("HBZFIX")) {
         catalogId = "DE-605";
      }
      else if (collectionFromParameter.equals("OBV")) {
         catalogId = "AT-OBV";
      }
      else if (field003 != null) {
         if (field003.length() > 6) {
            field003 = field003.substring(0, 6);
         }
         catalogId = field003;
      }
      else if (field040a != null) {
         catalogId = field040a;
      }
      else if (f001 != null && f001.startsWith("BV")) {
         catalogId = "DE-604";
      }
      else {
         catalogId = "UNDEFINED";
      }
      return catalogId;
   }

   private Set<String> findConsortium(Record record, String catalogId) {
      Set<String> consortiumSet = new HashSet<>();
      String collection = System.getProperty("data.collection", "UNDEFINED");
      switch (catalogId) {
         case "AT-OBV":
            consortiumSet.add("AT-OBV");
            break;
         case "DE-101": // DNB
            if (collection.equals("ZDB")) {
               consortiumSet.add("DE-600");
            }
            else {
               consortiumSet.add(catalogId);
            }
            break;
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
            Set<String> regions = getFieldList(record, "924c");
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

}
