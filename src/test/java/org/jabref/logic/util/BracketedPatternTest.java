package org.jabref.logic.util;

import org.jabref.logic.bibtexkeypattern.BracketedPattern;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BracketedPatternTest {

    private BibEntry bibentry;
    private BibDatabase database;
    private BibEntry dbentry;

    @BeforeEach
    void setUp() throws Exception {
        bibentry = new BibEntry();
        bibentry.setField(StandardField.AUTHOR, "O. Kitsune");
        bibentry.setField(StandardField.YEAR, "2017");
        bibentry.setField(StandardField.PAGES, "213--216");

        dbentry = new BibEntry();
        dbentry.setType(StandardEntryType.Article);
        dbentry.setCiteKey("HipKro03");
        dbentry.setField(StandardField.AUTHOR, "Eric von Hippel and Georg von Krogh");
        dbentry.setField(StandardField.TITLE, "Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science");
        dbentry.setField(StandardField.JOURNAL, "Organization Science");
        dbentry.setField(StandardField.YEAR, "2003");
        dbentry.setField(StandardField.VOLUME, "14");
        dbentry.setField(StandardField.PAGES, "209--223");
        dbentry.setField(StandardField.NUMBER, "2");
        dbentry.setField(StandardField.ADDRESS, "Institute for Operations Research and the Management Sciences (INFORMS), Linthicum, Maryland, USA");
        dbentry.setField(StandardField.DOI, "http://dx.doi.org/10.1287/orsc.14.2.209.14992");
        dbentry.setField(StandardField.ISSN, "1526-5455");
        dbentry.setField(StandardField.PUBLISHER, "INFORMS");

        database = new BibDatabase();
        database.insertEntry(dbentry);
    }

    @Test
    void bibentryExpansionTest() {
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage]");
        assertEquals("2017_Kitsune_213", pattern.expand(bibentry));
    }

    @Test
    void nullDatabaseExpansionTest() {
        BibDatabase another_database = null;
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage]");
        assertEquals("2017_Kitsune_213", pattern.expand(bibentry, another_database));
    }

    @Test
    void pureauthReturnsAuthorIfEditorIsAbsent() {
        BibDatabase emptyDatabase = new BibDatabase();
        BracketedPattern pattern = new BracketedPattern("[pureauth]");
        assertEquals("Kitsune", pattern.expand(bibentry, emptyDatabase));
    }

    @Test
    void pureauthReturnsAuthorIfEditorIsPresent() {
        BibDatabase emptyDatabase = new BibDatabase();
        BracketedPattern pattern = new BracketedPattern("[pureauth]");
        bibentry.setField(StandardField.EDITOR, "Editorlastname, Editorfirstname");
        assertEquals("Kitsune", pattern.expand(bibentry, emptyDatabase));
    }

    @Test
    void pureauthReturnsEmptyStringIfAuthorIsAbsent() {
        BibDatabase emptyDatabase = new BibDatabase();
        BracketedPattern pattern = new BracketedPattern("[pureauth]");
        bibentry.clearField(StandardField.AUTHOR);
        assertEquals("", pattern.expand(bibentry, emptyDatabase));
    }

    @Test
    void pureauthReturnsEmptyStringIfAuthorIsAbsentAndEditorIsPresent() {
        BibDatabase emptyDatabase = new BibDatabase();
        BracketedPattern pattern = new BracketedPattern("[pureauth]");
        bibentry.clearField(StandardField.AUTHOR);
        bibentry.setField(StandardField.EDITOR, "Editorlastname, Editorfirstname");
        assertEquals("", pattern.expand(bibentry, emptyDatabase));
    }

    @Test
    void emptyDatabaseExpansionTest() {
        BibDatabase another_database = new BibDatabase();
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage]");
        assertEquals("2017_Kitsune_213", pattern.expand(bibentry, another_database));
    }

    @Test
    void databaseWithStringsExpansionTest() {
        BibDatabase another_database = new BibDatabase();
        BibtexString string = new BibtexString("sgr", "Saulius Gražulis");
        another_database.addString(string);
        bibentry = new BibEntry();
        bibentry.setField(StandardField.AUTHOR, "#sgr#");
        bibentry.setField(StandardField.YEAR, "2017");
        bibentry.setField(StandardField.PAGES, "213--216");
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage]");
        assertEquals("2017_Gražulis_213", pattern.expand(bibentry,
                another_database));
    }

    @Test
    void unbalancedBracketsExpandToSomething() {
        BracketedPattern pattern = new BracketedPattern("[year]_[auth_[firstpage]");
        assertNotEquals("", pattern.expand(bibentry));
    }

    @Test
    void unbalancedLastBracketExpandsToSomething() {
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage");
        assertNotEquals("", pattern.expand(bibentry));
    }

    @Test
    void entryTypeExpansionTest() {
        BracketedPattern pattern = new BracketedPattern("[entrytype]:[year]_[auth]_[pages]");
        assertEquals("Misc:2017_Kitsune_213--216", pattern.expand(bibentry));
    }

    @Test
    void entryTypeExpansionLowercaseTest() {
        BracketedPattern pattern = new BracketedPattern("[entrytype:lower]:[year]_[auth]_[firstpage]");
        assertEquals("misc:2017_Kitsune_213", pattern.expand(bibentry));
    }

    @Test
    void suppliedBibentryBracketExpansionTest() {
        BibDatabase another_database = null;
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage]");
        BibEntry another_bibentry = new BibEntry();
        another_bibentry.setField(StandardField.AUTHOR, "Gražulis, Saulius");
        another_bibentry.setField(StandardField.YEAR, "2017");
        another_bibentry.setField(StandardField.PAGES, "213--216");
        assertEquals("2017_Gražulis_213", pattern.expand(another_bibentry, ';', another_database));
    }

    @Test
    void nullBibentryBracketExpansionTest() {
        BibDatabase another_database = null;
        BibEntry another_bibentry = null;
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage]");
        assertThrows(NullPointerException.class, () -> pattern.expand(another_bibentry, ';', another_database));
    }

    @Test
    void bracketedExpressionDefaultConstructorTest() {
        BibDatabase another_database = null;
        BracketedPattern pattern = new BracketedPattern();
        assertThrows(NullPointerException.class, () -> pattern.expand(bibentry, ';', another_database));
    }

    @Test
    void unknownKeyExpandsToEmptyString() {
        assertEquals("", BracketedPattern.expandBrackets("[unknownkey]", ';', dbentry, database));
    }

    @Test
    void emptyPatternAndEmptyModifierExpandsToEmptyString() {
        assertEquals("", BracketedPattern.expandBrackets("[:]", ';', dbentry, database));
    }

    @Test
    void emptyPatternAndValidModifierExpandsToEmptyString() {
        Character separator = ';';
        assertEquals("", BracketedPattern.expandBrackets("[:lower]", separator, dbentry, database));
    }

    @Test
    void bibtexkeyPatternExpandsToBibTeXKey() {
        Character separator = ';';
        assertEquals("HipKro03", BracketedPattern.expandBrackets("[bibtexkey]", separator, dbentry, database));
    }

    @Test
    void bibtexkeyPatternWithEmptyModifierExpandsToBibTeXKey() {
        assertEquals("HipKro03", BracketedPattern.expandBrackets("[bibtexkey:]", ';', dbentry, database));
    }

    @Test
    void authorPatternTreatsVonNamePrefixCorrectly() {
        assertEquals("Eric von Hippel and Georg von Krogh",
                BracketedPattern.expandBrackets("[author]", ';', dbentry, database));
    }

    @Test
    void lowerFormatterWorksOnVonNamePrefixes() {
        assertEquals("eric von hippel and georg von krogh",
                BracketedPattern.expandBrackets("[author:lower]", ';', dbentry, database));
    }

    @Test
    void testResolvedFieldAndFormat() {
        BibEntry child = new BibEntry();
        child.setField(StandardField.CROSSREF, "HipKro03");
        database.insertEntry(child);

        Character separator = ';';
        assertEquals("Eric von Hippel and Georg von Krogh",
                BracketedPattern.expandBrackets("[author]", separator, child, database));

        assertEquals("", BracketedPattern.expandBrackets("[unknownkey]", separator, child, database));

        assertEquals("", BracketedPattern.expandBrackets("[:]", separator, child, database));

        assertEquals("", BracketedPattern.expandBrackets("[:lower]", separator, child, database));

        assertEquals("eric von hippel and georg von krogh",
                BracketedPattern.expandBrackets("[author:lower]", separator, child, database));

        // the bibtexkey is not inherited
        assertEquals("", BracketedPattern.expandBrackets("[bibtexkey]", separator, child, database));

        assertEquals("", BracketedPattern.expandBrackets("[bibtexkey:]", separator, child, database));
    }

    @Test
    void testResolvedParentNotInDatabase() {
        BibEntry child = new BibEntry();
        child.setField(StandardField.CROSSREF, "HipKro03");
        database.removeEntry(dbentry);
        database.insertEntry(child);

        assertEquals("", BracketedPattern.expandBrackets("[author]", ';', child, database));
    }

    @Test
    void regularExpressionReplace() {
        assertEquals("2003-JabRef Science",
                BracketedPattern.expandBrackets("[year]-[journal:regex(\"Organization\",\"JabRef\")]", ';', dbentry, database));
    }

    /**
     * This function tests that the expandBrackets() method returns the expected value if the database entered is equal to null.
     * The function will call the getFieldValue() method and thus improve its branch coverage since it will visit a new branch.
     */
    @Test
    void testIfDatabaseNull() {
        BibEntry child = new BibEntry();
        assertEquals("", BracketedPattern.expandBrackets("[author]", ';', child, null));
    }

    /**
     * This function tests that the expandBrackets() method returns the expected value if the pattern entered is equal to [authForeIni].
     * The method should return the initial of the authors first name.
     * The function will call the getFieldValue() method and thus improve its branch coverage since it will visit a new branch.
     */
    @Test
    void testAuthForeIniPattern() {
        BibDatabase db = new BibDatabase();
        BibEntry bibEntry = new BibEntry();
        bibEntry.setField(StandardField.AUTHOR, "Rosquist, Christine");
        assertEquals("C", BracketedPattern.expandBrackets("[authForeIni]", ';', bibEntry, db));
    }

    /**
     * This function tests that the expandBrackets() method returns the expected value if the pattern entered is equal to [authFirstFull].
     * The method should return the von part and the last name of the first author.
     * The function will call the getFieldValue() method and thus improve its branch coverage since it will visit a new branch.
     */
    @Test
    void testAuthFirstFullPattern() {
        BibDatabase db = new BibDatabase();
        BibEntry bibEntry = new BibEntry();
        bibEntry.setField(StandardField.AUTHOR, "Von Rosquist, Christine");
        assertEquals("Von Rosquist", BracketedPattern.expandBrackets("[authFirstFull]", ';', bibEntry, db));
    }

    /**
     * This function tests that the expandBrackets() method returns the expected value if the pattern entered is equal to [authors].
     * The method should return the last name of the author/authors.
     * The function will call the getFieldValue() method and thus improve its branch coverage since it will visit a new branch.
     */
    @Test
    void testAuthorsPattern() {
        BibDatabase db = new BibDatabase();
        BibEntry bibEntry = new BibEntry();
        bibEntry.setField(StandardField.AUTHOR, "Anka, Kalle");
        assertEquals("Anka", BracketedPattern.expandBrackets("[authors]", ';', bibEntry, db));
    }

}
