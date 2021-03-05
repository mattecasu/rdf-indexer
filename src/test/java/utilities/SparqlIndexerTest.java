package utilities;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.store.NIOFSDirectory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import utilities.lucene.SparqlIndexer;

import java.io.IOException;
import java.nio.file.Paths;

@Slf4j
public class SparqlIndexerTest {

    private SparqlIndexer sesameIndexer;
    private static final String endpoint = "http://dbpedia.org/sparql";
    private static final String sparqlIndexerPathSesame = System.getProperty("user.home") + "/Desktop/sparqlIndexerIndex";
    private QueryConfig queryInstance;

    @Before
    public void setup() throws IOException {

        queryInstance = new QueryConfig(new ClassPathResource("queries.properties"), endpoint);

        sesameIndexer = new SparqlIndexer(
                queryInstance.getEndpoint(),
                new NIOFSDirectory(Paths.get(sparqlIndexerPathSesame)),
                queryInstance);
    }

    @Test
    public void test() {
        sesameIndexer.index();
    }

}
