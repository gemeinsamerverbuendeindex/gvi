package org.gvi.solrmarc.index.gvi.enums;


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

   public static final MARCSubjectCategory mapToMARCSubjectCategory(final char indicator2From653) {
      final MARCSubjectCategory marcSubjectCategory;
      switch (indicator2From653) {
         case '0':
            marcSubjectCategory = MARCSubjectCategory.TOPICAL_TERM;
            break;
         case '1':
            marcSubjectCategory = MARCSubjectCategory.PERSONAL_NAME;
            break;
         case '2':
            marcSubjectCategory = MARCSubjectCategory.CORPORATE_NAME;
            break;
         case '3':
            marcSubjectCategory = MARCSubjectCategory.MEETING_NAME;
            break;
         case '4':
            marcSubjectCategory = MARCSubjectCategory.CHRONOLOGICAL_TERM;
            break;
         case '5':
            marcSubjectCategory = MARCSubjectCategory.GEOGRAPHIC_NAME;
            break;
         case '6':
            marcSubjectCategory = MARCSubjectCategory.GENRE_FORM;
            break;
         default:
            marcSubjectCategory = MARCSubjectCategory.TOPICAL_TERM;
      }
      return marcSubjectCategory;
   }

}
