package utilities.lucene;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;

public class LowercaseAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new KeywordTokenizer();
        TokenStream result = new LowerCaseFilter(source);

        return new TokenStreamComponents(source, result);
    }

}
