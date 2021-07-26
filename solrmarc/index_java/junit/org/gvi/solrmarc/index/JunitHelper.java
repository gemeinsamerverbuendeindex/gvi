package org.gvi.solrmarc.index;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gvi.solrmarc.index.GVIIndexer;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.solrmarc.index.indexer.ValueIndexerFactory;

/**
 * Gemeinsame Methoden der einzelnen Tests.
 *
 * @author Uwe Reh (uh), HeBIS-IT
 * @version 14.07.2021 uh initial
 */
public class JunitHelper {
   
   private static final Logger LOG = LogManager.getLogger(JunitHelper.class);
   protected GVIIndexer indexer = null;
   
   public JunitHelper() {
      System.setProperty("GviIndexer.skipSynonyms", "true");
      System.setProperty("GviIndexer.skipClusterMap", "true");
      try {
         indexer = new GVIIndexer();
         ValueIndexerFactory.initialize(null); // this singelton has to be called once
      }
      catch (Exception e) {
         LOG.error("Fehler beim initialisieren:", e);
      }
      
   }

   /**
    * Neuen Marcrecord erzeugen sowie Label und ID voreinstellen.
    *
    * @return the record
    */
   protected Record buildTestRecord() {
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

   protected DataField newField(MarcFactory factory, String fieldId, String reference, String data) {
      DataField field = factory.newDataField(fieldId, ' ', ' ');
      if (reference != null) field.addSubfield(newSubfield(factory, '6', reference));
      if (data != null) field.addSubfield(newSubfield(factory, 'a', data));
      return field;
   }

   protected Subfield newSubfield(MarcFactory factory, char code, String data) {
      Subfield subfield = factory.newSubfield();
      subfield.setCode(code);
      subfield.setData(data);
      return subfield;
   }

}

