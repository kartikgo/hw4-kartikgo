package edu.cmu.lti.f13.hw4.hw4_kartikgo.annotators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.util.FSCollectionFactory;

import edu.cmu.lti.f13.hw4.hw4_kartikgo.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_kartikgo.typesystems.Document;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {
  private StanfordCoreNLP pipeline;

  public void initialize(UimaContext aContext)
      throws ResourceInitializationException {
    super.initialize(aContext);

    Properties props = new Properties();
    props.put("annotators", "tokenize");

    this.pipeline = new StanfordCoreNLP(props);
  }
	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}
	/**
	 * 
	 * @param jcas
	 * @param doc
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {

		String docText = doc.getText();
		
		//TO DO: construct a vector of tokens and update the tokenList in CAS
		edu.stanford.nlp.pipeline.Annotation document = this.pipeline.process(docText);
		

    // int docBegin = doc.getBegin();
    

    List<Token> tokens = new ArrayList<Token>();
    Map<String, Integer> tokenCounts = new HashMap<String, Integer>();
    for (CoreLabel coreToken : document.get(TokensAnnotation.class)) {
      
      String word = coreToken.word().toLowerCase();

      // String word = coreToken.lemma().toLowerCase();
      if (tokenCounts.containsKey(word)) {
        tokenCounts.put(word, tokenCounts.get(word) + 1);
      } else {
        tokenCounts.put(word, 1);
      }
    }

    for (Entry<String, Integer> freq : tokenCounts.entrySet()) {
      Token token = new Token(jcas);
      token.setFrequency(freq.getValue());
      token.setText(freq.getKey());
      token.addToIndexes();
      tokens.add(token);
    }

    doc.setTokenList(FSCollectionFactory.createFSList(jcas, tokens));

	}

}
