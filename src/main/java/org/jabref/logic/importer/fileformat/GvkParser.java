package org.jabref.logic.importer.fileformat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GvkParser implements Parser {
    private static boolean[] visited = new boolean[135];
    private static final Logger LOGGER = LoggerFactory.getLogger(GvkParser.class);

    private String author = null;
    private String editor = null;
    private String title = null;
    private String publisher = null;
    private String year = null;
    private String address = null;
    private String series = null;
    private String edition = null;
    private String isbn = null;
    private String issn = null;
    private String number = null;
    private String pagetotal = null;
    private String volume = null;
    private String pages = null;
    private String journal = null;
    private String ppn = null;
    private String booktitle = null;
    private String url = null;
    private String note = null;

    private String quelle = "";
    private String mak = "";
    private String subtitle = "";

    private EntryType entryType = StandardEntryType.Book; // Default

    @Override
    public List<BibEntry> parseEntries(InputStream inputStream) throws ParseException {
        try {
            DocumentBuilder dbuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document content = dbuild.parse(inputStream);
            return this.parseEntries(content);
        } catch (ParserConfigurationException | SAXException | IOException exception) {
            throw new ParseException(exception);
        }
    }

    private List<BibEntry> parseEntries(Document content) {
        List<BibEntry> result = new LinkedList<>();

        // used for creating test cases
        // XMLUtil.printDocument(content);

        // Namespace srwNamespace = Namespace.getNamespace("srw","http://www.loc.gov/zing/srw/");

        // Schleife ueber allen Teilergebnissen
        //Element root = content.getDocumentElement();
        Element root = (Element) content.getElementsByTagName("zs:searchRetrieveResponse").item(0);
        Element srwrecords = getChild("zs:records", root);
        if (srwrecords == null) {
            // no records found -> return empty list
            return result;
        }
        List<Element> records = getChildren("zs:record", srwrecords);
        for (Element record : records) {
            Element e = getChild("zs:recordData", record);
            if (e != null) {
                e = getChild("record", e);
                if (e != null) {
                    result.add(parseEntry(e));
                }
            }
        }
        return result;
    }

    private BibEntry parseEntry(Element e) {
        // Alle relevanten Informationen einsammeln

        List<Element> datafields = getChildren("datafield", e);
        int SIZEOFDATAFIELDS = datafields.size();
        for (Element datafield : datafields) {
            visited[0] = true;
            String tag = datafield.getAttribute("tag");
            LOGGER.debug("tag: " + tag);

            // mak
            if ("002@".equals(tag)) {
                visited[1] = true;
                setBibliographicTypeAndStatus(datafield);
            } else {
                visited[4] = true;
            }

            //ppn
            if ("003@".equals(tag)) {
                visited[5] = true;
                setRecordControlNumber(datafield);
            } else {
                visited[6] = true;
            }

            //author
            if ("028A".equals(tag)) {
                visited[7] = true;  
                setPrimaryauthor(datafield);              
            } else {
                visited[10] = true;
            }

            //author (weiterer)
            if ("028B".equals(tag)) {
                visited[11] = true;
                setCoauthor(datafield);
            } else {
                visited[14] = true;
            }

            //editor
            if ("028C".equals(tag)) {
                visited[15] = true;
                setSecondaryAuthor(datafield);
            } else {
                visited[18] = true;
            }

            //title and subtitle
            if ("021A".equals(tag)) {
                visited[19] = true;
                setTitleAndStatementOfResponsibilityArea(datafield);
            } else {
                visited[20] = true;
            }

            //publisher and address
            if ("033A".equals(tag)) {
                visited[21] = true;
                setFirstPublisher(datafield);
            } else {
                visited[22] = true;
            }

            //year
            if ("011@".equals(tag)) {
                visited[23] = true;
                setDateOfPublication(datafield);
            } else {
                visited[24] = true;
            }

            //year, volume, number, pages (year bei Zeitschriften (evtl. redundant mit 011@))
            if ("031A".equals(tag)) {
                visited[25] = true;
                setNumberingArea(datafield);
            } else {
                visited[26] = true;
            }

            // 036D seems to contain more information than the other fields
            // overwrite information using that field
            // 036D also contains information normally found in 036E
            if ("036D".equals(tag)) {
                visited[27] = true;
                setLinkToMultiVolumePublication(datafield);
            } else {
                visited[32] = true;
            }

            //series and number
            if ("036E".equals(tag)) {
                visited[33] = true;
                series = getSubfield("a", datafield);
                number = getSubfield("l", datafield);
                String kor = getSubfield("b", datafield);

                if (kor != null) {
                    visited[34] = true;
                    series = series + " / " + kor;
                } else {
                    visited[35] = true;
                }
            } else {
                visited[36] = true;
            }

            //note
            if ("037A".equals(tag)) {
                visited[37] = true;
                note = getSubfield("a", datafield);
            } else {
                visited[38] = true;
            }

            //edition
            if ("032@".equals(tag)) {
                visited[39] = true;
                edition = getSubfield("a", datafield);
            } else {
                visited[40] = true;
            }

            //isbn
            if ("004A".equals(tag)) {
                visited[41] = true;
                final String isbn10 = getSubfield("0", datafield);
                final String isbn13 = getSubfield("A", datafield);

                if (isbn10 != null) {
                    visited[42] = true;
                    isbn = isbn10;
                } else {
                    visited[43] = true;
                }

                if (isbn13 != null) {
                    visited[44] = true;
                    isbn = isbn13;
                } else {
                    visited[45] = true;
                }

            } else {
                visited[46] = true;
            }

            // Hochschulschriftenvermerk
            // Bei einer Verlagsdissertation ist der Ort schon eingetragen
            if ("037C".equals(tag)) {
                visited[47] = true;
                if (address == null) {
                    visited[48] = true;
                    address = getSubfield("b", datafield);
                    if (address != null) {
                        visited[49] = true;
                        address = removeSortCharacters(address);
                    } else {
                        visited[50] = true;
                    }
                } else {
                    visited[51] = true;
                }

                String st = getSubfield("a", datafield);
                if ((st != null) && st.contains("Diss")) {
                    visited[52] = true;
                    entryType = StandardEntryType.PhdThesis;
                } else {
                    visited[53] = true;
                }
            } else {
                visited[54] = true;
            }

            //journal oder booktitle

            /* Problematiken hier: Sowohl für Artikel in
             * Zeitschriften als für Beiträge in Büchern
             * wird 027D verwendet. Der Titel muß je nach
             * Fall booktitle oder journal zugeordnet
             * werden. Auch bei Zeitschriften werden hier
             * ggf. Verlag und Ort angegeben (sind dann
             * eigentlich überflüssig), während bei
             * Buchbeiträgen Verlag und Ort wichtig sind
             * (sonst in Kategorie 033A).
             */
            if ("027D".equals(tag)) {
                visited[55] = true;
                parseVolumeSetAndEssayData(datafield);
            } else {
                visited[56] = true;
            }

            //pagetotal
            if ("034D".equals(tag)) {
                visited[57] = true;
                parsePhysicalInformationData(datafield);
            } else {
                visited[60] = true;
            }

            // Behandlung von Konferenzen
            if ("030F".equals(tag)) {
                visited[61] = true;
                parseConferenceData(datafield);
            } else {
                visited[64] = true;
            }

            // Wenn eine Verlagsdiss vorliegt
            if (entryType.equals(StandardEntryType.PhdThesis) && (isbn != null)) {
                visited[65] = true;
                entryType = StandardEntryType.Book;
            } else {
                visited[66] = true;
            }

            //Hilfskategorien zur Entscheidung @article
            //oder @incollection; hier könnte man auch die
            //ISBN herausparsen als Erleichterung für das
            //Auffinden der Quelle, die über die
            //SRU-Schnittstelle gelieferten Daten zur
            //Quelle unvollständig sind (z.B. nicht Serie
            //und Nummer angegeben werden)
            if ("039B".equals(tag)) {
                visited[67] = true;
                parseRelationToParentLiteratureData(datafield);
            } else {
                visited[68] = true;
            }

            if ("046R".equals(tag) && ((quelle == null) || quelle.isEmpty())) {
                visited[69] = true;
                parseLiteratureSourceData(datafield);
            } else {
                visited[70] = true;
            }

            // URLs behandeln
            if ("009P".equals(tag) && ("03".equals(datafield.getAttribute("occurrence"))
                    || "05".equals(datafield.getAttribute("occurrence"))) && (url == null)) {
                visited[71] = true;
                parseOnlineResourceData(datafield);
            } else {
                visited[72] = true;
            }
        }
        // if we skipped the for loop completely
        if (SIZEOFDATAFIELDS == 0) {
            visited[73] = true;
        }
        // Abfangen von Nulleintraegen
        if (quelle == null) {
            visited[74] = true;
            quelle = "";
        } else {
            visited[75] = true;
        }

        // Nichtsortierzeichen entfernen
        if (author != null) {
            visited[76] = true;
            author = removeSortCharacters(author);
        } else {
            visited[77] = true;
        }

        if (editor != null) {
            visited[78] = true;
            editor = removeSortCharacters(editor);
        } else {
            visited[79] = true;
        }

        if (title != null) {
            visited[80] = true;
            title = removeSortCharacters(title);
        } else {
            visited[81] = true;
        }

        if (subtitle != null) {
            visited[82] = true;
            subtitle = removeSortCharacters(subtitle);
        } else {
            visited[83] = true;
        }

        // Dokumenttyp bestimmen und Eintrag anlegen

        if (mak.startsWith("As")) {
            visited[84] = true;
            entryType = BibEntry.DEFAULT_TYPE;

            if (quelle.contains("ISBN")) {
                visited[85] = true;
                entryType = StandardEntryType.InCollection;
            } else {
                visited[86] = true;
            }

            if (quelle.contains("ZDB-ID")) {
                visited[87] = true;
                entryType = StandardEntryType.Article;
            } else {
                visited[88] = true;
            }

        } else if (mak.isEmpty()) {
            visited[89] = true;
            entryType = BibEntry.DEFAULT_TYPE;
        } else if (mak.startsWith("O")) {
            visited[90] = true;
            entryType = BibEntry.DEFAULT_TYPE;
            // FIXME: online only available in Biblatex
            //entryType = "online";
        } else {
            visited[91] = true;
        }

        /*
         * Wahrscheinlichkeit, dass ZDB-ID
         * vorhanden ist, ist größer als ISBN bei
         * Buchbeiträgen. Daher bei As?-Sätzen am besten immer
         * dann @incollection annehmen, wenn weder ISBN noch
         * ZDB-ID vorhanden sind.
         */
        BibEntry result = new BibEntry(entryType);

        configureBibEntry(result);

        try {
            File f = new File("/tmp/parseEntry.txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            double frac = 0;
            for(int i = 0; i < visited.length; ++i) {
                frac += (visited[i] ? 1 : 0);
                bw.write("branch " + i + " was " + (visited[i] ? " visited." : " not visited.") + "\n");
            }
            bw.write("" + frac/visited.length);
            bw.close();
        } catch (Exception exc) {
            System.err.println("ye");
        }

        return result;
    }

    /* HELPER FUNCTIONS FOR parseEntry */

    private void parseVolumeSetAndEssayData(Element datafield) {
        journal = getSubfield("a", datafield);
        booktitle = getSubfield("a", datafield);
        address = getSubfield("p", datafield);
        publisher = getSubfield("n", datafield);
    }

    private void parsePhysicalInformationData(Element datafield) {
        pagetotal = getSubfield("a", datafield);

        if (pagetotal != null) {
            // S, S. etc. entfernen
            pagetotal = pagetotal.replaceAll(" S\\.?$", "");
        }
    }

    private void parseConferenceData(Element datafield) {
        address = getSubfield("k", datafield);

        if (!"proceedings".equals(entryType)) {
            subtitle = getSubfield("a", datafield);
        }

        entryType = StandardEntryType.Proceedings;
    }

    private void parseRelationToParentLiteratureData(Element datafield) {
        quelle = getSubfield("8", datafield);
    }
    
    private void parseLiteratureSourceData(Element datafield) {
        quelle = getSubfield("a", datafield);
    }

    private void parseOnlineResourceData(Element datafield) {
        url = getSubfield("a", datafield);
    }

    private void configureBibEntry(BibEntry result) {
        // Zuordnung der Felder in Abhängigkeit vom Dokumenttyp
        if (author != null) {
            result.setField(StandardField.AUTHOR, author);
        }

        if (editor != null) {
            result.setField(StandardField.EDITOR, editor);
        }

        if (title != null) {
            result.setField(StandardField.TITLE, title);
        }

        if (!Strings.isNullOrEmpty(subtitle)) {
            // ensure that first letter is an upper case letter
            // there could be the edge case that the string is only one character long, therefore, this special treatment
            // this is Apache commons lang StringUtils.capitalize (https://commons.apache.org/proper/commons-lang/javadocs/api-release/org/apache/commons/lang3/StringUtils.html#capitalize%28java.lang.String%29), but we don't want to add an additional dependency  ('org.apache.commons:commons-lang3:3.4')
            StringBuilder newSubtitle = new StringBuilder(
                    Character.toString(Character.toUpperCase(subtitle.charAt(0))));
            if (subtitle.length() > 1) {
                newSubtitle.append(subtitle.substring(1));
            }

            result.setField(StandardField.SUBTITLE, newSubtitle.toString());
        }

        if (publisher != null) {
            result.setField(StandardField.PUBLISHER, publisher);
        } 

        if (year != null) {
            result.setField(StandardField.YEAR, year);
        }

        if (address != null) {
            result.setField(StandardField.ADDRESS, address);
        }

        if (series != null) {
            result.setField(StandardField.SERIES, series);
        }

        if (edition != null) {
            result.setField(StandardField.EDITION, edition);
        } 

        if (isbn != null) {
            result.setField(StandardField.ISBN, isbn);
        }

        if (issn != null) {
            result.setField(StandardField.ISSN, issn);
        }

        if (number != null) {
            result.setField(StandardField.NUMBER, number);
        }

        if (pagetotal != null) {
            result.setField(StandardField.PAGETOTAL, pagetotal);
        }

        if (pages != null) {
            result.setField(StandardField.PAGES, pages);
        }

        if (volume != null) {
            result.setField(StandardField.VOLUME, volume);
        }

        if (journal != null) {
            result.setField(StandardField.JOURNAL, journal);
        }

        if (ppn != null) {
            result.setField(new UnknownField("ppn_GVK"), ppn);
        } 

        if (url != null) {
            result.setField(StandardField.URL, url);
        }

        if (note != null) {
            result.setField(StandardField.NOTE, note);
        }

        if ("article".equals(entryType) && (journal != null)) {
            result.setField(StandardField.JOURNAL, journal);
        } else if ("incollection".equals(entryType) && (booktitle != null)) {
            result.setField(StandardField.BOOKTITLE, booktitle);
        }
    }

    private void setBibliographicTypeAndStatus(Element datafield) {
        mak = getSubfield("0", datafield);
        if (mak == null) {
            visited[2] = true;
            mak = "";
        } else {
            visited[3] = true;
        }
    }

    private void setRecordControlNumber(Element datafield) {
        ppn = getSubfield("0", datafield);
    }

    private void setPrimaryauthor(Element datafield) {
        String vorname = getSubfield("d", datafield);
        String nachname = getSubfield("a", datafield);

        if (author == null) {
            visited[8] = true;
            author = "";
        } else {
            visited[9] = true;
            author = author.concat(" and ");
        }
        author = author.concat(vorname + " " + nachname);
    }

    private void setCoauthor(Element datafield) {
        String vorname = getSubfield("d", datafield);
        String nachname = getSubfield("a", datafield);

        if (author == null) {
            visited[12] = true;
            author = "";
        } else {
            visited[13] = true;
            author = author.concat(" and ");
        }
        author = author.concat(vorname + " " + nachname);        
    }

    private void setSecondaryAuthor(Element datafield) {
        String vorname = getSubfield("d", datafield);
        String nachname = getSubfield("a", datafield);

        if (editor == null) {
            visited[16] = true;
            editor = "";
        } else {
            visited[17] = true;
            editor = editor.concat(" and ");
        }
        editor = editor.concat(vorname + " " + nachname);
    }

    private void setLinkToMultiVolumePublication(Element datafield) {
        // 021 might have been present
        if (title != null) {
            visited[28] = true;
            // convert old title (contained in "a" of 021A) to volume
            if (title.startsWith("@")) {
                visited[29] = true;
                // "@" indicates a number
                title = title.substring(1);
            } else {
                visited[30] = true;
            }
            number = title;
        } else {
            visited[31] = true;
        }
        //title and subtitle
        title = getSubfield("a", datafield);
        subtitle = getSubfield("d", datafield);
        volume = getSubfield("l", datafield);
    }    

    private void setNumberingArea(Element datafield) {
        year = getSubfield("j", datafield);
        volume = getSubfield("e", datafield);
        number = getSubfield("a", datafield);
        pages = getSubfield("h", datafield);
    }

    private void setDateOfPublication(Element datafield) {
        year = getSubfield("a", datafield);
    }

    private void setFirstPublisher(Element datafield) {
        publisher = getSubfield("n", datafield);
        address = getSubfield("p", datafield);
    }

    private void setTitleAndStatementOfResponsibilityArea(Element datafield) {
        title = getSubfield("a", datafield);
        subtitle = getSubfield("d", datafield);
    }

    private String getSubfield(String a, Element datafield) {
        List<Element> liste = getChildren("subfield", datafield);

        for (Element subfield : liste) {
            if (subfield.getAttribute("code").equals(a)) {
                return (subfield.getTextContent());
            }
        }
        return null;
    }

    private Element getChild(String name, Element e) {
        if (e == null) {
            return null;
        }
        NodeList children = e.getChildNodes();

        int j = children.getLength();
        for (int i = 0; i < j; i++) {
            Node test = children.item(i);
            if (test.getNodeType() == Node.ELEMENT_NODE) {
                Element entry = (Element) test;
                if (entry.getTagName().equals(name)) {
                    return entry;
                }
            }
        }
        return null;
    }

    private List<Element> getChildren(String name, Element e) {
        List<Element> result = new LinkedList<>();
        NodeList children = e.getChildNodes();

        int j = children.getLength();
        for (int i = 0; i < j; i++) {
            Node test = children.item(i);
            if (test.getNodeType() == Node.ELEMENT_NODE) {
                Element entry = (Element) test;
                if (entry.getTagName().equals(name)) {
                    result.add(entry);
                }
            }
        }

        return result;
    }

    private String removeSortCharacters(String input) {
        return input.replaceAll("\\@", "");
    }

}
