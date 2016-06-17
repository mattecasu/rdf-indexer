package utilities;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.jena.query.QueryExecutionFactory.sparqlService;
import static org.apache.jena.shared.PrefixMapping.Extended;

@Slf4j
public class JenaQueryExecutor implements QueryExecutor {

    private String endpoint;
    private String variable;

    public JenaQueryExecutor(String endpoint, String variable) {
        this.endpoint = endpoint;
        this.variable = variable;
    }

    @Override
    public Integer makeCount(String countQuery) {
        log.info("::: I am using the following COUNT query:\n" + countQuery);
        QueryExecution service = sparqlService(endpoint, countQuery);
        QuerySolution qs = service.execSelect().next();
        String var = qs.varNames().next();
        Integer count = qs.getLiteral(var).getInt();
        service.close();
        return count;
    }

    @Override
    public List<String> makeSelect(String limitedSelect) {
        log.info("::: I am using the following (limited) SELECT query:\n" + limitedSelect);
        QueryExecution service = sparqlService(endpoint, limitedSelect);

        List<QuerySolution> solutions = newArrayList();
        service.execSelect().forEachRemaining(s -> solutions.add(s));

        List<String> uris = solutions.stream()
                .map(s -> s.getResource(variable).getURI())
                .collect(Collectors.toList());

        service.close();
        return uris;
    }

    @Override
    public Optional<Document> makeConstruct(String uri, String constructQuery) {
        QueryExecution service;
        try {
            service = sparqlService(endpoint, constructQuery);
        } catch (final Exception c) {
            log.error("::: Problems in executing query " + constructQuery);
            return Optional.empty();
        }
        Model model;
        try {
            model = service.execConstruct();
        } catch (final Exception c) {
            log.error("::: Problems in getting the RDF: " + c.getMessage());
            return Optional.empty();
        }
        if (model.isEmpty()) {
            log.info("::: Empty RDF for " + uri);
            return Optional.empty();
        }
        Resource res = model.createResource(uri);

        Document doc = new Document();

        doc.add(new Field("rdfUri", uri, StringField.TYPE_STORED));
        doc.add(new Field("endpoint", endpoint, TextField.TYPE_STORED));

        model.listStatements(res, null, (RDFNode) null)
                .forEachRemaining(s -> {
                    String prop = s.getPredicate().getURI();
                    RDFNode obj = s.getObject();
                    Optional<String> langPart = empty();
                    if (obj.isLiteral() && obj.asLiteral().getDatatypeURI().equals(RDF.langString.getURI())) {
                        String lang = obj.asLiteral().getLanguage();
                        langPart = lang.isEmpty() ? Optional.of("_xx") : Optional.of("_" + lang);
                    }

                    doc.add(
                            new Field(abbreviateUri(prop) + langPart.orElse(""), obj.asNode().toString(),
                                    TextField.TYPE_STORED));
                });

        model.close();
        service.close();

        return Optional.of(doc);
    }

    private String abbreviateUri(String uri) {
        return ofNullable(Extended.qnameFor(uri)).map(x -> x.replace(":", "__")).orElse(uri);
    }

}
