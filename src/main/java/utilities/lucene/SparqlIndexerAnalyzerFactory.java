package utilities.lucene;

import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;

import com.google.common.collect.ImmutableMap;

public class SparqlIndexerAnalyzerFactory {

    Map<String, Analyzer> analyzerMap;

    public SparqlIndexerAnalyzerFactory() {
        this.analyzerMap = ImmutableMap.<String, Analyzer>builder()
                .put("rdfs__comment_it", new ItalianAnalyzer())
                .put("rdfs__comment_en", new EnglishAnalyzer())
                .build();
    }

    public Analyzer getSparqlIndexerAnalyzer() {
        return new PerFieldAnalyzerWrapper(new LowercaseAnalyzer(), analyzerMap);
    }



}
