package utilities;

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.jena.ext.com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.springframework.core.io.Resource;

import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = PRIVATE, makeFinal = true)
public class SparqlIndexerQueryInstance {

    public static class SparqlIndexerQueries {
        @Getter
        private Optional<String> countQuery;

        @Getter
        private String selectQuery;

        @Getter
        private String constructQuery;

        private SparqlIndexerQueries(String countQuery, String selectQuery, String constructQuery) {
            this.countQuery = Optional.ofNullable(countQuery);
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


    public SparqlIndexerQueryInstance(Resource resource, String endpoint) throws IOException {
        Properties props = new Properties();
        props.load(resource.getInputStream());

        int i = 1;
        Optional<String> selectQuery = ofNullable(props.getProperty("selectQuery_" + i));
        do {
            String countQuery = props.getProperty("countQuery_" + i);
            String constructQuery = props.getProperty("constructQuery_" + i);
            this.queries.add(new SparqlIndexerQueries(countQuery, selectQuery.get(), constructQuery));
            i += 1;
        } while ((selectQuery = ofNullable(props.getProperty("selectQuery_" + i))).isPresent());

        this.endpoint = endpoint;
        this.variable = props.getProperty("variable");

    }

}
