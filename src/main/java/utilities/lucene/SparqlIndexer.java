package utilities.lucene;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;

import utilities.QueryExecutor;
import utilities.SparqlIndexerQueryInstance;
import utilities.SparqlIndexerQueryInstance.SparqlIndexerQueries;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SparqlIndexer {

    private static final Integer PAGINATION_LIMIT = 10000;

    private IndexWriter iwriter;
    private final SparqlIndexerQueryInstance queryInstance;

    private final String variable;

    private final QueryExecutor queryExecutor;

    public SparqlIndexer(
            String endpoint,
            Directory indexDirectory,
            SparqlIndexerQueryInstance queryInstance,
            QueryExecutor queryExecutor) throws IOException {

        this.queryExecutor = queryExecutor;
        Analyzer analyzer = new SparqlIndexerAnalyzerFactory().getSparqlIndexerAnalyzer();

        IndexWriterConfig config = new IndexWriterConfig(analyzer).setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        try {
            iwriter = new IndexWriter(indexDirectory, config);
        } catch (IOException e1) {
            log.error(e1.getMessage());
        }

        this.queryInstance = queryInstance;
        variable = queryInstance.getVariable();
    }

    public void index() {
        queryInstance.getQueries().forEach(queryConfig -> makeQueries(queryConfig));
        try {
            iwriter.commit();
            iwriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void makeQueries(SparqlIndexerQueries config) {

        Optional<String> countQuery = config.getCountQuery();

        if (countQuery.isPresent()) {
            Integer count = queryExecutor.makeCount(countQuery.get());
            Integer offset = 0;

            while (count > 0) {
                String selectQuery = config.getSelectQuery();
                String limitedSelect = selectQuery;
                if (!selectQuery.contains("LIMIT ") && !selectQuery.contains("limit ")) {
                    limitedSelect = selectQuery + "\nOFFSET " + offset + "\nLIMIT " + PAGINATION_LIMIT;
                }
                List<String> uris = queryExecutor.makeSelect(limitedSelect);
                getAndIndex(uris, config.getConstructQuery());
                offset += PAGINATION_LIMIT;
                count = count - PAGINATION_LIMIT;
            }
        } else {
            List<String> uris = queryExecutor.makeSelect(config.getSelectQuery());
            getAndIndex(uris, config.getConstructQuery());
        }

    }


    private void getAndIndex(List<String> uris, String constructQueryTemplate) {

        uris.forEach(uri -> {
            String constructQuery = constructQueryTemplate.replaceAll("\\?" + variable, "<" + uri.replace("$", "\\$") + ">");
            log.info("::: Getting the RDF for " + uri);
            Optional<Document> maybeDoc = queryExecutor.makeConstruct(uri, constructQuery);

            if (maybeDoc.isPresent()) {
                try {
                    iwriter.addDocument(maybeDoc.get());
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        });
    }

}
