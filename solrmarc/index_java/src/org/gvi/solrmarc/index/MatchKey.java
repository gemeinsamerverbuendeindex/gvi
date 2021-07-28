package org.gvi.solrmarc.index;

import java.util.List;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.gvi.solrmarc.normalizer.ISBNNormalizer;
import org.gvi.solrmarc.normalizer.impl.PunctuationSingleNormalizer;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.solrmarc.mixin.GetFormatMixin;

public class MatchKey extends Material {
   
   private static final Logger                      LOG                         = LogManager.getLogger(MatchKey.class);
   private static final PunctuationSingleNormalizer punctuationSingleNormalizer = new PunctuationSingleNormalizer();

   public MatchKey(String indexingPropsFile, String[] propertyDirs) {
      super(indexingPropsFile, propertyDirs);
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
    * Short helper for {@link #matchkeyISBN(Record)}<br>
    * Get the value of the first matching subfield
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
    * Short helper for {@link #matchkeyISBN(Record)}<br>
    * Fault tolerant Normalizer for invalid ISBNs<br>
    * Removes all white spaces and all punctuation characters. Matches all characters to lower case.
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
         if (contentTypes.contains("Journal/Magazine")) contentTypes.remove("Journal/Magazine");
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
    * 
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
    * Short helper for {@link #matchkeyAuthor(Record)}<br>
    * Extract the last name from normalized fields.
    * 
    * @param record
    * @return
    */
   public String getInvertetLastName(Record record) {
      String firstAuthor = getFirstFieldVal(record, "100a:110a:111a:700a:710a:711a");
      if ((firstAuthor == null) || firstAuthor.isEmpty()) return null;
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
         lastName = (pos < 0) ? firstAuthor : firstAuthor.substring(pos);
      }
      return lastName;
   }

   /**
    * Short helper for {@link #matchkeyAuthor(Record)}<br>
    * Extract the last name from unstructured field 245c.
    * 
    * @param record
    * @return
    */
   public String findLastNameIn245(Record record) {
      String firstAuthor = getFirstFieldVal(record, "245c");
      if ((firstAuthor == null) || firstAuthor.isEmpty()) return null;
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

   private String matchkeyVolume(Record record) {
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

}
