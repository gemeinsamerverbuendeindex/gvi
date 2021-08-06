package org.gvi.solrmarc.index.gvi;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.gvi.solrmarc.index.GVIIndexer;
import org.gvi.solrmarc.index.gvi.enums.GNDSubjectCategory;
import org.gvi.solrmarc.index.gvi.enums.MARCSubjectCategory;
import org.gvi.solrmarc.index.gvi.enums.SWDSubjectCategory;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;

public class Subject {
   private static final Logger LOG  = LogManager.getLogger(Subject.class);
   private GVIIndexer          main = null;

   public Subject(GVIIndexer callback) {
      main = callback;
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

   private Set<String> getSubject(Record record, String tagStr, MARCSubjectCategory subjectCategory) {
      Set<String> result = main.getFieldList(record, tagStr);
      result.addAll(getSubjectUncontrolled(record, subjectCategory));
      result.addAll(getSWDSubject(record, subjectCategory));
      return result;
   }

   /**
    * Evaluate marc:653
    * @param record
    * @param subjectCategory
    * @return
    */
   private Set<String> getSubjectUncontrolled(Record record, MARCSubjectCategory subjectCategory) {
      Set<String> result = new LinkedHashSet<>();
      List<VariableField> fields = record.getVariableFields("653");
      if (fields != null) {
         Iterator<VariableField> iterator = fields.iterator();
         while (iterator.hasNext()) {
            DataField field = (DataField) iterator.next();
            Character subjectKey = getSubjectKey(field);
            if (subjectKey != null) {
               result.addAll(getSubjectTerms(field, subjectCategory, MARCSubjectCategory.mapToMARCSubjectCategory(subjectKey)));
            }
         }
      }
      return result;
   }

   /**
    * Evaluate marc:689
    * @param record
    * @param subjectCategory
    * @return
    */
   private Set<String> getSWDSubject(Record record, MARCSubjectCategory subjectCategory) {
      Set<String> result = new LinkedHashSet<>();
      List<VariableField> fields = record.getVariableFields("689");
      if (fields != null) {
         Iterator<VariableField> iterator = fields.iterator();
         while (iterator.hasNext()) {
            DataField field = (DataField) iterator.next();
            // GND Sachbegriff
            Character subjectKey = getSubjectKey(field, 'D');
            if (subjectKey != null) {
               result.addAll(getSubjectTerms(field, subjectCategory, GNDSubjectCategory.mapToMARCSubjectCategory(subjectKey)));
            }
            else {
               // Alter SWD Sachbegriff. Muss gemappt werden!
               subjectKey = getSubjectKey(field, 'A');
               if (subjectKey != null) {
                  result.addAll(getSubjectTerms(field, subjectCategory, SWDSubjectCategory.mapToMARCSubjectCategory(subjectKey)));
               }
            }
         }
      }
      return result;
   }

   /**
    * Get Indicator 2 as key
    * @param field
    * @return
    */
   private char getSubjectKey(DataField field) {
      return field.getIndicator2();
   }

   /**
    * Get content of subfield as key
    * @param field
    * @param subFieldKey
    * @return
    */
   private Character getSubjectKey(DataField field, char subFieldKey) {
      Subfield subField = field.getSubfield(subFieldKey);
      if (subField == null) return null;
      String data = subField.getData();
      if (data.length() != 1) return null;
      return data.charAt(0);
   }

   /**
    * Get subject terms from the 'a' subfield(s)
    * @param field
    * @param result
    * @param neededSubjectCategory
    * @param detectedSubjectCategory
    */
   private Set<String> getSubjectTerms(DataField field, MARCSubjectCategory neededSubjectCategory, MARCSubjectCategory detectedSubjectCategory) {
      Set<String> ret = new HashSet<String>();
      if (neededSubjectCategory.equals(detectedSubjectCategory)) {
         List<Subfield> subjects = field.getSubfields('a');
         for (Subfield subject : subjects) {
            ret.add(subject.getData());
         }
      }
      return ret;
   }

}
