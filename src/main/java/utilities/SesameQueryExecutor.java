package utilities;

import static java.lang.Integer.parseInt;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.openrdf.query.QueryResults.singleResult;

import java.util.List;
import java.util.Optional;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.util.Literals;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryResults;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.sparql.SPARQLRepository;

import lombok.extern.log4j.Log4j;

@Log4j
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
                        String lang = ((Literal)obj).getLanguage().orElse("");
                        langPart = lang.isEmpty() ? Optional.of("_xx") : Optional.of("_" + lang);
                    }

                    doc.add(
                            new Field(f.createIRI(prop).getLocalName() + langPart.orElse(""), obj.stringValue(),
                                    TextField.TYPE_STORED));
                });

        return Optional.of(doc);
    }


}
