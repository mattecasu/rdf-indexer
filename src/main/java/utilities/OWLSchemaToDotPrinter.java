package utilities;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.UnionClass;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;

public class OWLSchemaToDotPrinter {

  // top-bottom, bottom-top, left-right, rightToLeft
  private final String langTag;

  public enum DIRECTIONS {
    TB,
    BT,
    LR,
    RL
  };

  public enum NAMETYPE {
    QNAME,
    LABEL
  };

  private final String ranking;
  static List<String> superClassesToIgnore = Arrays.asList(RDFS.Resource.getURI());
  private String fileLocation;
  private String toPrint;
  private Set<String> printedClasses = new HashSet<String>();
  private String color;
  private Set<UnionClass> unionClasses = new HashSet<UnionClass>();
  private Set<Pair<String, String>> couples = new HashSet<Pair<String, String>>();
  private NAMETYPE nameType;

  static final OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_RDFS_INF);

  public OWLSchemaToDotPrinter(
      String fileLocation, NAMETYPE nameType, String langTag, DIRECTIONS dir, String color) {
    this.fileLocation = fileLocation;
    this.nameType = nameType;
    this.ranking = dir.toString();
    this.langTag = langTag;
    this.color = color;
  }

  public String getDot(String rdfFormat) {
    toPrint = "digraph g {\n rankdir = " + ranking + ";\n";
    model.read(fileLocation, rdfFormat);
    OntClass thing = model.getOntResource(OWL.Thing).asClass();
    thing.setLabel("Thing", langTag);
    OntClass resource = model.getOntResource(RDFS.Resource).asClass();
    resource.setLabel("Resource", langTag);
    OntClass literal = model.getOntResource(RDFS.Literal).asClass();
    literal.setLabel("Literal", langTag);

    // properties
    ExtendedIterator<OntProperty> properties = model.listAllOntProperties();

    while (properties.hasNext()) {
      OntProperty property = properties.next();
      OntResource domain = property.getDomain();
      OntResource range = property.getRange();

      if (domain == null)
        if (property.isObjectProperty()) domain = thing;
        else domain = resource;

      if (range == null)
        if (property.isObjectProperty()) range = thing;
        else if (property.isAnnotationProperty()) range = literal;
        else range = resource;

      String domainLabel = null;
      String rangeLabel = null;

      if (domain.asClass().isUnionClass()) {
        domainLabel = getUnionLabel(domain);
        unionClasses.add(domain.asClass().asUnionClass());
      } else {
        domainLabel = getNameOrLabel(domain);
      }

      if (range.asClass().isUnionClass()) {
        rangeLabel = getUnionLabel(range);
        unionClasses.add(range.asClass().asUnionClass());
      } else if (property.isDatatypeProperty()) rangeLabel = range.getLocalName();
      else rangeLabel = getNameOrLabel(range);

      if (property.isObjectProperty()) {
        if (domainLabel != null && rangeLabel != null) {
          String toAdd =
              "\""
                  + domainLabel
                  + "\""
                  + "->"
                  + "\""
                  + rangeLabel
                  + "\""
                  + " [label=\""
                  + getNameOrLabel(property)
                  + "\"]\n";
          toPrint += toAdd;
          printedClasses.add(domainLabel);
          printedClasses.add(rangeLabel);
        }
      } else if (property.isDatatypeProperty() || property.isAnnotationProperty()) {
        if (domainLabel != null && rangeLabel != null) {
          String toAdd =
              "\""
                  + domainLabel
                  + "\""
                  + "->"
                  + "\""
                  + getNameOrLabel(property)
                  + " ("
                  + rangeLabel
                  + ")"
                  + "\" ["
                  + "arrowhead=dot]\n";
          toAdd +=
              "\""
                  + getNameOrLabel(property)
                  + " ("
                  + rangeLabel
                  + ")"
                  + "\""
                  + " [shape=plaintext]\n";
          toPrint += toAdd;
          printedClasses.add(domainLabel);
          // printedClasses.add(rangeLabel);
        }
      }
    }

    // recursively print subclasses of root
    toPrint += "// subclasses\n";
    recursivePrint(thing);
    // print union classes subclassing
    for (UnionClass cl : unionClasses) {
      for (OntClass operand : cl.listOperands().toList()) {
        String operandLabel = getNameOrLabel(operand);
        String unionClassLabel = getUnionLabel(cl);
        Pair<String, String> couple =
            new ImmutablePair<String, String>(operandLabel, unionClassLabel);
        couples.add(couple);
      }
    }
    for (Pair<String, String> couple : couples) {
      toPrint +=
          String.format("\"%s\" -> \"%s\" [style=dotted]\n", couple.getLeft(), couple.getRight());
      printedClasses.add(couple.getLeft());
      printedClasses.add(couple.getRight());
    }
    // class styles
    toPrint += "// class styles\n";
    for (String cl : printedClasses)
      toPrint += String.format("\"%s\" [color=\"%s\", style=\"filled\"]\n", cl, color);
    toPrint += "}";
    return toPrint;
  }

  private String getUnionLabel(OntResource domainOrRange) {
    UnionClass unionClass = domainOrRange.asClass().asUnionClass();
    List<? extends OntClass> operands = unionClass.listOperands().toList();
    ArrayList<String> newOperands = new ArrayList<String>();
    for (OntClass c : operands) {
      newOperands.add(getNameOrLabel(c));
    }
    return StringUtils.join(newOperands, " OR ");
  }

  private String getNameOrLabel(OntResource c) {
    String qname = "null";
    if (nameType == NAMETYPE.LABEL) return c.getLabel(langTag);
    else if (nameType == NAMETYPE.QNAME) {
      return model.qnameFor(c.getURI()).toString();
    } else return qname;
  }

  private void recursivePrint(OntClass current) {
    ExtendedIterator<OntClass> superClasses = current.listSuperClasses(true);
    while (superClasses.hasNext()) {
      OntClass superClass = superClasses.next();
      if (superClassesToIgnore.contains(superClass.getURI())) continue;
      if (superClass.isUnionClass()) {
        String toAdd =
            "\""
                + getNameOrLabel(current)
                + "\" -> \""
                + getUnionLabel(superClass)
                + "\" [style=dotted]\n";
        toPrint += toAdd;
        printedClasses.add(getNameOrLabel(current));
        printedClasses.add(getUnionLabel(superClass));
        Pair<String, String> couple =
            new ImmutablePair<String, String>(getNameOrLabel(current), getUnionLabel(superClass));
        couples.add(couple);
      } else {
        String toAdd =
            "\""
                + getNameOrLabel(current)
                + "\" -> \""
                + getNameOrLabel(superClass)
                + "\" [style=dotted]\n";
        toPrint += toAdd;
        printedClasses.add(getNameOrLabel(current));
        printedClasses.add(getNameOrLabel(superClass));
      }
    }
    ExtendedIterator<OntClass> subClasses = current.listSubClasses(true);
    if (subClasses.hasNext()) {
      while (subClasses.hasNext()) {
        OntClass child = subClasses.next();
        recursivePrint(child);
      }
    } else {

    }
  }

  public static void main(String... args) throws IOException {

    String folder = "/Users/epi/Desktop/SI/Platts/";
    String inFile = folder + "platts_model.ttl";
    // String outFile = inFile + ".dot";
    String outFile = folder + "plattsModel.dot";

    OWLSchemaToDotPrinter printer =
        new OWLSchemaToDotPrinter(
            "file://" + inFile, NAMETYPE.LABEL, "en", DIRECTIONS.LR, "#FFCC99");
    String dot = printer.getDot(RDFFormat.TURTLE.toString());
    FileWriter writer = new FileWriter(outFile);
    writer.write(dot);
    writer.close();
  }
}
