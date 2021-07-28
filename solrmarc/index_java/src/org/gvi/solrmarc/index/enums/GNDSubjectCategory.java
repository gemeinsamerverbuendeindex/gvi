package org.gvi.solrmarc.index.enums;


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
