package utilities.lucene;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import utilities.ModelToDocumentTransducer;
import utilities.QueryConfig;
import utilities.QueryConfig.SparqlIndexerQueries;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE;

@Slf4j
public class SparqlIndexer {

    private static int skip = 10000;
    private final String endpoint;
    private final IndexWriter iwriter;
    private final QueryConfig queryInstance;

    public SparqlIndexer(
            String endpoint,
            Directory indexDirectory,
            QueryConfig queryInstance) throws IOException {

        this.endpoint = endpoint;
        this.queryInstance = queryInstance;

        Analyzer analyzer = new SparqlIndexerAnalyzerFactory()
                .getSparqlIndexerAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer)
                .setOpenMode(CREATE);

        iwriter = new IndexWriter(indexDirectory, config);
    }

    public void index() {

        queryInstance.getQueries().forEach(queryConfig -> index(queryConfig));

        try {
            iwriter.commit();
            iwriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void index(SparqlIndexerQueries config) {

        log.info("::: I am using the following SELECT query:\n" + config.getSelectQuery());

        SPARQLRepository repo = new SPARQLRepository(endpoint);
        repo.init();

        try (RepositoryConnection conn = repo.getConnection()) {

            int offset = 0;

            TupleQueryResult ress = null;

            while (ress == null || ress.hasNext()) {

                log.info("Taking " + skip + " tuples..");

                log.debug("Offset: " + offset);
                log.debug("Limit: " + skip);

                if (ress == null) {
                    ress = QueryResults.distinctResults(conn
                            .prepareTupleQuery(increasePagination(config.getSelectQuery(), offset))
                            .evaluate());
                }

                if (ress != null) {

                    List<String> uris = QueryResults.stream(ress)
                            .map(x -> x.getValue(queryInstance.getVariable()).stringValue())
                            .collect(toList());

                    getAndIndex(uris, config.getConstructQuery(), conn);

                }
                offset += skip;
            }

        }
        repo.shutDown();
    }

    private static String increasePagination(String basicQuery, int offset) {
        return basicQuery
                .concat("\noffset ")
                .concat(String.valueOf(offset))
                .concat("\n")
                .concat("limit ")
                .concat(String.valueOf(skip));
    }

    private void getAndIndex(List<String> uris, String constructQueryTemplate, RepositoryConnection conn) {

        uris.forEach(uri -> {

            String constructQuery = constructQueryTemplate
                    .replaceAll("\\?" + queryInstance.getVariable(),
                            "<" + uri.replace("$", "\\$") + ">"
                    );

            log.info("::: Getting the RDF for " + uri);
            Optional<Document> maybeDoc = makeConstruct(uri, constructQuery, conn);

            if (maybeDoc.isPresent()) {
                try {
                    iwriter.addDocument(maybeDoc.get());
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        });
    }

    private Optional<Document> makeConstruct(String uri, String constructQuery, RepositoryConnection conn) {

        GraphQueryResult result = conn.prepareGraphQuery(constructQuery).evaluate();
        Model model = QueryResults.asModel(result);

        return ModelToDocumentTransducer.translate(model, uri, endpoint);

    }

}
