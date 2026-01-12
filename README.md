# GVI: Gemeinsamer Verbünde Index 
  @see [English version](https://github.com/gemeinsamerverbuendeindex/gvi#gvi-combined-consortional-library-index)
## Beschreibung
Neben der [Deutschen Nationalbibliothek](http://www.dnb.de/) pflegen die wissenschaftlichen [Bibliotheksverbünde](https://de.wikipedia.org/wiki/Bibliotheksverbund) große bibliografische Verzeichnisse.
Ziel diese Projektes ist es eine gemeinsame Schnittstelle über den Gesamtbestand bereitzustellen und Verbundübergreifende Anwendungen zu entwickeln.
### Technik
Der so entstandene gemeinsame Datenbestand von über 185 Mio Titeldaten wird in einer [SolrCloud](https://cwiki.apache.org/confluence/display/solr/SolrCloud) abgelegt. Über die [AbfrageSprache](https://cwiki.apache.org/confluence/display/solr/Query+Syntax+and+Parsing) von SOLR wird der Index für verschiedenste Anwendungen zur Verfügung stehen. Als eine erste Beispielanwendung wird es eine angepasste Variante von [VuFind](http://vufind-org.github.io/vufind/) geben.

----
# GVI: Combined Consortional Library Index
## Description
Beside the [German National Library](http://www.dnb.de/EN/Home/home_node.html) the [Bibliotheksverbünde](https://de.wikipedia.org/wiki/Bibliotheksverbund) are providing big bibliographic collections. The goal of this project is to offer a single interface to the union of this collections. 
### Technology
The combined collection (aprox.185 Mio titles) will be stored in a [SolrCloud](https://cwiki.apache.org/confluence/display/solr/SolrCloud). The public interface to this repository will be [query syntax](https://cwiki.apache.org/confluence/display/solr/Query+Syntax+and+Parsing) of Solr. A slightly adapted [VuFind](http://vufind-org.github.io/vufind/) will be one of the first applications. 
