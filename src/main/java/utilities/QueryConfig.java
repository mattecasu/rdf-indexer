package utilities;

import lombok.Getter;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static java.util.Optional.ofNullable;
import static org.apache.jena.ext.com.google.common.collect.Lists.newArrayList;

public class QueryConfig {

    public static class SparqlIndexerQueries {

        @Getter
        private final String selectQuery;

        @Getter
        private final String constructQuery;

        private SparqlIndexerQueries(String selectQuery, String constructQuery) {
            this.selectQuery = selectQuery;
            this.constructQuery = constructQuery;
        }
    }

    @Getter
    private final String endpoint;
    @Getter
    private final String variable;
    @Getter
    private final List<SparqlIndexerQueries> queries = newArrayList();


    public QueryConfig(Resource resource, String endpoint) throws IOException {
        Properties props = new Properties();
        props.load(resource.getInputStream());

        int i = 1;
        Optional<String> selectQuery = ofNullable(props.getProperty("selectQuery_" + i));
        do {
            String constructQuery = props.getProperty("constructQuery_" + i);
            this.queries.add(new SparqlIndexerQueries(selectQuery.get(), constructQuery));
            i += 1;
        } while ((selectQuery = ofNullable(props.getProperty("selectQuery_" + i))).isPresent());

        this.endpoint = endpoint;
        this.variable = props.getProperty("variable");

    }

}
