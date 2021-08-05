package org.gvi.solrmarc.index.gvi;

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

   private Set<String> getSubjectUncontrolled(Record record, MARCSubjectCategory subjectCategory) {
      Set<String> result = new LinkedHashSet<>();
      List<VariableField> fields = record.getVariableFields("653");
      if (fields != null) {
         Iterator<VariableField> iterator = fields.iterator();
         while (iterator.hasNext()) {
            DataField field = (DataField) iterator.next();
            if (field.getSubfield('a') != null) {
               final char ind2 = field.getIndicator2();
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

   private Set<String> getSWDSubject(Record record, MARCSubjectCategory subjectCategory) {
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

}
