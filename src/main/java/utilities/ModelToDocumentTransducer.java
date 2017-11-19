package utilities;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.Optional;

import static java.util.Optional.empty;

public class ModelToDocumentTransducer {

    public static Optional<Document> translate(Model model, String uri, String endpoint) {

        ValueFactory vf = SimpleValueFactory.getInstance();

        if (model.isEmpty()) {
            return Optional.empty();
        }

        Document doc = new Document();
        doc.add(new Field("rdfUri", uri, StringField.TYPE_STORED));
        doc.add(new Field("endpoint", endpoint, TextField.TYPE_STORED));

        model
                .filter(vf.createIRI(uri), null, null)
                .stream()
                .forEach(st -> {
                    String prop = st.getPredicate().stringValue();
                    Value obj = st.getObject();
                    Optional<String> langPart = empty();
                    if (Literals.canCreateLiteral(obj) && ((Literal) obj).getDatatype().equals(RDF.LANGSTRING)) {
                        String lang = ((Literal) obj).getLanguage().orElse("");
                        langPart = lang.isEmpty() ? Optional.of("_xx") : Optional.of("_" + lang);
                    }

                    doc.add(
                            new Field(vf.createIRI(prop).getLocalName() + langPart.orElse(""), obj.stringValue(),
                                    TextField.TYPE_STORED));
                });

        return Optional.of(doc);
    }

}
