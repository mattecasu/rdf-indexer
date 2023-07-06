package utilities;

import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.util.Values;

@Slf4j
public class ModelToDocumentTransducer {

  public static Optional<Document> translate(Model model, String uri, String endpoint) {

    if (model.isEmpty()) {
      return Optional.empty();
    }

    Document doc = new Document();
    doc.add(new Field("rdfUri", uri, StringField.TYPE_STORED));
    doc.add(new Field("endpoint", endpoint, TextField.TYPE_STORED));

    model
        .filter(Values.iri(uri), null, null)
        .forEach(
            st -> {
              String prop = st.getPredicate().getLocalName();
              Value obj = st.getObject();

              Optional<String> langPart = empty();
              if (obj instanceof Literal && Literals.isLanguageLiteral((Literal) obj)) {
                String lang = ((Literal) obj).getLanguage().get();
                langPart = lang.isEmpty() ? Optional.of("_xx") : Optional.of("_" + lang);
              }

              doc.add(
                  new Field(
                      prop + langPart.orElse(EMPTY), obj.stringValue(), TextField.TYPE_STORED));
            });

    return Optional.of(doc);
  }
}
