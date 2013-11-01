package edu.cmu.lti.f13.hw4.hw4_kartikgo.casconsumers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;
import org.uimafit.util.FSCollectionFactory;

import edu.cmu.lti.f13.hw4.hw4_kartikgo.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_kartikgo.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_kartikgo.utils.*;

public class RetrievalEvaluator extends CasConsumer_ImplBase {

	/** query id number **/
	public ArrayList<Integer> qIdList;

	/** query and text relevant values **/
	public ArrayList<Integer> relList;

	/** Mapping documents to ids**/
	public HashMap<Integer,Candidate> docmap;
	
	/** Mapping candidates to documents**/
	public HashMap<Integer,ArrayList<Candidate>> candidatemap;
	
	/** Vocabulary **/
	public HashSet<String> vocab;
	
	/** Candidate list **/
	public ArrayList<Candidate> candidates;
	
	private List<Map<String, Integer>> vectors;

  private List<String> raw;
  
	public void initialize() throws ResourceInitializationException {

		qIdList = new ArrayList<Integer>();

		relList = new ArrayList<Integer>();
		
		docmap= new HashMap<Integer,Candidate>();
		
		candidatemap= new HashMap<Integer,ArrayList<Candidate>>();
		
		vocab = new HashSet<String>();

    vectors = new ArrayList<Map<String, Integer>>();

    raw = new ArrayList<String>();
    
    candidates= new ArrayList<Candidate>();
	}

	
	/**
	 * TODO :: 1. construct the global word dictionary 2. keep the word
	 * frequency for each sentence
	 */
	@Override
	public void processCas(CAS aCas) throws ResourceProcessException {

		JCas jcas;
		try {
			jcas =aCas.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}

		FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();
	
		if (it.hasNext()) {
			Document doc = (Document) it.next();

			//Make sure that your previous annotators have populated this in CAS
			FSList fsTokenList = doc.getTokenList();
			//ArrayList<Token>tokenList=Utils.fromFSListToCollection(fsTokenList, Token.class);
			int id = doc.getQueryID();
			int rel = doc.getRelevanceValue();
			String tex = doc.getText();
			//qIdList.add(doc.getQueryID());
			//relList.add(doc.getRelevanceValue());
			
			//Do something useful here
			// Making vocabulary, candidate list and filling up the relevant Hashmaps so that later computations are easy 
			Map<String, Integer> freq = new HashMap<String, Integer>();
      for (Token token : FSCollectionFactory.create(fsTokenList,
          Token.class)) {

        String tokenText = token.getText();

        if (!Pattern.matches("\\p{Punct}", tokenText)) {
          vocab.add(tokenText);
          freq.put(tokenText, token.getFrequency());
        }
      }
      Candidate cd= new Candidate(id,rel,tex,freq);
      if (rel==99){
        docmap.put(id, cd);
      }
      else{
        if (candidatemap.containsKey(id)){
          ArrayList<Candidate> inter = candidatemap.get(id);
          inter.add(cd);
          candidatemap.put(id,inter);
        }
        else{
          ArrayList<Candidate> inter = new ArrayList<Candidate>();
          inter.add(cd);
          candidatemap.put(id,inter);
        }
        
      }
     
      
		}

	}

	/**
	 * TODO 1. Compute Cosine Similarity and rank the retrieved sentences 2.
	 * Compute the MRR metric
	 */
	@Override
	public void collectionProcessComplete(ProcessTrace arg0)
			throws ResourceProcessException, IOException {

		super.collectionProcessComplete(arg0);
		HashMap<Integer, ArrayList<Candidate>> cmap = new HashMap<Integer,ArrayList<Candidate>>();
		// TODO :: compute the cosine similarity measure
		for (Integer id: docmap.keySet()){
		  Map<String, Integer> queryVector = docmap.get(id).getVector();
		  ArrayList<Candidate> cnd= new ArrayList<Candidate>();
      for (Candidate cand:candidatemap.get(id)){
		    
		    Candidate newcand = new Candidate(cand.getQid(),cand.getRelevance(),cand.getSentText(),cand.getVector());
		    Map<String, Integer> docvector = cand.getVector();
		    double score = computeCosineSimilarity(vocab,queryVector,docvector);
		    newcand.setScore(score);
		    cnd.add(newcand);
		  }
      cmap.put(id, cnd);
		}
		
		
		// TODO :: compute the rank of retrieved sentences
		
		HashMap<Integer, ArrayList<Candidate>> cmap2 = SortCandidates(cmap);
		
		// TODO :: compute the metric:: mean reciprocal rank
		double metric_mrr = compute_mrr(cmap2);
		System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
	}

	/**
	 * 
	 * @return cosine_similarity
	 */
	private double computeCosineSimilarity(Set<String> vocab,Map<String, Integer> queryVector,
			Map<String, Integer> docVector) {
		double cosine_similarity=0.0;

		// TODO :: compute cosine similarity between two sentences
		double norm1 = 0.0;
    double norm2 = 0.0;
    for (String word : vocab) {
      double freq1 = 0.0;
      if (queryVector.containsKey(word)) {
        freq1 = queryVector.get(word);
      }
      double freq2 = 0.0;
      if (docVector.containsKey(word)) {
        freq2 = docVector.get(word);
      }

      cosine_similarity += freq1 * freq2;

      norm1 += freq1 * freq1;
      norm2 += freq2 * freq2;
    }

    cosine_similarity = cosine_similarity == 0 ? 0.0 : cosine_similarity
        / (Math.sqrt(norm1) * Math.sqrt(norm2));

    return cosine_similarity;

		
	}
	/**
	 * JaccardSimilarity
	 * @param vocab
	 * @param queryVector
	 * @param docVector
	 * @return
	 */
	public double computeJaccard(Set<String> vocab,
	        Map<String, Integer> queryVector, Map<String, Integer> docVector) {
	      double intersection = 0.0;
	      double union = 0.0;

	      for (String word : vocab) {
	        double freq1 = 0.0;
	        if (queryVector.containsKey(word)) {
	          freq1 = queryVector.get(word);
	        }
	        double freq2 = 0.0;
	        if (docVector.containsKey(word)) {
	          freq2 = docVector.get(word);
	        }

	        intersection += Math.min(freq1, freq2);
	        union += Math.max(freq1, freq2);
	      }

	      return intersection / union;
	    }
	private HashMap<Integer, ArrayList<Candidate>> SortCandidates(HashMap<Integer,ArrayList<Candidate>> canmap) {
	  HashMap<Integer, ArrayList<Candidate>> canmap2 = new HashMap<Integer,ArrayList<Candidate>>();
	  for(Entry<Integer, ArrayList<Candidate>> entry: canmap.entrySet()){
	    ArrayList<Candidate> k = entry.getValue();
      Collections.sort(k, Collections.reverseOrder());
      canmap2.put(entry.getKey(), k);
    }

    return canmap2;
    // TODO :: compute the metric:: mean reciprocal rank
    //double metric_mrr = compute_mrr(canmap2);
    //System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
  }

	
	/**
	 * 
	 * @return mrr
	 */
	private double compute_mrr(HashMap<Integer,ArrayList<Candidate>> all) {
		double metric_mrr=0.0;

		// TODO :: compute Mean Reciprocal Rank (MRR) of the text collection
		for (Entry<Integer, ArrayList<Candidate>> pool : all.entrySet()) {
      for (int i = 0; i < pool.getValue().size(); i++) {
        Candidate cndt = pool.getValue().get(i);
        System.out.println(String.format(
                "Score : %f,\t rank=%d\t,rel=%d,qid=%d,%s",
                cndt.getScore(), i + 1,
                cndt.getRelevance(), cndt.getQid(),
                cndt.getSentText()));
        if (cndt.getRelevance() == 1) {
          //System.out.println("Score: "+(String)cndt.getScore()+"\\t Rank= "+(String)(i+1)+"\\t rel="+(String))
          
          metric_mrr += 1.0 / (i + 1);
          break;
        }
      }
    }

    metric_mrr /= all.size();
		return metric_mrr;
	}

}
