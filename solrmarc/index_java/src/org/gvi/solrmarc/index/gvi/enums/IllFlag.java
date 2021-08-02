package org.gvi.solrmarc.index.gvi.enums;

/**
 * Ausprägungen des Fernleihindikators,<br>
 * 
 * @see https://wiki.dnb.de/download/attachments/132744284/marcAend201201Leihverkehrsangabe.pdf
 *      <dl>
 *      <dd>'a' ⇨ "Loan: Ausleihe von Bänden möglich, keine Kopien."</dd>
 *      <dd>'b' ⇨ "Copy: Keine Ausleihe von Bänden, nur Papierkopien werden versandt."</dd>
 *      <dd>'c' ⇨ "Loan" &amp; "Copy: Uneingeschränkte Fernleihe, Kopie und Ausleihe (ergibt sich aus Kombination von 'a' und 'b' nicht aber 'e'."</dd>
 *      <dd>'d' ⇨ "None: Keine Fernleihe."</dd>
 *      <dd>'e' ⇨ "Ecopy: Keine Ausleihe von Bänden, der Endnutzer erhält eine elektronische Kopie."</dd>
 *      <dd>'' ⇨ "Undefined: Wenn marc:924d nicht angegeben ist."</dd>
 *      </dl>
 */
public enum IllFlag {
   Loan, 
   Copy,
   // Both 'c'
   None,
   Ecopy,
   Undefined;
}
