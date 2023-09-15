package utilities;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import org.springframework.core.io.Resource;

public class QueryConfig {

  private static final ObjectMapper mapper =
      new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

  public static class SparqlIndexerQueries {

    @Getter private final String selectQuery;

    @Getter private final String constructQuery;

    private SparqlIndexerQueries(String selectQuery, String constructQuery) {
      this.selectQuery = selectQuery;
      this.constructQuery = constructQuery;
    }
  }

  @Getter private final String endpoint;
  @Getter private final String variable;
  @Getter private final List<SparqlIndexerQueries> queries = newArrayList();

  public QueryConfig(Resource resource, String endpoint) throws IOException {

    Map<String, String> props = mapper.readValue(resource.getInputStream(), Map.class);

    int i = 1;
    Optional<String> selectQuery = ofNullable(props.get("selectQuery_" + i));
    do {
      String constructQuery = props.get("constructQuery_" + i);
      this.queries.add(new SparqlIndexerQueries(selectQuery.get(), constructQuery));
      i += 1;
    } while ((selectQuery = ofNullable(props.get("selectQuery_" + i))).isPresent());

    this.endpoint = endpoint;
    this.variable = props.get("variable");
  }
}
