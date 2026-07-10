package org.gvi.solrmarc.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;
import org.solrmarc.index.SolrIndexer;

/**
 * Tests for basic methods
 * 
 * @author uwe
 *
 */
public class Basic extends JunitHelper {
   private static final Logger LOG          = LogManager.getLogger(Basic.class);
   private Record              testRecord_1 = readMarcFromFile("K10plus_012653829.xml");
   private Record              testRecord_2 = readMarcFromFile("ZDB_1003673171.xml");

   /**
    * Validate the generation of the catalog id
    */
   @Test
   public void catalogId() {
      catalogIdByCollection(testRecord_1, "ZDB", "DE-600");
      catalogIdByCollection(testRecord_1, "HBZFIX", "DE-605");
      catalogIdByCollection(testRecord_1, "OBV", "AT-OBV");
      catalogIdByData(testRecord_1, "GBV", "DE-627");
      catalogIdByData(readMarcFromFile("CatalogBy040.xml"), "hebis", "DE-603");
      catalogIdByData(readMarcFromFile("CatalogByLocalId.xml"), "BVB", "DE-604");
   }

   private void catalogIdByCollection(Record record, String collection, String expectedCatalog) {
      indexer.basic.setCollection(collection);
      String catalogId = indexer.getCatalogId(record);
      LOG.trace("Catalog id: " + catalogId);
      assertTrue("The catalog for " + collection + " should be \"" + expectedCatalog + "\"", expectedCatalog.equals(catalogId));
   }

   private void catalogIdByData(Record record, String expectedName, String expectedCatalog) {
      indexer.basic.setCollection("UNDEFINED");
      String catalogId = indexer.getCatalogId(record);
      LOG.trace("Catalog id: " + catalogId);
      assertTrue("The catalog for " + expectedName + " should be \"" + expectedCatalog + "\"", expectedCatalog.equals(catalogId));
   }

   /**
    * Validate the identification of the consortia
    */
   @Test
   public void consortium() {
      // K10plus GBV holding
      consortiumHelper(testRecord_1, "UNDEFINED", "GBV", "DE-601", 1);

      // K10plus BVB + GBV holding
      Record both = readMarcFromFile("K10plus_012653829_both.xml");
      consortiumHelper(both, "UNDEFINED", "GBV", "DE-601", 2);
      consortiumHelper(both, "UNDEFINED", "BSZ", "DE-576", 2);

      // K10plus no holding
      consortiumHelper(readMarcFromFile("K10plus_012653829_none.xml"), "UNDEFINED", "K10plus", "DE-627", 1);

      // ZDB vs. DNB
      consortiumHelper(testRecord_2, "ZDB", "ZDB", "DE-600", 1);
      consortiumHelper(testRecord_2, "UNDEFINED", "DNB", "DE-101", 1);

   }

   private void consortiumHelper(Record record, String collection, String expectedName, String expectedCatalog, int expectedCount) {
      indexer.basic.setCollection(collection);
      Set<String> isils = indexer.getConsortium(record);
      LOG.trace("ISILs: " + isils);
      assertTrue("Wrong number of ISILs.", (isils.size() == expectedCount));
      assertTrue("The catalog for " + expectedName + " should be \"" + expectedCatalog + "\"", isils.contains(expectedCatalog));
   }

   /**
    * Validate the identification of the consortia
    */
   @Test
   public void holdingCount() {
      String count = indexer.getHoldingCount(testRecord_1);
      assertEquals("Wrong number of 924 entries.", count, "8");
      count = indexer.getHoldingCount(testRecord_2);
      assertEquals("Wrong number of 924 entries.", count, "91");
   }

   /**
    * Can't test the evaluation of system parameter at startup.<br>
    * The dynamic usage of {@link GVIIndexer#getCollection(Record)} is implicitly tested in other tests e.g.{@link #consortium()}.
    */
   @Test
   @Ignore
   public void collection() {
      // do nothing
   }

   /**
    * Validate the extraction of the classification data
    */
   @Test
   public void classification() {
      // unindexed classifications
      classificationHelper(testRecord_1, "084", "not a existing classification", 0, null);
      classificationHelper(testRecord_1, "084", "blk", 1, "blk #1");
      classificationHelper(testRecord_1, "084", "bcl", 1, "bcl #1");
      classificationHelper(testRecord_1, "084", "ssgn", 1, "ssgn #1");
      classificationHelper(testRecord_1, "084", "rvk", 1, "ZL 3210");
      classificationHelper(testRecord_1, "084", "ddc", 1, "100.123");
      classificationHelper(testRecord_1, "084", "DFI", 1, "DFI #1");
      classificationHelper(testRecord_1, "084", "FIV", 1, "FIV #1");
      // TODO ...
      // classification_fivr = custom(org.gvi.solrmarc.index.GVIIndexer), getClassification(936, fivr)
      // classification_fivs = custom(org.gvi.solrmarc.index.GVIIndexer), getClassification(936, fivs)
      // classification_fivrk = custom(org.gvi.solrmarc.index.GVIIndexer), getClassification(936, fivrk)
      // classification_fivsk = custom(org.gvi.solrmarc.index.GVIIndexer), getClassification(936, fivsk)

      // unindexed classifications
      classificationHelper(testRecord_1, "084", "sdnb", 2, "23a");
      classificationHelper(testRecord_1, "084", "sdnb", 2, "06a");

   }

   private void classificationHelper(Record record, String marcField, String classificationType, int expectedCount, String expectedValue) {
      Set<String> values = indexer.getClassification(record, marcField, classificationType);
      LOG.warn("Values for " + classificationType + ": " + values);
      assertTrue("Wrong number of entries.", (values.size() == expectedCount));
      if (expectedValue != null) {
         assertTrue("The classification" + classificationType + " should contain \"" + expectedValue + "\"", values.contains(expectedValue));
      }
   }

   /**
    * Validate the evaluation of the illFlag
    */
   @Test
   public void illFlag() {
      Set<String> values = indexer.getIllFlag(testRecord_1);
      LOG.warn("Values" + ": " + values);
      assertTrue("The title should be available for loan.", values.contains("Loan"));
      assertTrue("The title should be available for copy.", values.contains("Copy"));
      assertFalse("The title shouldn't be available for elecronic copy.", values.contains("Ecopy"));
      values = indexer.getIllFlag(testRecord_2);
      LOG.warn("Values" + ": " + values);
      assertTrue("The title should be available for copy.", values.contains("Copy"));
      assertFalse("The title shouldn't be available for loan.", values.contains("Loan"));
   }

   /**
    * Validate the generation of GVI-ID
    */
   @Test
   public void recordId() {
      assertEquals("Wrong id.", "(DE-627)012653829", indexer.getRecordId(testRecord_1));
   }

   /**
    * Validate the generation of the VuFind marker 'recordtype'
    */
   @Test
   public void recordType() {
      assertEquals("Wrong recordtype.", "GviMarcDE627", indexer.getMarcTypByConsortium(testRecord_1));
      indexer.basic.setCollection("ZDB");
      assertEquals("Wrong recordtype.", "GviMarcDE600", indexer.getMarcTypByConsortium(testRecord_2));
   }

   /**
    * Validate the extraction of the "Production year"
    */
   @Test
   public void productionYear() {
      Set<String> years = indexer.getProductYear(readMarcFromFile("K10plus_product_year.xml"));
      LOG.warn("Product years: " + years);
      productionYearCheck(years, "ZDB-1-IGE", "2020");
      productionYearCheck(years, "BSZ-98-IGB-MAUB", "2015");
      productionYearCheck(years, "BSZ-98-IGB-MAUB", "2016");
      productionYearCheck(years, "Test, Aufzählung_1", "1021");
      productionYearCheck(years, "Test, Aufzählung_2", "1031");
      productionYearCheck(years, "Test, Aufzählung_3", "1041");
      productionYearCheck(years, "Test, Wiedholung", "1202");
   }

   /**
    * Verifies the appearance of a given value in the set.<br>
    * 
    * @param years The set of years
    * @param productId Just a comment
    * @param productYear The year to check
    */
   private void productionYearCheck(Set<String> years, String productId, String productYear) {
      assertTrue("\"" + productYear + "\" for " + productId + " is missing", years.contains(productYear));
   }

   /**
    * Validate the default extraction of the publish date.<br>
    * !Be aware {@link SolrIndexer#getPublicationDate(record)} ignores Dates before 1200 and after 2099
    */
   @Test
   public void publicationDate() {
      Record patchMe = readMarcFromFile("K10plus_012653829.xml");
      assertEquals("Wrong publish date in 008.", "1980", indexer.getPublicationDate(patchMe));
      publicationDatePurgeField(patchMe, "008");
      assertEquals("Wrong publish date in 260.", "1960", indexer.getPublicationDate(patchMe));
      publicationDatePurgeField(patchMe, "260");
      assertEquals("Wrong publish date in 264.", "1964", indexer.getPublicationDate(patchMe));
      publicationDatePurgeField(patchMe, "264");
      assertTrue("The should be no more date", indexer.getPublicationDate(patchMe).isEmpty());
   }

   /**
    * Remove the named variable Field from the Record.<br>
    * This allows additional negative tests with the same loaded data.
    * 
    * @param patchMe
    * @param fieldName
    */
   private void publicationDatePurgeField(Record patchMe, String fieldName) {
      VariableField purge = patchMe.getVariableField(fieldName);
      assertNotNull("Check test data", purge);
      patchMe.removeVariableField(purge);
   }

   /**
    * Validate the generation of GVI-ID
    */
   @Test
   public void zdbId() {
      assertTrue("Wrong ZDB id.", indexer.getZdbId(testRecord_2).contains("2559636-6"));
   }

   /**
    * Validate the generation of GVI-ID TODO replace mock with real code.
    */
   @Test
   public void hasEnrichment() {
      assertTrue("Should contain the key for resource", indexer.getEnrichmentTypes(testRecord_1).contains(" "));
      assertTrue("Should contain the key for resource", indexer.getEnrichmentTypes(testRecord_1).contains("0"));
      assertTrue("Should contain the key for related resource", indexer.getEnrichmentTypes(testRecord_1).contains("2"));
   }

   /**
    * Validate the separation of lists in subfields.<br>
    *  
    */
   @Test
   public void splitSubfield() {
      Set<String> result = indexer.splitSubfield(testRecord_1, "937a:937b:937c:937d:937e:937f");
      assertEquals("Anzahl der Elemente ", 14, result.size());
      result = indexer.splitSubfield(testRecord_1, "937[a-f]");
      assertNotEquals("The behavior of SolrIndexer.getFieldList() has changed. (see GVI-235)", 14, result.size());
   }
}
