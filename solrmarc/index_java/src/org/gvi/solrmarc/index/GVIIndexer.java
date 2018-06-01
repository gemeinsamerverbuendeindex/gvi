package org.gvi.solrmarc.index;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.FileInputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.gvi.solrmarc.normalizer.ISBNNormalizer;
import org.gvi.solrmarc.normalizer.impl.PunctuationSingleNormalizer;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;
import org.solrmarc.index.SolrIndexer;
import org.solrmarc.mixin.GetFormatMixin;
import org.solrmarc.tools.Utils;

/**
 *
 * @author Thomas Kirchhoff <thomas.kirchhoff@bsz-bw.de>
 * @version 2017-06-26 uh, Synonyme aus File lesen und ergänzen
 * @version 2018-03-02 uh, GND-Synonyme vom remote repository lesen und ergänzen
 */
public class GVIIndexer extends SolrIndexer {

   private static final String                      institutionToConsortiumFile    = "translation_maps/kobv.properties";
   private static final Properties                  institutionToConsortiumMap     = new Properties();
   private static final String                      kobvInstitutionReplacementFile = "translation_maps/kobv_replacement.properties";
   private static final Properties                  kobvInstitutionReplacementMap  = new Properties();
   private static final String                      kobvClusterInfoFile            = "kobv_clusters.properties";
   private static final Properties                  kobvClusterInfoMap             = new Properties();
   private static final String                      gndSynonymFile                 = "gnd_synonyms.properties";
   private static final String                      gndLineSeperator               = "!_#_!";
   private static final Properties                  gndSynonymMap                  = new Properties();
   private static final PunctuationSingleNormalizer punctuationSingleNormalizer    = new PunctuationSingleNormalizer();
   private static boolean                           isInitialized                  = false;
   private static final Logger                      LOG                            = LogManager.getLogger(GVIIndexer.class);

   public GVIIndexer(String indexingPropsFile, String[] propertyDirs) throws Exception {
      super(indexingPropsFile, propertyDirs);
      init();
   }

   public GVIIndexer() throws Exception {
      super(null, null);
      init();
   }

   private synchronized void init() throws Exception {
      if (isInitialized) return;
      isInitialized = true;
      if (System.getProperty("GviIndexer.skipBigFiles") != null) return;

      institutionToConsortiumMap.load(new FileInputStream(institutionToConsortiumFile));
      kobvInstitutionReplacementMap.load(new FileInputStream(kobvInstitutionReplacementFile));

      if (LOG.isInfoEnabled()) {
         listMem();
         LOG.info("Loading of gnd synonymes started at: " + LocalDateTime.now().toString());
      }
      gndSynonymMap.load(new FileInputStream(gndSynonymFile));
      if (LOG.isInfoEnabled()) {
         LOG.info("Loading of gnd synonymes finished at: " + LocalDateTime.now().toString());
         listMem();
         LOG.info("Loading of cluster map started at: " + LocalDateTime.now().toString());
      }
      kobvClusterInfoMap.load(new FileInputStream(kobvClusterInfoFile));
      if (LOG.isInfoEnabled()) {
         LOG.info("Loading of cluster map finished at: " + LocalDateTime.now().toString());
         listMem();
      }
   }

   private void listMem() {
      for (MemoryPoolMXBean mpBean : ManagementFactory.getMemoryPoolMXBeans()) {
         if (mpBean.getType() == MemoryType.HEAP) {
            LOG.info(String.format("Name: %s max_used: %2.2fG now_used: %2.2fG (max_avail: %2.2fG)\n", mpBean.getName(), toGb(mpBean.getPeakUsage().getUsed()), toGb(mpBean.getUsage().getUsed()), toGb(mpBean.getUsage().getMax())));
         }
      }
   }

   private float toGb(long num) {
      float ret = num / 1024;
      return ret / 1024 / 1024;
   }

   public String matchkeyISBN(Record record) {
      String isbn = getFirstFieldVal(record, "020a");
      if (isbn != null) {
         isbn = ISBNNormalizer.normalize(isbn);
      } else {
         isbn = "";
      }
      return isbn;
   }

   public String matchkeyMaterial(Record record) {
      String material = "";

      GetFormatMixin formatMixin = new GetFormatMixin();
      Set<String> contentTypes = formatMixin.getContentTypesAndMediaTypesMapped(record, "getformat_mixin_map.properties");
      String materialForm = getMaterialForm(record);

      // Thesis
      if (contentTypes.contains("Thesis/Dissertation")) {
         material = "thesis";
      }
      // EJournal
      else if (contentTypes.contains("Journal/Magazine") && contentTypes.contains("Online")) {
         material = "ejournal";
      }
      // Journal
      else if (contentTypes.contains("Journal/Magazine")) {
         material = "journal";
      }
      // E-Book
      else if (contentTypes.contains("EBook")) {
         material = "ebook";
      }
      // Book
      else if (contentTypes.contains("Book")) {
         material = "book";
      }
      // Article
      else if (contentTypes.contains("Article")) {
         material = "article";
      }
      // Musical Score
      else if (contentTypes.contains("Musical Score")) {
         material = "music";
      }
      // Sound
      else if (contentTypes.contains("Sound Recording")) {
         material = "sound";
      }
      // Video
      else if (contentTypes.contains("Video")) {
         material = "video";
      }
      // Map
      else if (contentTypes.contains("Map")) {
         material = "map";
      }
      // Mixed Materials
      else if (contentTypes.contains("Mixed Materials") && materialForm.equals("m")) {
         material = "book";
      } else if (contentTypes.contains("Mixed Materials")) {
         material = "mixed";
      }
      // Undetermined
      else {
         material = "other";
      }

      return material;
   }

   public String matchkeyAuthor(Record record) {
      String firstAuthor = getFirstFieldVal(record, "100a:110a:111a:700a:710a:711a:245c");
      String lastName = "";
      if (firstAuthor != null) {
         String[] nameParts = firstAuthor.split("[, ]+");
         if (nameParts.length > 0) { // yes in some titles the given author is "," (sik)
            lastName = punctuationSingleNormalizer.normalize(nameParts[0].toLowerCase());
            lastName = lastName.replaceAll(" ", "");
         }
      }
      return lastName;
   }

   public String matchkeyPublisher(Record record) {
      String publisherKey = "";
      String publisher = getFirstFieldVal(record, "260b:264b:502c");
      if (publisher != null) {
         publisher = publisher.replaceAll("[Vv]erlag", "");
         publisher = publisher.replaceAll("[Vv]erl[\\.]", "");
         publisher = publisher.replaceAll("\\.", "");
         publisherKey = extractWords(publisher, 2);
      }
      return publisherKey;
   }

   public String matchkeyPubdate(Record record) {
      String pubdateKey = getPublicationDate008or26xc(record);
      if (pubdateKey == null) {
         pubdateKey = "";
      }
      return pubdateKey;
   }

   public String extractWords(String text, int nwords) {
      String result = "";
      if (text != null) {
         text = punctuationSingleNormalizer.normalize(text);
         text = text.replaceAll("§", "");
         String[] words = text.split(" ");
         int maxWord = Math.min(nwords, words.length);
         for (int i = 0; i < maxWord; i++) {
            result += words[i];
         }
      }
      return result;
   }

   public String matchkeyTitle(Record record) {
      String title = "";
      String mainTitle = getSortableMainTitle(record);
      if ((mainTitle != null) && !mainTitle.isEmpty()) {
         title = extractWords(mainTitle, 5);
      }
      return title;
   }

   public String matchkeyNumParts(Record record) {
      String volume = "";
      String field = getFirstFieldVal(record, "245n:800n:810n:811n:830n");
      if (field != null) {
         volume = punctuationSingleNormalizer.normalize(field);
      }
      return volume;
   }

   public String matchkeyVolume(Record record) {
      String volume = "";
      String field = getFirstFieldVal(record, "800v:810v:811v:830v");
      if (field != null) {
         volume = punctuationSingleNormalizer.normalize(field);
      }
      return volume;
   }

   public String matchkeyMaterialISBNYear(Record record) {
      String material = matchkeyMaterial(record);
      String isbn = matchkeyISBN(record);
      String pubdate = matchkeyPubdate(record);
      if (!isbn.isEmpty()) {
         return String.format("%s:%s:%s", material, isbn, pubdate);
      }
      return matchkeyMaterialAuthorTitleYearPublisher(record);
   }

   public String matchkeyMaterialAuthorTitle(Record record) {
      String matchkey = null;
      String material = null;
      String author = null;
      String title = null;
      String volume = null;
      String hostTitle = null;
      String relatedPart = null;

      try {
         material = matchkeyMaterial(record);
         author = matchkeyAuthor(record);
         title = matchkeyTitle(record);
         matchkey = String.format("%s:%s:%s", material, author, title);
         if (material.equals("map")) {
            volume = matchkeyVolume(record);
            if (!volume.isEmpty()) {
               matchkey = String.format("%s:%s", matchkey, volume);
            }
         } else if (material.equals("article")) {
            hostTitle = extractWords(getFirstFieldVal(record, "773t"), 3);
            relatedPart = extractWords(getFirstFieldVal(record, "773g"), 3);
            if (!hostTitle.isEmpty()) {
               matchkey = String.format("%s:%s", matchkey, hostTitle);
            }
            if (!relatedPart.isEmpty()) {
               matchkey = String.format("%s:%s", matchkey, relatedPart);
            }
         }
      } catch (Throwable e) {
         LOG.error("MatchkeyException at record " + getRecordID(record), e);
      }

      return matchkey;
   }

   public String matchkeyMaterialAuthorTitleYear(Record record) {
      String matchkey = null;
      String pubdate = null;
      try {
         pubdate = matchkeyPubdate(record);
         matchkey = String.format("%s:%s", matchkeyMaterialAuthorTitle(record), pubdate);
      } catch (Throwable e) {
         LOG.error("MatchkeyException at record " + getRecordID(record), e);
      }
      return matchkey;
   }

   public String matchkeyMaterialAuthorTitleYearPublisher(Record record) {
      String publisher = matchkeyPublisher(record);
      String matchkey = String.format("%s:%s", matchkeyMaterialAuthorTitleYear(record), publisher);
      return matchkey;
   }

   public String getSortableMainTitle(Record record) {
      String title = "";
      DataField titleField = (DataField) record.getVariableField("245");
      if (titleField == null) {
         return "";
      }

      int nonFilingInt = 0;// getInd2AsInt(titleField);

      title = getFirstFieldVal(record, "245a");
      if (title != null) {
         title = title.toLowerCase();

         // Skip non-filing chars, if possible.
         if (title.length() > nonFilingInt) {
            title = title.substring(nonFilingInt);
         }
         return title;
      }
      LOG.debug("MatchkeyException because no title found at record " + getRecordID(record));
      return "";
   }

   /**
    * Lookup to get the duplicate key to the record's id
    * 
    * @param record The current data record
    * @return If found the duplicate key else the own id.
    */
   public String getDupId(Record record) {
      String id = getRecordID(record);
      return kobvClusterInfoMap.getProperty(id, id);
   }

   /**
    * Wrapper to {@link #expandGnd2(Record, String...)}<br>
    * (SolrMarc's reflection can't resolve varargs)
    * 
    * @param record The current data record
    * @param tagStr1 The (sub)fields to expand
    * @param tagStr2 Additional (sub)fields to expand.<br>
    *           Used for individual handling of marc:689
    * @return The found expansions
    */
   public Set<String> expandGnd(Record record, String tagStr1, String tagStr2) {
      return expandGnd2(record, tagStr1, tagStr2);
   }

   /**
    * Wrapper to {@link #expandGnd2(Record, String...)}<br>
    * (SolrMarc's reflection can't resolve varargs)
    * 
    * @param record The current data record
    * @param tagStr The (sub)fields to expand
    * @return The found expansions
    */
   public Set<String> expandGnd(Record record, String tagStr) {
      return expandGnd2(record, tagStr);
   }

   /**
    * Expand GND synonyms from provided finder<br>
    *
    * @param record The current data record
    * @param tagArr The (sub)fields to expand
    * @return The found expansions
    */
   private Set<String> expandGnd2(Record record, String... tagArr) {
      Set<String> alreadyProcessed = new HashSet<>();
      Set<String> result = new HashSet<>();
      for (String tagStr : tagArr) {
         if (tagStr.length() < 4) tagStr += "0"; // if needed add subfield '0'
         for (String testId : getFieldList(record, tagStr)) {
            if (!testId.startsWith("(DE-588)")) continue; // nur GND nutzen
            if (alreadyProcessed.contains(testId)) continue; // only once
            alreadyProcessed.add(testId);
            String normData = gndSynonymMap.getProperty(testId);
            if (normData == null) continue; // wenn es keinen passenden Normdatensatz gibt, dann weiter
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
    * Return the date in 260c/264c as a string
    *
    * @param record - the marc record object
    * @return 260c/264c, "cleaned" per org.solrmarc.tools.Utils.cleanDate()
    */
   public String getDate26xc(Record record) {
      String date260c = getFieldVals(record, "260c", ", ");
      String date264c = getFieldVals(record, "264c", ", ");
      String date = null;
      if (date260c != null && date260c.length() > 0) {
         date = date260c;
      } else if (date264c != null && date264c.length() > 0) {
         date = date264c;
      }
      if (date == null || date.length() == 0) {
         return (null);
      }
      return Utils.cleanDate(date);
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
      } else if (field008_d1.equals(pubDate26xc)) {
         retVal = field008_d1;
      } else if (field008_d2.equals(pubDate26xc)) {
         retVal = field008_d2;
      } else if (pubDate26xcJustDigits != null && pubDate26xcJustDigits.length() == 4 && pubDate26xc != null && pubDate26xc.matches("(20|19|18|17|16|15)[0-9][0-9]")) {
         retVal = pubDate26xc;
      } else if (field008_d1.matches("(20|1[98765432])[0-9][0-9]")) {
         retVal = field008_d1;
      } else if (field008_d2.matches("(20|1[98765432])[0-9][0-9]")) {
         retVal = field008_d2;
      } else {
         retVal = pubDate26xc;
      }
      return (retVal);
   }

   public Set<String> getProductYear(final Record record) {
      Set<String> values912b = getFieldList(record, "912b");
      Set<String> productYears = new HashSet<>();
      for (String yearExpr : values912b) {
         String yearExprs[] = yearExpr.replaceAll("[^0-9,^\\-,^\\,]", "").split(",");
         for (String y : yearExprs) {
            String range[] = y.split("\\-", -1);
            String year = Utils.cleanDate(range[0]);
            if (year != null) {
               productYears.add(year);
            }

            if (range.length > 1) {
               year = Utils.cleanDate(range[1]);
               if (year != null) {
                  productYears.add(year);
               }
            }
         }
      }
      return productYears;
   }

   public String getLocalId(final Record record) {
      return getFirstFieldVal(record, "001");
   }

   public String getCatalog(final Record record) {
      return findCatalog(record, getLocalId(record));
   }
   
   protected String getCollection(final Record record) {
      return System.getProperty("data.collection", "UNDEFINED");
   }
   public Set<String> getConsortium(final Record record) {
      String catalog = getCatalog(record);
      return findConsortium(record, catalog, findInstitutionID(record, catalog), institutionToConsortiumMap);
   }

   public Set<String> getInstitutionID(final Record record) {
      return findInstitutionID(record, getCatalog(record));
   }

   public String getRecordID(final Record record) {
      return "(" + getCatalog(record) + ")" + getLocalId(record);
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
      String catalog = getCatalog(record);
      if ((catalog == null) || (catalog.length() < 4)) return "GviMarcUnknown";
      return "GviMarcDE" + catalog.substring(3);
   }

   protected String findCatalog(Record record, String f001) {
      String catalog = "UNSET";
      String collection = getCollection(record);
      // guess catalog
      String field003 = getFirstFieldVal(record, "003");
      String field040a = getFirstFieldVal(record, "040a");
      if (collection.equals("ZDB")) {
         catalog = "DE-600";
      } else if (field003 != null) {
         if (field003.length() > 6) {
            field003 = field003.substring(0, 6);
         }
         catalog = field003;
      } else if (field040a != null) {
         catalog = field040a;
      } else if (f001 != null && f001.startsWith("BV")) {
         catalog = "DE-604";
      } else {
         catalog = "UNDEFINED";
      }
      return catalog;
   }

   protected Set<String> getKobvInstitutions(Record record) {
      Set<String> kobvInstitutions = new LinkedHashSet<>();
      Set<String> values049a = getFieldList(record, "049a");
      if (values049a == null) {
         return kobvInstitutions;
      }
      for (String value049a : values049a) {
         // In some cases (KobvIndex), the Sigel (ISIL) is followed by the internal uid.
         // The delimiter is then ';', which cannot be a part of the Sigel
         int semicolonIndex = value049a.indexOf(';');
         if (semicolonIndex > 0) {
            String isil = value049a.substring(0, semicolonIndex);
            // Additionally, Brandendurg VOEB delivers all isils of the partner libraries (comma separated)
            String[] isils = isil.split(",");
            for (String oneisil : isils) {
               if (!oneisil.isEmpty()) {
                  if (kobvInstitutionReplacementMap.containsKey(oneisil)) {
                     kobvInstitutions.add(kobvInstitutionReplacementMap.getProperty(oneisil));
                  } else {
                     kobvInstitutions.add(oneisil);
                  }
               }
            }
         } else {
            if (kobvInstitutionReplacementMap.containsKey(value049a)) {
               kobvInstitutions.add(kobvInstitutionReplacementMap.getProperty(value049a));
            } else {
               kobvInstitutions.add(value049a);
            }
         }
      }
      return kobvInstitutions;
   }

   protected Set<String> findInstitutionID(Record record, String catalogId) {
      Set<String> institution = new LinkedHashSet<>();
      switch (catalogId) {
         case "DE-576": // SWB
            institution.addAll(getFieldList(record, "924b"));
            break;
         case "DE-601": // GBV+KOBV
            institution.addAll(getFieldList(record, "924b"));
            break;
         case "DE-604": // BVB+KOBV
            institution.addAll(getFieldList(record, "049a"));
            break;
         case "DE-602": // KOBV
            institution.addAll(getKobvInstitutions(record));
            break;
         default:
            institution.add("UNDEFINED");
      }
      return institution;
   }

   protected Set<String> findConsortium(Record record, String catalog, Set<String> institutionSet, Properties institutionToConsortiumMap) {
      Set<String> consortiumSet = new HashSet<>();
      String collection = System.getProperty("data.collection", "UNDEFINED");
      switch (catalog) {
         case "DE-101": // DNB
            if (collection.equals("ZDB")) {
               consortiumSet.add("DE-600");
            } else {
               consortiumSet.add(catalog);
            }
            break;
         case "DE-601": // GBV+KOBV+ZDB
            consortiumSet.addAll(findConsortiumByInstitution(catalog, institutionSet, institutionToConsortiumMap));
            break;
         case "DE-576": // SWB
         case "DE-600": // ZDB
         case "DE-602": // KOBV
         case "DE-603": // HEBIS
         case "DE-604": // BVB
         case "DE-605": // HBZ
            consortiumSet.add(catalog);
            break;
         default:
            consortiumSet.add("UNDEFINED");
            break;
      }
      return consortiumSet;
   }

   protected Set<String> findConsortiumByInstitution(String defaultCatalog, Set<String> institutionSet, Properties institutionToConsortiumMap) {
      Set<String> consortiumSet = new HashSet<>();
      int numOtherConsortium = 0;
      for (String i : institutionSet) {
         if (institutionToConsortiumMap.containsKey(i)) {
            consortiumSet.add(institutionToConsortiumMap.getProperty(i));
            numOtherConsortium++;
         }
      }
      if (institutionSet.isEmpty() || institutionSet.size() > numOtherConsortium) {
         consortiumSet.add(defaultCatalog);
      }
      return consortiumSet;
   }

   public Set<String> getTermID(Record record, String tagStr, String prefixStr, String keepPrefixStr) {
      boolean keepPrefix = Boolean.parseBoolean(keepPrefixStr);
      Set<String> candidates = getFieldList(record, tagStr);
      Set<String> result = new HashSet<>();
      for (String candidate : candidates) {
         if (candidate.contains(prefixStr)) {
            result.add(keepPrefix ? candidate : candidate.substring(prefixStr.length() + 2));
         }
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
    * Determine ILL (Inter Library Loan) Flag
    *
    * @param record
    * @return Set ill facets
    */
   public Set<String> getIllFlag(Record record) {

      // a = Fernleihe (nur Ausleihe)
      // e = Fernleihe (Kopie, elektronischer Versand an Endnutzer möglich)
      // k = Fernleihe (Nur Kopie)
      // l = Fernleihe (Kopie und Ausleihe)
      // n = Keine Fernleihe
      Set<String> result = new HashSet<>();
      List<VariableField> fields = record.getVariableFields("924");
      if (fields != null) {
         Iterator<VariableField> iterator = fields.iterator();

         while (iterator.hasNext()) {
            DataField field = (DataField) iterator.next();
            if (null != field.getSubfield('d')) {
               String data = field.getSubfield('d').getData();
               String illCodeString = data.toUpperCase();
               char illCode = illCodeString.length() > 0 ? illCodeString.charAt(0) : 'U';
               switch (illCode) {
                  case 'U':
                     result.add(IllFlag.Undefined.toString());
                     if (result.size() > 1) {
                        result.remove(IllFlag.Undefined.toString());
                     }
                     break;
                  case 'N':
                     result.add(IllFlag.None.toString());
                     result.remove(IllFlag.Undefined.toString());
                     if (result.size() > 1) {
                        result.remove(IllFlag.None.toString());
                     }
                     break;
                  case 'A':
                     result.add(IllFlag.Loan.toString());
                     result.remove(IllFlag.Undefined.toString());
                     result.remove(IllFlag.None.toString());
                     break;
                  case 'E':
                     result.add(IllFlag.Copy.toString());
                     result.add(IllFlag.Ecopy.toString());
                     result.remove(IllFlag.Undefined.toString());
                     result.remove(IllFlag.None.toString());
                     break;
                  case 'K':
                     result.add(IllFlag.Copy.toString());
                     result.remove(IllFlag.Undefined.toString());
                     result.remove(IllFlag.None.toString());
                     break;
                  case 'L':
                     result.add(IllFlag.Copy.toString());
                     result.add(IllFlag.Loan.toString());
                     result.remove(IllFlag.Undefined.toString());
                     result.remove(IllFlag.None.toString());
                     break;
                  default:
                     if (!(result.contains(IllFlag.Copy.toString()) || result.contains(IllFlag.Ecopy.toString()) || result.contains(IllFlag.Loan.toString()))) {
                        result.add(IllFlag.Undefined.toString());
                     }
                     break;
               }
            }
         }
      }

      if (result.isEmpty()) {
         result.add(IllFlag.Undefined.toString());
      }

      return result;
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
    * Determine publication form of material
    *
    * @param record
    * @return Set material type of record
    */
   // public String getMaterialForm(Record record, String mapFileName )
   public String getMaterialForm(Record record) {
      char publicationForm = record.getLeader().marshal().charAt(7);
      // String materialForm = "material_form."+publicationForm;
      String materialForm = "" + publicationForm;

      /*
       * String mapName = null; try { mapName = loadTranslationMap(null, mapFileName); } catch (IllegalArgumentException e) { // TODO Auto-generated catch block e.printStackTrace(); } Map<String,
       * String> translationMap = findMap(mapName); String materialFormMapped = Utils.remap(materialForm, translationMap, true); return materialFormMapped;
       */
      return materialForm;
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

   public Set<String> getSubjectTopicalTerm(Record record, String tagStr) {
      return getSubject(record, tagStr, MARCSubjectCategory.TOPICAL_TERM);
   }

   public Set<String> getSubjectGeographicalName(Record record, String tagStr) {
      return getSubject(record, tagStr, MARCSubjectCategory.GEOGRAPHIC_NAME);
   }

   public Set<String> getSubjectGenreForm(Record record, String tagStr) {
      return getSubject(record, tagStr, MARCSubjectCategory.GENRE_FORM);
   }

   public Set<String> getSubjectPersonalName(Record record, String tagStr) {
      return getSubject(record, tagStr, MARCSubjectCategory.PERSONAL_NAME);
   }

   public Set<String> getSubjectCorporateName(Record record, String tagStr) {
      return getSubject(record, tagStr, MARCSubjectCategory.CORPORATE_NAME);
   }

   public Set<String> getSubjectMeetingName(Record record, String tagStr) {
      return getSubject(record, tagStr, MARCSubjectCategory.MEETING_NAME);
   }

   public Set<String> getSubjectUniformTitle(Record record, String tagStr) {
      return getSubject(record, tagStr, MARCSubjectCategory.UNIFORM_TITLE);
   }

   public Set<String> getSubjectChronologicalTerm(Record record, String tagStr) {
      return getSubject(record, tagStr, MARCSubjectCategory.CHRONOLOGICAL_TERM);
   }

   public Set<String> getSubject(Record record, String tagStr, MARCSubjectCategory subjectCategory) {
      Set<String> result = getFieldList(record, tagStr);
      result.addAll(getSubjectUncontrolled(record, subjectCategory));
      result.addAll(getSWDSubject(record, subjectCategory));
      return result;
   }

   public Set<String> getSubjectUncontrolled(Record record, MARCSubjectCategory subjectCategory) {
      Set<String> result = new LinkedHashSet<>();
      List<VariableField> fields = record.getVariableFields("653");
      if (fields != null) {
         Iterator<VariableField> iterator = fields.iterator();
         while (iterator.hasNext()) {
            DataField field = (DataField) iterator.next();
            if (field.getSubfield('a') != null) {
               final int ind2 = field.getIndicator2();
               MARCSubjectCategory marcSubjectCategory = MARCSubjectCategory.mapToMARCSubjectCategory(ind2);
               if (marcSubjectCategory.equals(subjectCategory)) {
                  List<Subfield> subjects = field.getSubfields('a');
                  for (Subfield subject : subjects) {
                     result.add(subject.getData());
                  }
               }
            }
         }
      }
      return result;
   }

   public Set<String> getSWDSubject(Record record, MARCSubjectCategory subjectCategory) {
      Set<String> result = new LinkedHashSet<>();
      List<VariableField> fields = record.getVariableFields("689");
      if (fields != null) {
         Iterator<VariableField> iterator = fields.iterator();
         while (iterator.hasNext()) {
            DataField field = (DataField) iterator.next();
            if (field.getSubfield('D') != null) {
               String gndCategoryString = field.getSubfield('D').getData();
               // GND Sachbegriff
               if (gndCategoryString != null && !gndCategoryString.isEmpty()) {
                  char gndCategory = field.getSubfield('D').getData().charAt(0);
                  MARCSubjectCategory marcSubjectCategory = GNDSubjectCategory.mapToMARCSubjectCategory(gndCategory);
                  if (marcSubjectCategory.equals(subjectCategory)) {
                     List<Subfield> subjects = field.getSubfields('a');
                     for (Subfield subject : subjects) {
                        result.add(subject.getData());
                     }
                  }
               }
            } else if (field.getSubfield('A') != null) {
               // Alter SWD Sachbegriff. Muss gemappt werden!
               String swdCategoryString = field.getSubfield('A').getData();
               if (swdCategoryString != null && !swdCategoryString.isEmpty()) {
                  char swdCategory = field.getSubfield('A').getData().charAt(0);
                  MARCSubjectCategory marcSubjectCategory = SWDSubjectCategory.mapToMARCSubjectCategory(swdCategory);
                  if (marcSubjectCategory.equals(subjectCategory)) {
                     List<Subfield> subjects = field.getSubfields('a');
                     for (Subfield subject : subjects) {
                        result.add(subject.getData());
                     }

                  }
               }
            }
         }
      }
      return result;
   }

   public Set<String> getZdbId(Record record) {
      Set<String> result = new LinkedHashSet<>();
      List<VariableField> fields = record.getVariableFields("016");
      if (fields != null) {
         Iterator<VariableField> iterator = fields.iterator();
         while (iterator.hasNext()) {
            DataField field = (DataField) iterator.next();
            Boolean isZDB = field.getSubfield('2') != null && field.getSubfield('2').getData() != null && field.getSubfield('2').getData().equals("DE-600");
            if (isZDB && field.getSubfield('a') != null) {
               result.add(field.getSubfield('a').getData());
            }
         }
      }
      return result;
   }

   enum IllFlag {
                 Undefined(0),
                 None(1),
                 Ecopy(2),
                 Copy(3),
                 Loan(4);

      private final int value;

      IllFlag(int value) {
         this.value = value;
      }

      public int intValue() {
         return this.value;
      }

      @Override
      public String toString() {
         return name();
      }
   }

   /*
    * Entitätentyp der GND: $D: "b" - Körperschaft "f" - Kongress "g" - Geografikum "n" - Person (nicht individualisiert) "p" - Person (individualisiert) "s" - Sachbegriff "u" - Werk Entitätentyp der
    * SWD: $A a = Sachschlagwort b = geographisch-ethnographisches Schlagwort c = Personenschlagwort d = Koerperschaftsschlagwort f = Formschlagwort z = Zeitschlagwort
    * 
    */
   public enum MARCSubjectCategory {
                                    PERSONAL_NAME,
                                    CORPORATE_NAME,
                                    MEETING_NAME,
                                    UNIFORM_TITLE,
                                    CHRONOLOGICAL_TERM,
                                    TOPICAL_TERM,
                                    GEOGRAPHIC_NAME,
                                    GENRE_FORM,
                                    UNCONTROLLED_TERM;

      public static final MARCSubjectCategory mapToMARCSubjectCategory(final int indicator2From653) {
         final MARCSubjectCategory marcSubjectCategory;
         switch (indicator2From653) {
            case 0:
               marcSubjectCategory = MARCSubjectCategory.TOPICAL_TERM;
               break;
            case 1:
               marcSubjectCategory = MARCSubjectCategory.PERSONAL_NAME;
               break;
            case 2:
               marcSubjectCategory = MARCSubjectCategory.CORPORATE_NAME;
               break;
            case 3:
               marcSubjectCategory = MARCSubjectCategory.MEETING_NAME;
               break;
            case 4:
               marcSubjectCategory = MARCSubjectCategory.CHRONOLOGICAL_TERM;
               break;
            case 5:
               marcSubjectCategory = MARCSubjectCategory.GEOGRAPHIC_NAME;
               break;
            case 6:
               marcSubjectCategory = MARCSubjectCategory.GENRE_FORM;
               break;
            default:
               marcSubjectCategory = MARCSubjectCategory.TOPICAL_TERM;
         }
         return marcSubjectCategory;
      }

   }

   public enum GNDSubjectCategory {
                                   PERSON_NONINDIVIDUAL('n'),
                                   PERSON_INDIVIDUAL('p'),
                                   KOERPERSCHAFT('b'),
                                   KONGRESS('f'),
                                   GEOGRAFIKUM('g'),
                                   SACHBEGRIFF('s'),
                                   WERK('u');
      private final char value;

      private GNDSubjectCategory(char c) {
         this.value = c;
      }

      public static final MARCSubjectCategory mapToMARCSubjectCategory(final char gndCategory) {
         final MARCSubjectCategory marcSubjectCategory;
         switch (gndCategory) {
            case 'n':
               marcSubjectCategory = MARCSubjectCategory.PERSONAL_NAME;
               break;
            case 'p':
               marcSubjectCategory = MARCSubjectCategory.PERSONAL_NAME;
               break;
            case 'b':
               marcSubjectCategory = MARCSubjectCategory.CORPORATE_NAME;
               break;
            case 'f':
               marcSubjectCategory = MARCSubjectCategory.MEETING_NAME;
               break;
            case 'g':
               marcSubjectCategory = MARCSubjectCategory.GEOGRAPHIC_NAME;
               break;
            case 's':
               marcSubjectCategory = MARCSubjectCategory.TOPICAL_TERM;
               break;
            case 'u':
               marcSubjectCategory = MARCSubjectCategory.UNIFORM_TITLE;
               break;
            default:
               marcSubjectCategory = MARCSubjectCategory.TOPICAL_TERM;
         }
         return marcSubjectCategory;
      }

      public final char valueOf() {
         return value;
      }
   }

   public enum SWDSubjectCategory {
                                   SACHBEGRIFF('a'),
                                   GEOGRAFIKUM('b'),
                                   PERSON('c'),
                                   KOERPERSCHAFT('d'),
                                   FORMSCHLAGWORT('f'),
                                   ZEITSCHLAGWORT('z');

      private final char value;

      private SWDSubjectCategory(char c) {
         this.value = c;
      }

      public static final MARCSubjectCategory mapToMARCSubjectCategory(final char swdCategory) {
         final MARCSubjectCategory marcSubjectCategory;
         switch (swdCategory) {
            case 'a':
               marcSubjectCategory = MARCSubjectCategory.TOPICAL_TERM;
               break;
            case 'b':
               marcSubjectCategory = MARCSubjectCategory.GEOGRAPHIC_NAME;
               break;
            case 'c':
               marcSubjectCategory = MARCSubjectCategory.PERSONAL_NAME;
               break;
            case 'd':
               marcSubjectCategory = MARCSubjectCategory.CORPORATE_NAME;
               break;
            case 'f':
               marcSubjectCategory = MARCSubjectCategory.GENRE_FORM;
               break;
            case 'z':
               marcSubjectCategory = MARCSubjectCategory.CHRONOLOGICAL_TERM;
               break;
            default:
               marcSubjectCategory = MARCSubjectCategory.TOPICAL_TERM;
         }
         return marcSubjectCategory;
      }

      public final char valueOf() {
         return value;
      }
   }

}
