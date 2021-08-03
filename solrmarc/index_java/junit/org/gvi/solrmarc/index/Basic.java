package org.gvi.solrmarc.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.marc4j.marc.Record;

/**
 * Tests for matchKey methods
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
      assertEquals("Wrong nomber of 924 entries.", count, "8");
      count = indexer.getHoldingCount(testRecord_2);
      assertEquals("Wrong nomber of 924 entries.", count, "91");
   }

   /**
    * Can't test the evaluation of system parameter at startup.<br>
    * The dynamic usage of {@link GVIIndexer#getCollection(Record)} is imlicit tested in other tests e.g.{@link #consortium()}.
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

}
/*
 * getPublicationDate008or26xc(Record) getRecordID(Record) getZdbId(Record) hasEnrichment(Record) splitSubfield(Record, String)
 */
