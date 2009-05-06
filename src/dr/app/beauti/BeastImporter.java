package dr.app.beauti;

import dr.evolution.alignment.Alignment;
import dr.evolution.alignment.SimpleAlignment;
import dr.evolution.io.Importer;
import dr.evolution.sequence.Sequence;
import dr.evolution.util.*;
import dr.evoxml.AlignmentParser;
import dr.evoxml.TaxaParser;
import dr.evoxml.TaxonParser;
import dr.xml.XMLParser;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public class BeastImporter {

    private final Element root;
    private final DateFormat dateFormat;
    private final java.util.Date origin;

    public BeastImporter(Reader reader) throws IOException, JDOMException, Importer.ImportException {
        SAXBuilder builder = new SAXBuilder();

        Document doc = builder.build(reader);
        root = doc.getRootElement();
        if (!root.getName().equalsIgnoreCase("beast")) {
            throw new Importer.ImportException("Unrecognized root element in XML file");
        }

        dateFormat = DateFormat.getDateInstance(java.text.DateFormat.SHORT, Locale.UK);
        dateFormat.setLenient(true);

        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.set(0000, 01, 01);
        origin = cal.getTime();

    }

    public void importBEAST(List<TaxonList> taxonLists, List<Alignment> alignments) throws Importer.ImportException {

        TaxonList taxa = null;

        List children = root.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Element child = (Element)children.get(i);

            if (child.getName().equalsIgnoreCase(TaxaParser.TAXA)) {
                if (taxa == null) {
                    taxa = readTaxa(child);
                    taxonLists.add(taxa);
                } else {
                    taxonLists.add(readTaxa(child));
                }
            } else  if (child.getName().equalsIgnoreCase(AlignmentParser.ALIGNMENT)) {
                if (taxa == null) {
                    throw new Importer.ImportException("taxa not defined");
                }
                alignments.add(readAlignment(child, taxa));
            }
        }
    }

    private TaxonList readTaxa(Element e) throws Importer.ImportException {
        Taxa taxa = new Taxa();

        List children = e.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Element child = (Element)children.get(i);

            if (child.getName().equalsIgnoreCase(TaxonParser.TAXON)) {
                taxa.addTaxon(readTaxon(child));
            }
        }
        return taxa;
    }

    private Taxon readTaxon(Element e) throws Importer.ImportException {

        String id = e.getAttributeValue(XMLParser.ID);

        Taxon taxon = new Taxon(id);

        List children = e.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Element child = (Element)children.get(i);

            if (child.getName().equalsIgnoreCase("date")) {
                Date date = readDate(child);
                taxon.setAttribute("date", date);
            } else if (child.getName().equalsIgnoreCase("attr")) {
                String name = e.getAttributeValue("name");
                String value = e.getAttributeValue("value");
                taxon.setAttribute(name, value);
            }
        }
        return taxon;
    }

    private Alignment readAlignment(Element e, TaxonList taxa) throws Importer.ImportException {
        SimpleAlignment alignment = new SimpleAlignment();

        List children = e.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Element child = (Element)children.get(i);

            if (child.getName().equalsIgnoreCase("sequence")) {
                alignment.addSequence(readSequence(child, taxa));
            }
        }
        return alignment;
    }

    private Sequence readSequence(Element e, TaxonList taxa) throws Importer.ImportException {

        String taxonID = e.getChild(TaxonParser.TAXON).getAttributeValue(XMLParser.IDREF);
        int index = taxa.getTaxonIndex(taxonID);
        if (index < 0) {
            throw new Importer.ImportException("Unknown taxon, " + taxonID + ", in alignment");
        }
        Taxon taxon = taxa.getTaxon(index);

        String seq = e.getTextTrim();
        Sequence sequence = new Sequence(taxon, seq);

        return sequence;
    }

    private Date readDate(Element e) throws Importer.ImportException {

        String value = e.getAttributeValue("value");
        boolean backwards = true;
        String direction = e.getAttributeValue("direction");
        if (direction != null && direction.equalsIgnoreCase("forwards")) {
            backwards = false;
        }
        try {
            return new Date(dateFormat.parse(value), Units.Type.YEARS, origin);
        } catch (ParseException e1) {
            e1.printStackTrace();
        }
        try {
            return new Date(dateFormat.parse(value), Units.Type.YEARS, origin);
        } catch (ParseException e1) {

            // do nothing
        }

        // try just parsing it as a number
        return new Date(Double.valueOf(value), Units.Type.YEARS, backwards);

    }


}
