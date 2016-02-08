package utilities;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.store.SimpleFSDirectory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import utilities.lucene.SparqlIndexer;

public class SparqlIndexerTest {

    private SparqlIndexer jenaIndexer;
    private SparqlIndexer sesameIndexer;
    private static final String endpoint = "http://dbpedia.org/sparql";
    private static final String sparqlIndexerPathJena = "/Users/epi/Desktop/jenaSparqlIndexerIndex";
    private static final String sparqlIndexerPathSesame = "/Users/epi/Desktop/sesameSparqlIndexerIndex";
    private static SparqlIndexerQueryInstance queryInstance;
    private QueryExecutor jenaExecutor;
    private QueryExecutor sesameExecutor;

    @Before
    public void setup() throws IOException {
        queryInstance = new SparqlIndexerQueryInstance(new ClassPathResource("queries.properties"), endpoint);
        jenaExecutor = new JenaQueryExecutor(queryInstance.getEndpoint(), queryInstance.getVariable());
        sesameExecutor = new SesameQueryExecutor(queryInstance.getEndpoint(), queryInstance.getVariable());

        jenaIndexer = new SparqlIndexer(
                queryInstance.getEndpoint(),
                new SimpleFSDirectory(Paths.get(sparqlIndexerPathJena)),
                queryInstance,
                jenaExecutor);
        sesameIndexer = new SparqlIndexer(
                queryInstance.getEndpoint(),
                new SimpleFSDirectory(Paths.get(sparqlIndexerPathSesame)),
                queryInstance,
                sesameExecutor);

    }

    @Test
    public void test() {
        jenaIndexer.index();
        sesameIndexer.index();
    }

}
