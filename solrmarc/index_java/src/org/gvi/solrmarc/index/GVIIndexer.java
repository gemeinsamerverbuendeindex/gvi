package org.gvi.solrmarc.index;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.File;
import java.io.FileInputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.gvi.solrmarc.normalizer.ISBNNormalizer;
import org.gvi.solrmarc.normalizer.impl.PunctuationSingleNormalizer;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;
import org.solrmarc.index.SolrIndexer;
import org.solrmarc.index.indexer.ValueIndexerFactory;
import org.solrmarc.mixin.GetFormatMixin;
import org.solrmarc.tools.DataUtil;

/**
 *
 * @author Thomas Kirchhoff <thomas.kirchhoff@bsz-bw.de>
 * @version 2017-06-26 uh, Synonyme aus File lesen und ergänzen
 * @version 2018-03-02 uh, GND-Synonyme vom remote repository lesen und ergänzen
 */
public class GVIIndexer extends SolrIndexer {

   private static final String                      kobvClusterInfoFile         = "kobv_clusters.properties";
   private static final Properties                  kobvClusterInfoMap          = new Properties();
   private static final String                      gndSynonymFile              = "gnd_synonyms.properties";
   private static final String                      gndLineSeperator            = "!_#_!";
   private static final Properties                  gndSynonymMap               = new Properties();
   private static final PunctuationSingleNormalizer punctuationSingleNormalizer = new PunctuationSingleNormalizer();
   private static boolean                           isInitialized               = false;
   private static final Logger                      LOG                         = LogManager.getLogger(GVIIndexer.class);
   private Record                                   cachedRecord                = null;
   private Map<String, Set<String>>                 cached880Fields             = new HashMap<>();

   public GVIIndexer(String indexingPropsFile, String[] propertyDirs) throws Exception {
      super(indexingPropsFile, propertyDirs);
      init();
   }

   public GVIIndexer() throws Exception {
      super(null, null);
      init();
   }

   /**
    * Internal constructor for inline tests via {@link #main(String[])}
    * 
    * @param dummy Any integer number. The value is meaningless, but it's needed to fit the signature of the constructor.
    * @throws Exception
    */
   private GVIIndexer(int dummy) throws Exception {
      isInitialized = true;
      ValueIndexerFactory.initialize(null); // this singelton has to be called once
   }

   private synchronized void init() throws Exception {
      if (isInitialized) return;
      isInitialized = true;
      if (System.getProperty("GviIndexer.skipBigFiles").equals("true")) {
         LOG.warn("Skip loading of big property files (GND synonyms and cluster mappings). Only applicable for tests");
         return;
      }

      if (LOG.isInfoEnabled()) {
         listMem();
         LOG.info("Loading of gnd synonymes started at: " + LocalDateTime.now().toString());
      }
      String gndDir = System.getProperty("gnd.configdir", ".");
      gndSynonymMap.load(new FileInputStream(new File(gndDir, gndSynonymFile)));
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

   /**
    * Get first found ISBN from marc:020<br>
    * * prefer real isbns ($a)<br>
    * * check next unformatted isbns ($9)<br>
    * * at last and least check wrong isbns ($z)
    * 
    * @param record
    * @return the normalized ISBN or an empty String
    */
   public String matchkeyISBN(Record record) {
      DataField isbnField = (DataField) record.getVariableField("020");
      if (isbnField != null) {
         // 1st try: $a
         String isbn = getFirstSubfield(isbnField, 'a');
         // 2nd try: $9
         if (isbn == null) isbn = getFirstSubfield(isbnField, '9');
         // if found return normalized value
         if (isbn != null) try {
            return ISBNNormalizer.normalize(isbn);
         }
         catch (IllegalArgumentException e) {
            LOG.warn("Invalid ISBN " + getRecordID(record) + ": " + e.getMessage());
            return simpleIsbnNormalisation(isbn);
         }
         // still here? 3nd try: $z
         isbn = getFirstSubfield(isbnField, 'z');
         if (isbn != null) return simpleIsbnNormalisation(isbn);
      }
      // 4th not found, send empty String as flag
      return "";
   }

   /**
    * Short helper to get the value of the first matching subfield
    * 
    * @param isbnField The {@link DataField} to inspect
    * @param subFieldCode The code of the wanted subfield
    * @return The value of the first matching subfield or NULL is not found
    */
   private String getFirstSubfield(DataField isbnField, char subFieldCode) {
      List<Subfield> subFieldList = isbnField.getSubfields(subFieldCode);
      if (subFieldList == null) return null;
      if (subFieldList.isEmpty()) return null;
      return subFieldList.get(0).getData();
   }

   /**
    * Fault tolerant Normalizer for invalid ISBNs<br>
    * Removes all white spaces and all punctuation characters.
    * Matches all characters to lower case.
    * 
    * @param rawISBN
    * @return The normalized string or an empty string if the given value was null or empty.
    */
   private String simpleIsbnNormalisation(String rawISBN) {
      if ((rawISBN == null) || rawISBN.isEmpty()) return "";
      return punctuationSingleNormalizer.normalize(rawISBN.replaceAll("\\w", ""));
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
      // Article
      else if (contentTypes.contains("Article")) {
         material = "article";
         if (contentTypes.contains("Journal/Magazine"))
            contentTypes.remove("Journal/Magazine");
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
      }
      else if (contentTypes.contains("Mixed Materials")) {
         material = "mixed";
      }
      // EBook
      else if (contentTypes.contains("Computer Resource") && materialForm.equals("m")) {
         material = "ebook";
      }
      // Undetermined
      else {
         material = "other";
      }

      return material;
   }

   /**
    * Extract the last name of the first found author/contributor 
    * @param record
    * @return Normalized for of the last name
    */
   public String matchkeyAuthor(Record record) {
      String lastName = getInvertetLastName(record);
      if (lastName == null) lastName = findLastNameIn245(record);
      if (lastName == null) return "";
      lastName = punctuationSingleNormalizer.normalize(lastName);
      lastName = lastName.replaceAll(" ", "");
      return lastName;
   }
   
   /**
    * Extract the last name from normalized fields.
    * @param record
    * @return
    */
   public String getInvertetLastName(Record record) {
      String firstAuthor = getFirstFieldVal(record, "100a:110a:111a:700a:710a:711a");
      if ((firstAuthor == null)|| firstAuthor.isEmpty()) return null; 
      String lastName = null;
         // first normalization
         firstAuthor = firstAuthor.trim().toLowerCase();
        if (firstAuthor.contains(", ")) { // inverted notation: "last name, first name"
            String[] nameParts = firstAuthor.split(",");
            if (nameParts.length > 0) { // in some titles, the given author is really ",".
               lastName = nameParts[0];
            }
         }
         else { // non inverted notation: "first name last name"
            int pos = firstAuthor.indexOf(' ');
            lastName = (pos <0) ? firstAuthor : firstAuthor.substring(pos);
         }
      return lastName;
   }
   
   /**
    * Extract the last name from unstructured field 245c.
    * @param record
    * @return
    */
   public String findLastNameIn245(Record record) {
      String firstAuthor = getFirstFieldVal(record, "245c");
      if ((firstAuthor == null)|| firstAuthor.isEmpty()) return null;
      firstAuthor = firstAuthor.replaceAll("\\[.*?\\]", "");
      firstAuthor = firstAuthor.replaceAll("<.*?>", "");
      firstAuthor = firstAuthor.replaceAll("\\{.*?\\}", "");
      firstAuthor = firstAuthor.replaceAll("\\(.*?\\)", "");
      firstAuthor = punctuationSingleNormalizer.normalize(firstAuthor);
      firstAuthor = firstAuthor.replace("im auftr d ", "");
      firstAuthor = firstAuthor.replace("hrsg ", "");
      firstAuthor = firstAuthor.replace("pupl ", "");
      firstAuthor = firstAuthor.replace("ed ", "");
      firstAuthor = firstAuthor.replace("von ", "");
      firstAuthor = firstAuthor.replace("by ", "");
      firstAuthor = firstAuthor.replace("and ", "");
      firstAuthor = firstAuthor.replace("\\s\\d+\\s", "");
      String lastName = firstAuthor.replaceAll("\\s+", "");
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

   @Deprecated
   public String xmatchkeyNumParts(Record record) {
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
      String matchkey = null;
      try {
         String material = matchkeyMaterial(record);
         String isbn = matchkeyISBN(record);
         String pubdate = matchkeyPubdate(record);
         if (!isbn.isEmpty()) {
            matchkey = String.format("%s:%s:%s", material, isbn, pubdate);
         }
         else {
            matchkey = matchkeyMaterialAuthorTitleYearPublisher(record);
         }
      }
      catch (Throwable e) {
         LOG.warn("MatchkeyException at record " + getRecordID(record), e);
      }
      return matchkey;
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
         }
         else if (material.equals("article")) {
            hostTitle = extractWords(getFirstFieldVal(record, "773t"), 3);
            relatedPart = extractWords(getFirstFieldVal(record, "773g"), 3);
            if (!hostTitle.isEmpty()) {
               matchkey = String.format("%s:%s", matchkey, hostTitle);
            }
            if (!relatedPart.isEmpty()) {
               matchkey = String.format("%s:%s", matchkey, relatedPart);
            }
         }
      }
      catch (Throwable e) {
         LOG.warn("MatchkeyException at record " + getRecordID(record), e);
      }

      return matchkey;
   }

   public String matchkeyMaterialAuthorTitleYear(Record record) {
      String matchkey = null;
      String pubdate = null;
      try {
         pubdate = matchkeyPubdate(record);
         matchkey = String.format("%s:%s", matchkeyMaterialAuthorTitle(record), pubdate);
      }
      catch (Throwable e) {
         LOG.warn("MatchkeyException at record " + getRecordID(record), e);
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
    * Extent the category tags with their version in original writing
    * 
    * @param record The current marc record
    * @param tagString List of (sub)fields to examine (see {@link #getFieldList(Record, String)}
    * @return The result of {@link #getFieldList(Record, String)} extended by original writing.
    */
   public Set<String> getAllCharSets(Record record, String tagString) {
      Set<String> resultList = getFieldList(record, tagString);
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
    * The 'tagString' of SolrMarc is a List of elements separated by a colon.
    * Each of the elements is the number of a marc field followed by one or more subfield codes.<br>
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
    * Get all all original writing data from this record.
    * The method sorts all marc:880$a fields as value into the result {@link Map}.<br>
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

   public String getLocalId(final Record record) {
      return getFirstFieldVal(record, "001");
   }

   public String getCatalog(final Record record) {
      return findCatalog(record, getLocalId(record));
   }

   public String getCollection(final Record record) {
      return System.getProperty("data.collection", "UNDEFINED");
   }

   public Set<String> getConsortium(final Record record) {
      return findConsortium(record, getCatalog(record));
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
      }
      else if (field003 != null) {
         if (field003.length() > 6) {
            field003 = field003.substring(0, 6);
         }
         catalog = field003;
      }
      else if (field040a != null) {
         catalog = field040a;
      }
      else if (f001 != null && f001.startsWith("BV")) {
         catalog = "DE-604";
      }
      else {
         catalog = "UNDEFINED";
      }
      return catalog;
   }

   protected Set<String> findConsortium(Record record, String catalog) {
      Set<String> consortiumSet = new HashSet<>();
      String collection = System.getProperty("data.collection", "UNDEFINED");
      switch (catalog) {
         case "DE-101": // DNB
            if (collection.equals("ZDB")) {
               consortiumSet.add("DE-600");
            }
            else {
               consortiumSet.add(catalog);
            }
            break;
         case "DE-576": // SWB
         case "DE-600": // ZDB
         case "DE-601": // GBV+KOBV+ZDB
         case "DE-602": // KOBV
         case "DE-603": // HEBIS
         case "DE-604": // BVB
         case "DE-605": // HBZ
            consortiumSet.add(catalog);
            break;
         case "DE-627": // K10Plus
            Set<String> regions = getFieldList(record, "924c");
            if (regions.contains("GBV"))
               consortiumSet.add("DE-601");
            if (regions.contains("BSZ"))
               consortiumSet.add("DE-576");
            if (consortiumSet.isEmpty())
               consortiumSet.add("DE-627");
            break;
         default:
            consortiumSet.add("UNDEFINED");
            break;
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
            }
            else if (field.getSubfield('A') != null) {
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

   // #######################################################################################
   // ########## Poor man's test environment
   // #######################################################################################
   /**
    * Do simple tests while development. (w/o the full environment)
    * 
    * @param unused No command line parameters are evaluated.
    * @throws Exception
    */
   public static void main(String[] unused) throws Exception {
      GVIIndexer me = new GVIIndexer(1);
      Record testRecord = me.buildTestRecord();
      Set<String> ret = me.getAllCharSets(testRecord, "100a:245a:710abcd");
      for (String retret : ret) {
         System.out.println("TEST: " + retret + "#");
      }
   }

   /**
    * Neuen Marcrecord erzeugen sowie Label und ID voreinstellen.
    *
    * @return the record
    */
   private Record buildTestRecord() {
      MarcFactory marcfactory = MarcFactory.newInstance();
      Record mymarc = marcfactory.newRecord();
      // LEADER
      mymarc = marcfactory.newRecord("00000cam a2200000 a 4500");
      // CONTROL
      mymarc.addVariableField(marcfactory.newControlField("001", "test"));
      mymarc.addVariableField(marcfactory.newControlField("003", "DE-603"));
      mymarc.addVariableField(marcfactory.newControlField("001", "20161027161501.0"));
      mymarc.addVariableField(marcfactory.newControlField("008", "160930s2016 xx u00 u ger c"));
      // DATA
      mymarc.addVariableField(newField(marcfactory, "100", null, "QayQayQay"));
      mymarc.addVariableField(newField(marcfactory, "100", null, "HuHuHuHu"));
      mymarc.addVariableField(newField(marcfactory, "245", null, "BlaBlaBla"));
      mymarc.addVariableField(newField(marcfactory, "880", "245_dlkjdl", "FooFooFoo"));
      mymarc.addVariableField(newField(marcfactory, "880", "710_dlkjdl", "BarBarBar"));
      mymarc.addVariableField(newField(marcfactory, "880", "710_dlkjdl", "BuhBuhBuh"));
      return mymarc;
   }

   private DataField newField(MarcFactory factory, String fieldId, String reference, String data) {
      DataField field = factory.newDataField(fieldId, ' ', ' ');
      if (reference != null) field.addSubfield(newSubfield(factory, '6', reference));
      if (data != null) field.addSubfield(newSubfield(factory, 'a', data));
      return field;
   }

   private Subfield newSubfield(MarcFactory factory, char code, String data) {
      Subfield subfield = factory.newSubfield();
      subfield.setCode(code);
      subfield.setData(data);
      return subfield;
   }

}
