package org.gvi.solrmarc.index;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

   private static final Logger LOG     = LogManager.getLogger(JunitHelper.class);
   protected GVIIndexer        indexer = null;
   protected static final String pathToData = "solrmarc/index_java/junit/data/";
   private MarcFactory marcFactory = MarcFactory.newInstance();

   public JunitHelper() {
      System.setProperty("GviIndexer.skipSynonyms", "true");
      System.setProperty("GviIndexer.skipClusterMap", "true");
      System.setProperty("GviIndexer.skipCultureGraph", "true");
      indexer = new GVIIndexer();
      ValueIndexerFactory.initialize(null); // this singelton has to be called once
   }

   /**
    * Neuen Marcrecord erzeugen sowie Label und ID voreinstellen.
    *
    * @return the record
    */
   protected Record buildTestRecord() {
      Record mymarc = marcFactory.newRecord();
      // LEADER
      mymarc = marcFactory.newRecord("00000cam a2200000 a 4500");
      // CONTROL
      mymarc.addVariableField(marcFactory.newControlField("001", "test"));
      mymarc.addVariableField(marcFactory.newControlField("003", "DE-603"));
      mymarc.addVariableField(marcFactory.newControlField("001", "20161027161501.0"));
      mymarc.addVariableField(marcFactory.newControlField("008", "160930s2016 xx u00 u ger c"));
      // DATA
      mymarc.addVariableField(newField("100", null, "QayQayQay"));
      mymarc.addVariableField(newField("100", null, "HuHuHuHu"));
      mymarc.addVariableField(newField("245", null, "BlaBlaBla"));
      mymarc.addVariableField(newField("880", "245_dlkjdl", "FooFooFoo"));
      mymarc.addVariableField(newField("880", "710_dlkjdl", "BarBarBar"));
      mymarc.addVariableField(newField("880", "710_dlkjdl", "BuhBuhBuh"));
      return mymarc;
   }

   /**
    * 
    * @param factory
    * @param fieldId
    * @param reference
    * @param data
    * @return
    */
   protected DataField newField(String fieldId, String reference, String data) {
      DataField field = marcFactory.newDataField(fieldId, ' ', ' ');
      if (reference != null) field.addSubfield(newSubfield('6', reference));
      if (data != null) field.addSubfield(newSubfield('a', data));
      return field;
   }

   protected Subfield newSubfield(char code, String data) {
      Subfield subfield = marcFactory.newSubfield();
      subfield.setCode(code);
      subfield.setData(data);
      return subfield;
   }

   protected Record readMarcFile(String string) {
      // TODO Auto-generated method stub
      return null;
   }
}
