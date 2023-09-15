package utilities.lucene;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pt.PortugueseAnalyzer;

public class SparqlIndexerAnalyzerFactory {

    Map<String, Analyzer> analyzerMap;

    public SparqlIndexerAnalyzerFactory() {
        this.analyzerMap = ImmutableMap.<String, Analyzer>builder()
                .put("comment_en", new EnglishAnalyzer())
                .put("comment_it", new ItalianAnalyzer())
                .put("comment_fr", new FrenchAnalyzer())
                .put("comment_de", new GermanAnalyzer())
                .put("comment_es", new SpanishAnalyzer())
                .put("comment_pt", new PortugueseAnalyzer())
                .build();
    }

    public Analyzer getSparqlIndexerAnalyzer() {
        return new PerFieldAnalyzerWrapper(new LowercaseAnalyzer(), analyzerMap);
    }



}
