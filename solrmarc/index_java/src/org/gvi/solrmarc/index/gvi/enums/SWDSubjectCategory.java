package org.gvi.solrmarc.index.gvi.enums;


/**
 * Representation of the SWD notation of the DNB. (marc:689)<br>
 * @see https://d-nb.info/1072442361/34
 * SWD Entitättyp in $A<br>
 * <dl>
 * <li>a = Sachschlagwort</li>
 * <li>b = geographisch-ethnographisches Schlagwort
 * <li>c = Personenschlagwort
 * <li>d = Koerperschaftsschlagwort
 * <li>f = Formschlagwort
 * <li>z = Zeitschlagwort
 * </dl>
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
