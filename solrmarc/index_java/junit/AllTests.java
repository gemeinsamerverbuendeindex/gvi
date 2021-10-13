import org.gvi.solrmarc.index.Basic;
import org.gvi.solrmarc.index.Cluster;
import org.gvi.solrmarc.index.Initialisierung;
import org.gvi.solrmarc.index.Matchkey;
import org.gvi.solrmarc.index.Material;
import org.gvi.solrmarc.index.Subject;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite over all 
 * 
 * @author Uwe, 13.10.2021
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
   Initialisierung.class,
   Basic.class, 
   Cluster.class,
   Matchkey.class,
   Material.class,
   Subject.class
   })

public class AllTests {
}
