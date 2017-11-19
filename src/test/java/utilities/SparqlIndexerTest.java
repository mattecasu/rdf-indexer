package utilities;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.store.SimpleFSDirectory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import utilities.lucene.SparqlIndexer;

public class SparqlIndexerTest {

    private SparqlIndexer sesameIndexer;
    private static final String endpoint = "http://dbpedia.org/sparql";
    private static final String sparqlIndexerPathSesame = "/Users/epi/Desktop/sesameSparqlIndexerIndex";
    private QueryConfig queryInstance;

    @Before
    public void setup() throws IOException {

        queryInstance = new QueryConfig(new ClassPathResource("queries.properties"), endpoint);

        sesameIndexer = new SparqlIndexer(
                queryInstance.getEndpoint(),
                new SimpleFSDirectory(Paths.get(sparqlIndexerPathSesame)),
                queryInstance);
    }

    @Test
    public void test() {
        sesameIndexer.index();
    }

}
