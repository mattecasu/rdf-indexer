package utilities;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

import java.util.List;
import java.util.Optional;

import static java.lang.Integer.parseInt;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.eclipse.rdf4j.query.QueryResults.singleResult;

@Slf4j
public class SesameQueryExecutor implements QueryExecutor {

    private String endpoint;
    private SPARQLRepository repo;
    private String variable;

    public SesameQueryExecutor(String endpoint, String variable) {
        this.endpoint = endpoint;
        repo = new SPARQLRepository(endpoint);
        repo.initialize();
        this.variable = variable;
    }

    @Override
    public Integer makeCount(String countQuery) {
        TupleQueryResult count = repo.getConnection().prepareTupleQuery(countQuery).evaluate();
        String var = count.getBindingNames().iterator().next();
        return parseInt(singleResult(count).getValue(var).stringValue());
    }

    @Override
    public List<String> makeSelect(String limitedSelect) {
        log.info("::: I am using the following (limited) SELECT query:\n" + limitedSelect);
        TupleQueryResult select = repo.getConnection().prepareTupleQuery(limitedSelect).evaluate();
        return QueryResults.stream(select)
                .map(x -> x.getValue(variable).stringValue())
                .collect(toList());
    }

    @Override
    public Optional<Document> makeConstruct(String uri, String constructQuery) {
        GraphQueryResult result = repo.getConnection().prepareGraphQuery(constructQuery).evaluate();
        Model model = QueryResults.asModel(result);
        ValueFactory f = repo.getValueFactory();

        if (model.isEmpty()) {
            return Optional.empty();
        }

        Document doc = new Document();
        doc.add(new Field("rdfUri", uri, StringField.TYPE_STORED));
        doc.add(new Field("endpoint", endpoint, TextField.TYPE_STORED));

        model
                .filter(f.createIRI(uri), null, null)
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
                            new Field(f.createIRI(prop).getLocalName() + langPart.orElse(""), obj.stringValue(),
                                    TextField.TYPE_STORED));
                });

        return Optional.of(doc);
    }


}
