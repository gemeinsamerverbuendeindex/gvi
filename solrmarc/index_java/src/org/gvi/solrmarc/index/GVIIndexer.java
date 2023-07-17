package org.gvi.solrmarc.index;

import java.util.List;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.gvi.solrmarc.index.gvi.Basic;
import org.gvi.solrmarc.index.gvi.Cluster;
import org.gvi.solrmarc.index.gvi.Gnd_Charset;
import org.gvi.solrmarc.index.gvi.Init;
import org.gvi.solrmarc.index.gvi.MatchKey;
import org.gvi.solrmarc.index.gvi.Material;
import org.gvi.solrmarc.index.gvi.Subject;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;
import org.solrmarc.index.SolrIndexer;

/**
 * Wrapper der eigenen "custom" Methoden für die Indexierung<br>
 * In der Klasse werden die Aufrufe nur entgegen genommen und weitergeleitet.<br>
 * Die Methoden selbst befinden sich in thematisch geordneten Hilfsklassen.<br>
 * <dl>
 * <li>{@link Gnd_Charset} Expansion mit GND Normdaten und indexierung von originalschriftlichen Angaben</li>
 * <li>{@link Cluster} Verknüpfung von Dubletten</li>
 * <li>{@link MatchKey} Verknüpfung von wahrscheinlichen Dubletten</li>
 * <li>{@link Material} Bestimmung von Materialeigenschaften</li>
 * <li>{@link Subject} Extraktion von Schlagworten und Klassifikationen</li>
 * <li>{@link Basic} triviale Methoden</li>
 * <li>{@link SolrIndexer} Der eigentliche Einstieg</li>
 * </dl>
 * 
 * @author Thomas Kirchhoff <thomas.kirchhoff@bsz-bw.de>
 * @version 2017-06-26 uh, Synonyme aus File lesen und ergänzen
 * @version 2018-03-02 uh, GND-Synonyme vom remote repository lesen und ergänzen
 * @version
 * @version 2021-07-28 uh, Methoden der Klasse in mehrere thematisch geordnete Superklassen aufgeteilt.
 */
public class GVIIndexer extends SolrIndexer {

   @SuppressWarnings("unused")
   private static final Logger LOG        = LogManager.getLogger(GVIIndexer.class);
   Init                        init       = Init.init();                           // read implicit the property files.
   Basic                       basic      = new Basic(this);
   Subject                     subject    = new Subject(this);
   Material                    material   = new Material();
   MatchKey                    matchKey   = new MatchKey(this);
   Cluster                     cluster    = new Cluster(this);
   Gnd_Charset                 gndCharset = new Gnd_Charset(this);

   public GVIIndexer(String indexingPropsFile, String[] propertyDirs) throws Exception {
      super(indexingPropsFile, propertyDirs);
   }

   public GVIIndexer() {
      super(null, null);
   }

   /**
    * 
    * /** Get the catalog's id fore the record ...<br>
    * Wrapper to {@link Basic#getCatalogId(Record)}
    */
   public String getRecordId(Record record) {
      return basic.getRecordId(record);
   }

   /**
    * Count the number of 924 fields ...<br>
    * Wrapper to {@link Basic#countHoldingLibraries(Record)}
    */
   public String getHoldingCount(Record record) {
      return basic.getHoldingCount(record);
   }

   /**
    * <br>
    * Wrapper to {@link Basic#hasEnrichment(Record)
    */
   public Set<String> getEnrichmentTypes(Record record) {
      return basic.getEnrichmentTypes(record);
   }

   /**
    * Get Id of collection ... <br>
    * Wrapper to {@link Basic#getCollection(Record)}
    */
   public String getCollection(final Record record) {
      return basic.getCollection(record);
   }

   /**
    * Get the catalog's id for this record ... <br>
    * Wrapper to {@link Basic#getCatalogId(Record)}
    */
   public String getCatalogId(final Record record) {
      return basic.getCatalogId(record);
   }

   /**
    * Get set of other_ids. For HBZ (DE-605) add (DE-605)$001. This is used for deletion. <br>
    * Wrapper to {@link Basic#getOtherId(Record, tagString)
    */
   public  Set<String> getOtherId(final Record record, String tagStr) {
    return basic.getOtherId(record, tagStr);
   }
   
  /**
    * Get value(s) of selected classification schema ...<br>
    * Wrapper to {@link Basic#getClassification(Record, String, String)}
    */
   public Set<String> getClassification(Record record, String notationField, String schemaName) {
      return basic.getClassification(record, notationField, schemaName);
   }

   /**
    * Determine ILL (Inter Library Loan) Flag ...<br>
    * Wrapper to {@link Basic#getIllFlag(Record)}
    */
   public Set<String> getIllFlag(Record record) {
      return basic.getIllFlag(record);
   }

   /**
    * Try to extract the ZDB id ...<br>
    * Wrapper to {@link Basic#getZdbId(Record)}
    */
   public Set<String> getZdbId(Record record) {
      return basic.getZdbId(record);
   }

    /**
     * Get publication date
     */
    public String getPublicationDate(final Record record) {
	return basic.getPublicationDate(record);
    }

    /**
    * Get the licenced year(s) Field 912 ...<br>
    * Wrapper to {@link Basic#getProductYear(Record)}
    */
   public Set<String> getProductYear(final Record record) {
      return basic.getProductYear(record);
   }

   /**
    * Find and patch the ISIL of the Consortium.<br>
    * Wrapper to {@link Basic#getConsortium(Record)}
    */
   public Set<String> getConsortium(final Record record) {
      return basic.getConsortium(record);
   }

   /**
    * Extension for VuFind clients ...<br>
    * Wrapper to {@link Basic#getMarcTypByConsortium(Record)}
    */
   public String getMarcTypByConsortium(final Record record) {
      return basic.getMarcTypByConsortium(record);
   }

   /**
    * Split string with comma separated List ...<br>
    * Wrapper to {@link Basic#splitSubfield(Record)}
    */
   public Set<String> splitSubfield(Record record, String tagStr) {       
       Set<String> resultSet = basic.splitSubfield(record, tagStr);
       List<VariableField> list937 = record.getVariableFields("937");
       for (VariableField f: list937) {
            DataField d = (DataField) f;
            Subfield sf937e = d.getSubfield('e');
            Subfield sf937f = d.getSubfield('f');
            if (sf937e != null && sf937e.getData().matches("(.*)[Ss]ingstimme(.*)") && 
                sf937f != null && sf937f.getData().matches("(.*)[Ss]olo(.*)")) {
                resultSet.add("singstimme solo");
            }
       }       
      return resultSet;
   }

   /**
    * Lookup to find a KOBV cluster ...<br>
    * Wrapper to {@link Cluster#getDupId(Record)}
    */
   public String getDupId(Record record) {
      return cluster.getDupId(record);
   }

   /**
    * Lookup to find a CultureGraph cluster ...<br>
    * Wrapper to {@link Cluster#getDupId(Record)}
    */
   public String getCultureGraphClusterId(Record record) {
      return cluster.getCultureGraphClusterId(record);
   }

   /**
    * Expand GND synonyms from provided finder ...<br>
    * Wrapper to {@link Gnd_Charset}
    */
   public Set<String> expandGnd(Record record, String tagStr1, String tagStr2) {
      return gndCharset.expandGnd(record, tagStr1, tagStr2);
   }

   /**
    * Expand GND synonyms from provided finder ...<br>
    * Wrapper to {@link Gnd_Charset}
    */
   public Set<String> expandGnd(Record record, String tagStr) {
      return gndCharset.expandGnd(record, tagStr);
   }

   /**
    * Extent the category tags with their version in original writing ...<br>
    * Wrapper to {@link Gnd_Charset#getAllCharSets(Record, String)}
    */
   public Set<String> getAllCharSets(Record record, String tagString) {
      return gndCharset.getAllCharSets(record, tagString);
   }

   /**
    * Checks if the requested fields starts with ...<br>
    * Wrapper to {@link Gnd_Charset#getTermID(Record, String, String, String)}
    */
   public Set<String> getTermID(Record record, String tagStr, String prefixStr, String keepPrefixStr) {
      return gndCharset.getTermID(record, tagStr, prefixStr, keepPrefixStr);
   }

   /**
    * Detect the most specific material information ...<br>
    * Wrapper to {@link MatchKey#matchkeyMaterial(Record)}
    */
   public String matchkeyMaterial(Record record) {
      return matchKey.matchkeyMaterial(record);
   }

   /**
    * ...<br>
    * Wrapper to {@link MatchKey#matchkeyMaterialAuthorTitle(Record)}
    */
   public String matchkeyMaterialAuthorTitle(Record record) {
      return matchKey.matchkeyMaterialAuthorTitle(record);
   }

   /**
    * ...<br>
    * Wrapper to {@link MatchKey#matchkeyMaterialAuthorTitleYear(Record)}
    */
   public String matchkeyMaterialAuthorTitleYear(Record record) {
      return matchKey.matchkeyMaterialAuthorTitleYear(record);
   }

   /**
    * ...<br>
    * Wrapper to {@link MatchKey#matchkeyMaterialAuthorTitleYearPublisher(Record)}
    */
   public String matchkeyMaterialAuthorTitleYearPublisher(Record record) {
      return matchKey.matchkeyMaterialAuthorTitleYearPublisher(record);
   }

   /**
    * ...<br>
    * Wrapper to {@link MatchKey#matchkeyMaterialISBNYear(Record)}
    */
   public String matchkeyMaterialISBNYear(Record record) {
      return matchKey.matchkeyMaterialISBNYear(record);
   }

   /**
    * Determine type of material ...<br>
    * Wrapper to {@link Material#getMaterialType(Record)}
    */
   public Set<String> getMaterialType(Record record) {
      return material.getMaterialType(record);
   }

   /**
    * Determine access method of material (physical, online) ...<br>
    * Wrapper to {@link Material#getMaterialAccess(Record)}
    */
   public Set<String> getMaterialAccess(Record record) {
      return material.getMaterialAccess(record);
   }

   /**
    * Detects convolutes of journal issues ...<br>
    * Wrapper to {@link Material#isJournalVolume(Record)}
    */
   public String isJournalVolume(Record record) {
      return material.isJournalVolume(record);
   }

   /**
    * ...<br>
    * Wrapper to {@link Subject#getSubjectChronologicalTerm(Record, String)}
    */
   public Set<String> getSubjectChronologicalTerm(Record record, String tagStr) {
      return subject.getSubjectChronologicalTerm(record, tagStr);
   }

   /**
    * ...<br>
    * Wrapper to {@link Subject#getSubjectCorporateName(Record, String)}
    */
   public Set<String> getSubjectCorporateName(Record record, String tagStr) {
      return subject.getSubjectCorporateName(record, tagStr);
   }

   /**
    * ...<br>
    * Wrapper to {@link Subject#getSubjectGeographicalName(Record, String)}
    */
   public Set<String> getSubjectGeographicalName(Record record, String tagStr) {
      return subject.getSubjectGeographicalName(record, tagStr);
   }

   /**
    * ...<br>
    * Wrapper to {@link Subject#getSubjectGenreForm(Record, String)}
    */
   public Set<String> getSubjectGenreForm(Record record, String tagStr) {
      return subject.getSubjectGenreForm(record, tagStr);
   }

   /**
    * ...<br>
    * Wrapper to {@link Subject#getSubjectMeetingName(Record, String)}
    */
   public Set<String> getSubjectMeetingName(Record record, String tagStr) {
      return subject.getSubjectMeetingName(record, tagStr);
   }

   /**
    * ...<br>
    * Wrapper to {@link Subject#getSubjectPersonalName(Record, String)}
    */
   public Set<String> getSubjectPersonalName(Record record, String tagStr) {
      return subject.getSubjectPersonalName(record, tagStr);
   }

   /**
    * ...<br>
    * Wrapper to {@link Subject#getSubjectTopicalTerm(Record, String)}
    */
   public Set<String> getSubjectTopicalTerm(Record record, String tagStr) {
      return subject.getSubjectTopicalTerm(record, tagStr);
   }

   /**
    * ...<br>
    * Wrapper to {@link Subject#getSubjectUniformTitle(Record, String)}
    */
   public Set<String> getSubjectUniformTitle(Record record, String tagStr) {
      return subject.getSubjectUniformTitle(record, tagStr);
   }
}
