package org.gvi.solrmarc.normalizer;

import org.gvi.solrmarc.normalizer.impl.PunctuationSingleNormalizer;
import org.gvi.solrmarc.normalizer.impl.PagesNormalizer;
import org.gvi.solrmarc.normalizer.impl.SubstringNormalizer;
import org.gvi.solrmarc.normalizer.impl.ISSNNormalizer;
import org.gvi.solrmarc.normalizer.impl.RecconnumNormalizer;
import org.gvi.solrmarc.normalizer.impl.PlaceNormalizer;
import org.gvi.solrmarc.normalizer.impl.EditionNormalizer;
import org.gvi.solrmarc.normalizer.impl.YearNormalizer;
import org.gvi.solrmarc.normalizer.impl.NumpartNormalizer;
import org.gvi.solrmarc.normalizer.impl.BracketsPunctSingleNormalizer;
import org.gvi.solrmarc.normalizer.impl.ISMNNormalizer;
import org.gvi.solrmarc.normalizer.impl.NampartNormalizer;
import org.gvi.solrmarc.normalizer.impl.ISBNNormalizer;
import org.gvi.solrmarc.normalizer.impl.PunctuationMultiNormalizer;
import org.gvi.solrmarc.normalizer.impl.CoauthorsNormalizer;
import org.gvi.solrmarc.normalizer.impl.ScaleNormalizer;
import org.gvi.solrmarc.normalizer.impl.PublisherNormalizer;
import org.gvi.solrmarc.normalizer.impl.VolumeNormalizer;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * Created by asharenk on 12.06.14.
 */
public class NormalizerTest {
    @Test
    public void TestBracketsPunctSingleNormalizer() {
        BracketsPunctSingleNormalizer normalizer=new BracketsPunctSingleNormalizer();
        String s="\u0098\u0000";
        Assert.assertTrue(normalizer.normalize(s+"[test]1").compareTo("1")==0);
        Assert.assertTrue(normalizer.normalize("[test]1").compareTo("1")==0);
        Assert.assertTrue(normalizer.normalize("[[test]]").compareTo("test")==0);
        Assert.assertTrue(normalizer.normalize("[[test]  ]").compareTo("test")==0);
        Assert.assertTrue(normalizer.normalize("[te]st]").compareTo("st")==0);
        Assert.assertTrue(normalizer.normalize("[te]st[123]").compareTo("st") == 0);
        Assert.assertTrue(normalizer.normalize("[te]st[123]]]]]]").compareTo("st") == 0);
        Assert.assertNull(normalizer.normalize(null));
        Assert.assertTrue(normalizer.normalize("TesTing.,#+[test]").compareTo("testing") == 0);
        Assert.assertTrue(normalizer.normalize("Tes    Ting.,#123+[test]").compareTo("tes ting 123")==0);
        Assert.assertTrue(normalizer.normalize("   Tes    Ting.,#089+[test]    ").compareTo("tes ting 089")==0);
        Assert.assertTrue(normalizer.normalize("(   Tes    Ting.0000,#+[test]    )").compareTo("tes ting 0000")==0);
        Assert.assertTrue(normalizer.normalize("(   Tes    Ting.00ÄÜÖ00,#+[test]    )").compareTo("tes ting 00äüö00")==0);
    }
    @Test
    public void TestCoauthorsNormalizer() {
        CoauthorsNormalizer coauthorsNormalizer=new CoauthorsNormalizer();
        ArrayList<String> coauthors=new ArrayList<>();
        coauthors.add("Müller H. ");
        coauthors.add("Musterman Teo");
        coauthors.add("Bohlen Dieter");
        coauthors.add("Obama Barack");
        Assert.assertTrue(coauthorsNormalizer.normalize(coauthors).compareTo("bohlen dieter musterman teo müller h") == 0);
        coauthors.clear();
        coauthors.add("Müller, H. ");
        coauthors.add("Musterman, Teo");
        coauthors.add("Bohlen, Dieter");
        coauthors.add("###Obama Barack###   ");
        Assert.assertTrue(coauthorsNormalizer.normalize(coauthors).compareTo("bohlen dieter musterman teo müller h") == 0);
        coauthors.clear();
        coauthors.add("@@@Müller@@@@H. ");
        coauthors.add("Musterman, Teo");
        coauthors.add("Bohlen,,,,, Dieter");
        coauthors.add("###Obama Barack###   ");
        Assert.assertTrue(coauthorsNormalizer.normalize(coauthors).compareTo("bohlen dieter musterman teo müller h") == 0);
        coauthors.clear();
        coauthors.add("Müller, H. ");
        coauthors.add("###Obama Barack###   ");
        Assert.assertTrue(coauthorsNormalizer.normalize(coauthors).compareTo("müller h obama barack") == 0);
        coauthors.clear();
        coauthors.add(" ");
        coauthors.add("    ");
        Assert.assertTrue(coauthorsNormalizer.normalize(coauthors).compareTo("") == 0);
        coauthors.clear();
        Assert.assertTrue(coauthorsNormalizer.normalize(coauthors).compareTo("") == 0);
        Assert.assertNull(coauthorsNormalizer.normalize(null));
        coauthors.clear();
        coauthors.add(null);
        coauthors.add("@@@Müller@@@@H. ");
        coauthors.add("Musterman, Teo");
        coauthors.add(null);
        Assert.assertTrue(coauthorsNormalizer.normalize(coauthors).compareTo("musterman teo müller h") == 0);
        coauthors.clear();
        coauthors.add(null);
        coauthors.add(null);
        Assert.assertTrue(coauthorsNormalizer.normalize(coauthors).compareTo("") == 0);
    }
    @Test
    public void TestEditionNormalizer() {
        EditionNormalizer editionNormalizer=new EditionNormalizer();
        ArrayList<String> edition=new ArrayList<>();
        ArrayList<String> normEdition=new ArrayList<>();
        edition.add("Orig. Ausgabe[ca. 1954]");
        edition.add("[1]");
        edition.add("121");
        edition.add("xxxxx121,.#yyyyy");
        edition.add("xxxxx121,[.#yyyy]y");
        edition.add("0");
        edition.add("");
        edition.add(null);
        edition.add("xxxxx1.21,.#yyyyy");
        editionNormalizer.normalize(edition, normEdition);
        Assert.assertTrue("size is wrong test 1", normEdition.size() == 9);
        Assert.assertTrue("must be 1954 test 2", normEdition.get(0).compareTo("1954") == 0);
        Assert.assertTrue("must be 1 test 3", normEdition.get(1).compareTo("1") == 0);
        Assert.assertTrue("must be 121 test 4",normEdition.get(2).compareTo("121")==0);
        Assert.assertTrue("must be 121 test 5", normEdition.get(3).compareTo("121") == 0);
        Assert.assertTrue("must be 121 test 6",normEdition.get(4).compareTo("121")==0);
        Assert.assertTrue("must be 0 test 7", normEdition.get(5).compareTo("0") == 0);
        Assert.assertTrue("must be 1 test 8", normEdition.get(6).compareTo("1") == 0);
        Assert.assertTrue("must be 1 test 9", normEdition.get(7).compareTo("1") == 0);
        Assert.assertTrue("must be 21 test 10", normEdition.get(8).compareTo("21") == 0);
        editionNormalizer.normalize(null, normEdition);
        Assert.assertTrue("wrong size test 11", normEdition.size() == 1);
        Assert.assertTrue("must be 1 test 12",normEdition.get(0).compareTo("1")==0);
        edition.clear();
        editionNormalizer.normalize(edition, normEdition);
        Assert.assertTrue("wrong size test 13", normEdition.size() == 1);
        Assert.assertTrue("must be 1 test 14",normEdition.get(0).compareTo("1")==0);
    }
    @Test
    public void TestISBNNormalizer() {
        ISBNNormalizer isbnNormalizer=new ISBNNormalizer();
        ArrayList<String> isbn=new ArrayList<>();
        ArrayList<String> normISBN=new ArrayList<>();
        isbn.add("3898796922");
        isbn.add("[1]3898796922");
        isbn.add("Pp.");
        isbn.add("isbnO34X999[12]");
        isbn.add("\"Num.isbno34X999[12]\"");
        isbn.add("0");
        isbn.add("");
        isbn.add(null);
        isbn.add("xxxxx1.21,.#yyyyy");
        isbn.add("389-879-692-2X");
        isbnNormalizer.normalize(isbn, normISBN);

        isbnNormalizer.normalize(isbn, normISBN);
        Assert.assertTrue(normISBN.size() == 3);
        Assert.assertTrue(normISBN.get(0).compareTo("3898796922")==0);
        Assert.assertTrue(normISBN.get(1).compareTo("3898796922") == 0);
        Assert.assertTrue(normISBN.get(2).compareTo("3898796922x") == 0);
        isbnNormalizer.normalize(null, normISBN);
        Assert.assertTrue(normISBN.isEmpty());
    }
    @Test
    public void TestISSNNormalizer() {
        ISSNNormalizer issnNormalizer=new ISSNNormalizer();
        ArrayList<String> issn=new ArrayList<>();
        ArrayList<String> normISSN=new ArrayList<>();
        issn.add("3898796922");
        issn.add("[1]3898796922");
        issn.add("Pp.");
        issn.add("issnO34X999[12]");
        issn.add("\"Num.issno34X999[12]\"");
        issn.add("Num.isbno34X999[12]");
        issn.add("0");
        issn.add("");
        issn.add(null);
        issn.add("xxxxx1.21,.#yyyyy");
        issn.add("389-879-6-9");
        issnNormalizer.normalize(issn, normISSN);

        Assert.assertTrue(normISSN.size() == 1);
        Assert.assertTrue(normISSN.get(0).compareTo("38987969") == 0);
        issnNormalizer.normalize(null, normISSN);
        Assert.assertTrue(normISSN.isEmpty());
    }
    @Test
    public void TestNampartNormalizer() {
        NampartNormalizer nampartNormalizer=new NampartNormalizer();
        ArrayList<String> nampart=new ArrayList<>();
        ArrayList<String> normNampart=new ArrayList<>();
        nampart.add("TesTing.,   #+[test]");
        nampart.add("3898796922");
        nampart.add("\"Num.issno34X999[12]\"    ");
        nampart.add("");
        nampart.add(null);
        nampartNormalizer.normalize(nampart, normNampart);
        Assert.assertTrue(normNampart.size() == 3);
        Assert.assertTrue(normNampart.get(0).compareTo("testing") == 0);
        Assert.assertTrue(normNampart.get(1).compareTo("3898796922") == 0);
        Assert.assertTrue(normNampart.get(2).compareTo("num issno34x999") == 0);
        nampartNormalizer.normalize(null, normNampart);
        Assert.assertTrue(normNampart.isEmpty());
    }
    @Test
    public void TestNumpartNormalizer() {
        NumpartNormalizer numpartNormalizer=new NumpartNormalizer();
        ArrayList<String> numpart=new ArrayList<>();
        ArrayList<String> normNumpart=new ArrayList<>();
        numpart.add("TesTing.,   #+[test]");
        numpart.add("3898796922");
        numpart.add("\"Num.issno34X999[12]\"    ");
        numpart.add("Num.issno ddX WwW[12]    ");
        numpart.add("");
        numpart.add(null);
        numpartNormalizer.normalize(numpart, normNumpart);
        Assert.assertTrue(normNumpart.size() == 4);
        Assert.assertTrue(normNumpart.get(0).compareTo("testing") == 0);
        Assert.assertTrue(normNumpart.get(1).compareTo("3898796922") == 0);
        Assert.assertTrue(normNumpart.get(2).compareTo("34") == 0);
        Assert.assertTrue(normNumpart.get(3).compareTo("num issno ddx www") == 0);
        numpartNormalizer.normalize(null, normNumpart);
        Assert.assertTrue(normNumpart.isEmpty());
    }
    @Test
    public void TestPagesNormalizer() {
        PagesNormalizer pagesNormalizer=new PagesNormalizer();
        ArrayList<String> pages=new ArrayList<>();
        ArrayList<String> normPages=new ArrayList<>();
        pages.add("TesTing.,   #+[test]");
        pages.add("3898796922");
        pages.add("\"Num.issno34X999[12]\"    ");
        pages.add("Num.issno ddX WwW[12]    ");
        pages.add("Num.issno ddX WwW[9]    ");
        pages.add("Num. 7 issno ddX WwW[9]    ");
        pages.add("");
        pages.add(null);
        pagesNormalizer.normalize(pages, normPages);
        Assert.assertTrue(normPages.size() == 6);
        Assert.assertTrue(normPages.get(0).compareTo("testing test") == 0);
        Assert.assertTrue(normPages.get(1).compareTo("3898796922") == 0);
        Assert.assertTrue(normPages.get(2).compareTo("999") == 0);
        Assert.assertTrue(normPages.get(3).compareTo("num issno ddx www 12") == 0);
        Assert.assertTrue(normPages.get(4).compareTo("num issno ddx www 9") == 0);
        Assert.assertTrue(normPages.get(5).compareTo("num 7 issno ddx www") == 0);
        pagesNormalizer.normalize(null, normPages);
        Assert.assertTrue(normPages.isEmpty());
    }
    @Test
    public void TestPlaceNormalizer() {
        PlaceNormalizer placeNormalizer=new PlaceNormalizer();
        ArrayList<String> place=new ArrayList<>();
        ArrayList<String> placeNorm=new ArrayList<>();
        place.add("TesTing.,   #+[test]");
        place.add("389 87 9692 2");
        place.add("\"Num.issno 34X999 [12]\"    ");
        place.add("Num.issno ddX WwW[12]    ");
        place.add("New Orlean DC");
        place.add("VfVfVfVfVfVf Orlean DC");
        place.add("");
        place.add(null);
        placeNormalizer.normalize(place, placeNorm);
        Assert.assertTrue(placeNorm.size() == 6);
        Assert.assertTrue(placeNorm.get(0).compareTo("testi") == 0);
        Assert.assertTrue(placeNorm.get(1).compareTo("389") == 0);
        Assert.assertTrue(placeNorm.get(2).compareTo("num") == 0);
        Assert.assertTrue(placeNorm.get(3).compareTo("num") == 0);
        Assert.assertTrue(placeNorm.get(4).compareTo("new") == 0);
        Assert.assertTrue(placeNorm.get(5).compareTo("vfvfv") == 0);
        placeNormalizer.normalize(null, placeNorm);
        Assert.assertTrue(placeNorm.isEmpty());
    }
    @Test
    public void TestPublisherNormalizer() {
        PublisherNormalizer publisherNormalizer=new PublisherNormalizer();
        ArrayList<String> publisher=new ArrayList<>();
        ArrayList<String> publisherNorm=new ArrayList<>();
        publisher.add("TesTing.,   #+[test]abcdefgh");
        publisher.add("TesTingtest");
        publisher.add("389 87 9692 2");
        publisher.add("\"Num.issno 34X999 [12]\"    ");
        publisher.add("Num.issno ddX WwW[12]    ");
        publisher.add("New Orlean DC");
        publisher.add("VfVfVfVfVfVf Or lean DC");
        publisher.add("");
        publisher.add(null);
        publisherNormalizer.normalize(publisher, publisherNorm);

        Assert.assertTrue(publisherNorm.size() == 7);
        Assert.assertTrue(publisherNorm.get(0).compareTo("testi") == 0);
        Assert.assertTrue(publisherNorm.get(1).compareTo("testi") == 0);
        Assert.assertTrue(publisherNorm.get(2).compareTo("389 8") == 0);
        Assert.assertTrue(publisherNorm.get(3).compareTo("num i") == 0);
        Assert.assertTrue(publisherNorm.get(4).compareTo("num i") == 0);
        Assert.assertTrue(publisherNorm.get(5).compareTo("new o") == 0);
        Assert.assertTrue(publisherNorm.get(6).compareTo("vfvfv") == 0);
        publisherNormalizer.normalize(null, publisherNorm);
        Assert.assertTrue(publisherNorm.isEmpty());
    }
    @Test
    public void TestPunctuationMultiNormalizer() {
        PunctuationMultiNormalizer punctuationMultiNormalizer=new PunctuationMultiNormalizer();
        ArrayList<String> data=new ArrayList<>();
        ArrayList<String> normData=new ArrayList<>();
        data.add("[test]1");
        data.add("[[test]]");
        data.add("[[test]  ]");
        data.add("[te]st]");
        data.add("[te]st[123]");
        data.add("[te]st[123]]]]]]");
        data.add(null);
        data.add("");
        data.add("TesTing.,#+[test]");
        data.add("Tes    Ting.,#123+[test]");
        data.add("   Tes    Ting.,#089+[test]    ");
        data.add("(   Tes    Ting.0000,#+[test]    )");
        data.add("(   Tes    Ting.00ÄÜÖ00,#+[test]    )");
        punctuationMultiNormalizer.normalize(data,normData);
        Assert.assertTrue(normData.size() == 11);
        Assert.assertTrue(normData.get(0).compareTo("test 1")==0);
        Assert.assertTrue(normData.get(1).compareTo("test")==0);
        Assert.assertTrue(normData.get(2).compareTo("test")==0);
        Assert.assertTrue(normData.get(3).compareTo("te st")==0);
        Assert.assertTrue(normData.get(4).compareTo("te st 123") == 0);
        Assert.assertTrue(normData.get(5).compareTo("te st 123") == 0);
        Assert.assertTrue(normData.get(6).compareTo("testing test") == 0);
        Assert.assertTrue(normData.get(7).compareTo("tes ting 123 test")==0);
        Assert.assertTrue(normData.get(8).compareTo("tes ting 089 test")==0);
        Assert.assertTrue(normData.get(9).compareTo("tes ting 0000 test")==0);
        Assert.assertTrue(normData.get(10).compareTo("tes ting 00äüö00 test")==0);
        punctuationMultiNormalizer.normalize(null, normData);
        Assert.assertTrue(normData.isEmpty());
    }
    @Test
    public void TestPunctuationSingleNormalizer() {
        PunctuationSingleNormalizer punctuationSingleNormalizer=new PunctuationSingleNormalizer();
        Assert.assertTrue(punctuationSingleNormalizer.normalize("[test]1").compareTo("test 1")==0);
        Assert.assertTrue(punctuationSingleNormalizer.normalize("[[test]]").compareTo("test")==0);
        Assert.assertTrue(punctuationSingleNormalizer.normalize("[[test]  ]").compareTo("test")==0);
        Assert.assertTrue(punctuationSingleNormalizer.normalize("[te]st]").compareTo("te st")==0);
        Assert.assertTrue(punctuationSingleNormalizer.normalize("[te]st[123]").compareTo("te st 123") == 0);
        Assert.assertTrue(punctuationSingleNormalizer.normalize("[te]st[123]]]]]]").compareTo("te st 123") == 0);
        Assert.assertNull(punctuationSingleNormalizer.normalize(null));
        Assert.assertTrue(punctuationSingleNormalizer.normalize("TesTing.,#+[test]").compareTo("testing test") == 0);
        Assert.assertTrue(punctuationSingleNormalizer.normalize("Tes    Ting.,#123+[test]").compareTo("tes ting 123 test")==0);
        Assert.assertTrue(punctuationSingleNormalizer.normalize("   Tes    Ting.,#089+[test]    ").compareTo("tes ting 089 test")==0);
        Assert.assertTrue(punctuationSingleNormalizer.normalize("(   Tes    Ting.0000,#+[test]    )").compareTo("tes ting 0000 test")==0);
        Assert.assertTrue(punctuationSingleNormalizer.normalize("(   Tes    Ting.00ÄÜÖ00,#+[test]    )").compareTo("tes ting 00äüö00 test")==0);
    }
    @Test
    public void TestRecconnumNormalizer() {
        RecconnumNormalizer recconnumNormalizer=new RecconnumNormalizer();
        ArrayList<String> data=new ArrayList<>();
        ArrayList<String> normData=new ArrayList<>();
        data.add("(test)1");
        data.add("[(test)123]");
        data.add("([test]  )");
        data.add("(te)st]");
        data.add("(te)st[123]");
        data.add("[te]st(123)]]]]]");
        data.add(null);
        data.add("");
        data.add("TesTing.,#+(test)");
        data.add("Tes    Ting.,#123+(test)");
        data.add("   Tes    Ting.,#089+(test)    ");
        data.add("[   Tes    Ting.0000,#+(test)    ]");
        data.add("[   Tes    Ting.00ÄÜÖ00,#+(test)    ]");
        recconnumNormalizer.normalize(data,normData);
        Assert.assertTrue(normData.size() == 10);
        Assert.assertTrue(normData.get(0).compareTo("1")==0);
        Assert.assertTrue(normData.get(1).compareTo("123")==0);
        Assert.assertTrue(normData.get(2).compareTo("st")==0);
        Assert.assertTrue(normData.get(3).compareTo("st 123")==0);
        Assert.assertTrue(normData.get(4).compareTo("te st") == 0);
        Assert.assertTrue(normData.get(5).compareTo("testing") == 0);
        Assert.assertTrue(normData.get(6).compareTo("tes ting 123")==0);
        Assert.assertTrue(normData.get(7).compareTo("tes ting 089")==0);
        Assert.assertTrue(normData.get(8).compareTo("tes ting 0000")==0);
        Assert.assertTrue(normData.get(9).compareTo("tes ting 00äüö00")==0);
        recconnumNormalizer.normalize(null, normData);
        Assert.assertTrue(normData.isEmpty());
    }
    @Test
    public void TestScaleNormalizer() {
        ScaleNormalizer scaleNormalizer=new ScaleNormalizer();
        ArrayList<String> data=new ArrayList<>();
        ArrayList<String> normData=new ArrayList<>();
        data.add("1:250");
        data.add("");
        data.add("Scale 1:300");
        data.add(" Test maßstab 1:300 000 original");
        data.add(" 2:300 900");
        data.add(" 1:300 t 900");
        data.add(null);
        data.add(" Maßstabiert ");
        data.add(" #+[]}!\"%");
        data.add(" MaßStabiert 250");
        data.add(" MaßStabiert 1:0 t 300");
        scaleNormalizer.normalize(data, normData);
        Assert.assertTrue(normData.size() == 8);
        Assert.assertTrue(normData.get(0).compareTo("250")==0);
        Assert.assertTrue(normData.get(1).compareTo("300")==0);
        Assert.assertTrue(normData.get(2).compareTo("300000")==0);
        Assert.assertTrue(normData.get(3).compareTo("2 300 900")==0);
        Assert.assertTrue(normData.get(4).compareTo("300")==0);
        Assert.assertTrue(normData.get(5).compareTo("maßstabiert") == 0);
        Assert.assertTrue(normData.get(6).compareTo("maßstabiert 250") == 0);
        Assert.assertTrue(normData.get(7).compareTo("0") == 0);
        scaleNormalizer.normalize(null, normData);
        Assert.assertTrue(normData.isEmpty());
    }
    @Test
    public void TestSubstringNormalizer() {
        SubstringNormalizer substringNormalizer=new SubstringNormalizer();
        substringNormalizer.setStart(0);
        substringNormalizer.setEnd(15);
        Assert.assertNull(substringNormalizer.normalize("   teSt ,."));
        substringNormalizer.setStart(3);
        substringNormalizer.setEnd(3);
        Assert.assertTrue(substringNormalizer.normalize("   teSt ,.").compareTo("") == 0);
        substringNormalizer.setStart(3);
        substringNormalizer.setEnd(4);
        Assert.assertTrue(substringNormalizer.normalize("   teSt ,.").compareTo("t")==0);
        substringNormalizer.setStart(5);
        substringNormalizer.setEnd(6);
        Assert.assertTrue(substringNormalizer.normalize("   teSt ,.").compareTo("s")==0);
        substringNormalizer.setStart(-1);
        substringNormalizer.setEnd(6);
        Assert.assertNull(substringNormalizer.normalize("   teSt ,."));
        substringNormalizer.setStart(3);
        substringNormalizer.setEnd(7);
        Assert.assertTrue(substringNormalizer.normalize("   teSt ,.").compareTo("test") == 0);
        Assert.assertNull(substringNormalizer.normalize(null));
    }
    @Test
    public void TestVolumeNormalizer() {
        VolumeNormalizer volumeNormalizer=new VolumeNormalizer();
        ArrayList<String> data=new ArrayList<>();
        ArrayList<String> normData=new ArrayList<>();
        data.add("[test]1");
        data.add("[[test]]");
        data.add("[[test]  ]");
        data.add("[te]st]");
        data.add("[te]st[123]");
        data.add("[te]st[123]]]]]]");
        data.add(null);
        data.add("");
        data.add("TesTing.,#+[test]");
        data.add("Tes    Ting.,#123+[test]");
        data.add("   Tes    Ting.,#089+[test]    ");
        data.add("(   Tes    Ting.0000,#+[test]    )");
        data.add("(   Tes    Ting.00ÄÜÖ00,#+[test]    )");
        data.add("(   Tes    Ting.00ÄÜÖ00,#+[test] 1,2 3   )");
        volumeNormalizer.normalize(data,normData);
        Assert.assertTrue(normData.size() == 12);
        Assert.assertTrue(normData.get(0).compareTo("1")==0);
        Assert.assertTrue(normData.get(1).compareTo("test")==0);
        Assert.assertTrue(normData.get(2).compareTo("test")==0);
        Assert.assertTrue(normData.get(3).compareTo("test")==0);
        Assert.assertTrue(normData.get(4).compareTo("123") == 0);
        Assert.assertTrue(normData.get(5).compareTo("123") == 0);
        Assert.assertTrue(normData.get(6).compareTo("testingtest") == 0);
        Assert.assertTrue(normData.get(7).compareTo("123")==0);
        Assert.assertTrue(normData.get(8).compareTo("89")==0);
        Assert.assertTrue(normData.get(9).compareTo("testing0000test")==0);
        Assert.assertTrue(normData.get(10).compareTo("testing00äüö00test")==0);
        Assert.assertTrue(normData.get(11).compareTo("123")==0);
        volumeNormalizer.normalize(null, normData);
        Assert.assertTrue(normData.isEmpty());
    }
    @Test
    public void TestYearNormalizer() {
        YearNormalizer yearNormalizer=new YearNormalizer();
        ArrayList<String> data=new ArrayList<>();
        ArrayList<String> normData=new ArrayList<>();
        data.add("[test]1947 Jahr");
        data.add("[test]Jaht 1947 [vermutlich] 1095");
        data.add("[test]tttt 1 [vermutlich] 1095");
        data.add("   [test]tttt 1 [Vermutlich] 2    ");
        data.add("[1867]tttt 1 [vermutlich] 2");
        data.add("[1867]tttt [vermutlich] orig.");
        data.add("");
        data.add("[0000]tTtt 1 [verMutlicH] 2");
        data.add("[0000]tTtt [verMutlicH] Orig.; ");
        data.add("[Test]tttt orig. [vermutlich] Original");
        data.add("Tes    Ting.,#123+[test]");
        data.add(null);
        data.add("   Tes    Ting.,#089+[test]    ");
        data.add("(   Tes    Ting.0000,#+[test]    )");
        data.add("(   Tes    Ting.00ÄÜÖ00,#+[test]    )");
        yearNormalizer.normalize(data, normData);
        Assert.assertTrue(normData.size() == 13);
        Assert.assertTrue(normData.get(0).compareTo("1947")==0);
        Assert.assertTrue(normData.get(1).compareTo("1947")==0);
        Assert.assertTrue(normData.get(2).compareTo("1095")==0);
        Assert.assertTrue(normData.get(3).compareTo("tttt 1 2")==0);
        Assert.assertTrue(normData.get(4).compareTo("tttt 1 2") == 0);
        Assert.assertTrue(normData.get(5).compareTo("1867") == 0);
        Assert.assertTrue(normData.get(6).compareTo("tttt 1 2") == 0);
        Assert.assertTrue(normData.get(7).compareTo("0000 tttt vermutlich orig") == 0);
        Assert.assertTrue(normData.get(8).compareTo("test tttt orig vermutlich original")==0);
        Assert.assertTrue(normData.get(9).compareTo("123")==0);
        Assert.assertTrue(normData.get(10).compareTo("89")==0);
        Assert.assertTrue(normData.get(11).compareTo("tes ting 0000")==0);
        Assert.assertTrue(normData.get(12).compareTo("tes ting 00äüö00")==0);
        yearNormalizer.normalize(null, normData);
        Assert.assertTrue(normData.isEmpty());
    }
    @Test
    public void TestIsmnNormalizer() {
        ISMNNormalizer ismnNormalizer=new ISMNNormalizer();
        ArrayList<String> data=new ArrayList<>();
        ArrayList<String> normData=new ArrayList<>();
        data.add("979-0-123-456-9");
        data.add("m123-456-9");
        data.add("m1");
        data.add("");
        data.add("abc");
        data.add("979-0---------");
        data.add("M-123-456-9");
        data.add("mmmmmM");
        data.add(null);
        ismnNormalizer.normalize(data, normData);
        Assert.assertTrue(normData.size() == 3);
        Assert.assertTrue(normData.get(0).compareTo("123456")==0);
        Assert.assertTrue(normData.get(1).compareTo("123456")==0);
        Assert.assertTrue(normData.get(2).compareTo("123456")==0);
        ismnNormalizer.normalize(null, normData);
        Assert.assertTrue(normData.isEmpty());
    }
}
