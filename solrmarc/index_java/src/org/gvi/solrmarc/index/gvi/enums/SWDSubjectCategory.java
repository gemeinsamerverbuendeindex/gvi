package org.gvi.solrmarc.index.gvi.enums;

/*
 * Entitätentyp der GND: $D: "b" - Körperschaft "f" - Kongress "g" - Geografikum "n" - Person (nicht individualisiert) "p" - Person (individualisiert) "s" - Sachbegriff "u" - Werk Entitätentyp der
 * SWD: $A a = Sachschlagwort b = geographisch-ethnographisches Schlagwort c = Personenschlagwort d = Koerperschaftsschlagwort f = Formschlagwort z = Zeitschlagwort
 * 
 */
public enum SWDSubjectCategory {
   SACHBEGRIFF('a'),
   GEOGRAFIKUM('b'),
   PERSON('c'),
   KOERPERSCHAFT('d'),
   FORMSCHLAGWORT('f'),
   ZEITSCHLAGWORT('z');

   private final char value;

   private SWDSubjectCategory(char c) {
      this.value = c;
   }

   public static final MARCSubjectCategory mapToMARCSubjectCategory(final char swdCategory) {
      final MARCSubjectCategory marcSubjectCategory;
      switch (swdCategory) {
         case 'a':
            marcSubjectCategory = MARCSubjectCategory.TOPICAL_TERM;
            break;
         case 'b':
            marcSubjectCategory = MARCSubjectCategory.GEOGRAPHIC_NAME;
            break;
         case 'c':
            marcSubjectCategory = MARCSubjectCategory.PERSONAL_NAME;
            break;
         case 'd':
            marcSubjectCategory = MARCSubjectCategory.CORPORATE_NAME;
            break;
         case 'f':
            marcSubjectCategory = MARCSubjectCategory.GENRE_FORM;
            break;
         case 'z':
            marcSubjectCategory = MARCSubjectCategory.CHRONOLOGICAL_TERM;
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
