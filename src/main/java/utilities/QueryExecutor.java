package utilities;

import java.util.List;
import java.util.Optional;

import org.apache.lucene.document.Document;

public interface QueryExecutor {

    public Integer makeCount(String countQuery);

    public List<String> makeSelect(String limitedSelect);

    public Optional<Document> makeConstruct(String uri, String constructQuery);

}
