package utilities;

import java.util.List;
import java.util.Optional;

import org.apache.lucene.document.Document;

public interface QueryExecutor {

    Integer makeCount(String countQuery);

    List<String> makeSelect(String limitedSelect);

    Optional<Document> makeConstruct(String uri, String constructQuery);

}
