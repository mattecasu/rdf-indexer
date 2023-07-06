package utilities;

import java.io.IOException;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.store.NIOFSDirectory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import utilities.lucene.SparqlIndexer;

@Slf4j
public class SparqlIndexerTest {

  private SparqlIndexer sesameIndexer;
  private static final String endpoint = "http://dbpedia.org/sparql";
  private static final String sparqlIndexerPathSesame =
      System.getProperty("user.home") + "/Desktop/sparqlIndexerIndex";
  private QueryConfig queryInstance;

  @BeforeAll
  public void setup() throws IOException {

    queryInstance = new QueryConfig(new ClassPathResource("queries.properties"), endpoint);

    sesameIndexer =
        new SparqlIndexer(
            queryInstance.getEndpoint(),
            new NIOFSDirectory(Paths.get(sparqlIndexerPathSesame)),
            queryInstance);
  }

  @Test
  public void test() {
    sesameIndexer.index();
  }
}
