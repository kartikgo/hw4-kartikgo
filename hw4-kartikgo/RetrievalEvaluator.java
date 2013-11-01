package edu.cmu.lti.f13.hw4.hw4_zhengzhl.casconsumers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f13.hw4.hw4_zhengzhl.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_zhengzhl.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_zhengzhl.utils.Answer;
import edu.cmu.lti.f13.hw4.hw4_zhengzhl.utils.CosineQueryScorer;
import edu.cmu.lti.f13.hw4.hw4_zhengzhl.utils.JaccardScorer;
import edu.cmu.lti.f13.hw4.hw4_zhengzhl.utils.QueryScorer;
import edu.cmu.lti.f13.hw4.hw4_zhengzhl.utils.RelativeFreqScorer;

/**
 * This evaluator aggregate the query and use different methods to calculate the
 * score, then evaluate all the queries using MRR
 * 
 * @author Zhengzhong Liu, Hector
 * 
 */
public class RetrievalEvaluator extends CasConsumer_ImplBase {

	/** query id number **/
	public ArrayList<Integer> qIdList;

	/** query and text relevant values **/
	public ArrayList<Integer> relList;

	private Set<String> globalWords;

	private List<Map<String, Integer>> documents;

	private List<String> rawStrings;

	public void initialize() throws ResourceInitializationException {

		qIdList = new ArrayList<Integer>();

		relList = new ArrayList<Integer>();

		globalWords = new HashSet<String>();

		documents = new ArrayList<Map<String, Integer>>();

		rawStrings = new ArrayList<String>();
	}

	@Override
	public void processCas(CAS aCas) throws ResourceProcessException {

		JCas jcas;
		try {
			jcas = aCas.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}

		FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();

		if (it.hasNext()) {
			Document doc = (Document) it.next();

			// Make sure that your previous annotators have populated this in
			// CAS
			FSList fsTokenList = doc.getTokenList();
			// ArrayList<Token>tokenList=Utils.fromFSListToCollection(fsTokenList,
			// Token.class);

			qIdList.add(doc.getQueryID());
			relList.add(doc.getRelevanceValue());

			// Do something useful here

			Map<String, Integer> tokenWithFreq = new HashMap<String, Integer>();
			for (Token token : FSCollectionFactory.create(fsTokenList,
					Token.class)) {

				String tokenText = token.getText();

				if (!Pattern.matches("\\p{Punct}", tokenText)) {
					globalWords.add(tokenText);
					tokenWithFreq.put(tokenText, token.getFrequency());
				}
			}
			documents.add(tokenWithFreq);

			rawStrings.add(doc.getText());
		}

	}

	@Override
	public void collectionProcessComplete(ProcessTrace arg0)
			throws ResourceProcessException, IOException {

		super.collectionProcessComplete(arg0);

		List<QueryScorer> scorers = new ArrayList<QueryScorer>();

		scorers.add(new CosineQueryScorer());
		scorers.add(new JaccardScorer());
		scorers.add(new RelativeFreqScorer());

		for (QueryScorer scorer : scorers) {
			System.out.println(String.format("====Result of %s ====",
					scorer.name()));

			evaluate(scoreAnswers(scorer));

			// sortBasedEvaluate(scoreAnswers(scorer));
		}

	}

	/**
	 * Use the scorer to score the answers
	 * 
	 * @param scorer
	 *            This is an implementation of a scorer
	 * @return A list of answers for all the questions, scored by the provided
	 *         scorer
	 */
	private List<List<Answer>> scoreAnswers(QueryScorer scorer) {
		List<List<Answer>> scoredAnswers = new ArrayList<List<Answer>>();

		Map<String, Integer> currentQuery = null;
		// TODO :: compute the cosine similarity measure
		int sentid = 1;
		for (int i = 0; i < qIdList.size(); i++) {
			int qid = qIdList.get(i);
			int rel = relList.get(i);
			Map<String, Integer> document = documents.get(i);
			String text = rawStrings.get(i);
			if (rel == 99) {
				currentQuery = document;
				scoredAnswers.add(new ArrayList<Answer>());
				sentid = 1;
			} else {
				double score = scorer.computeScore(globalWords, currentQuery,
						document);
				Answer ans = new Answer(qid, sentid, rel, text);
				ans.setScore(score);
				scoredAnswers.get(scoredAnswers.size() - 1).add(ans);
				sentid++;
			}
		}

		return scoredAnswers;
	}

	/**
	 * Evaluate the answers by sorting
	 * 
	 * @param allScoredAnswer
	 */
	private void sortBasedEvaluate(List<List<Answer>> allScoredAnswer) {
		for (List<Answer> scoredAnswer : allScoredAnswer) {
			Collections.sort(scoredAnswer, Collections.reverseOrder());
		}

		for (List<Answer> scoreAnswers : allScoredAnswer) {
			for (int j = 0; j < scoreAnswers.size(); j++) {
				Answer scoredAnswer = scoreAnswers.get(j);
				// if (scoredAnswer.getRelevance() == 1) {
				if (true) {

					System.out.println(String.format(
							"Score : %f,\t rank=%d\t,rel=%d,qid=%d,%s",
							scoredAnswer.getScore(), j + 1,
							scoredAnswer.getRelevance(), scoredAnswer.getQid(),
							scoredAnswer.getSentText()));
				}
			}
		}
		// TODO :: compute the metric:: mean reciprocal rank
		double metric_mrr = compute_mrr(allScoredAnswer);
		System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
	}

	/**
	 * Evaluate the answers by finding where the correct answer rank
	 * 
	 * @param allScoredAnswer
	 */
	private void evaluate(List<List<Answer>> allScoredAnswer) {
		// TODO :: compute the rank of retrieved sentences
		double metric_mrr = 0.0;

		for (List<Answer> scoreAnswers : allScoredAnswer) {
			double correctAnswerScore = 0.0;
			int correctAnswerIndex = 0;

			for (int i = 0; i < scoreAnswers.size(); i++) {
				Answer answer = scoreAnswers.get(i);
				if (answer.getRelevance() == 1) {
					correctAnswerScore = answer.getScore();
					correctAnswerIndex = i;
				}
			}

			int rank = 1;
			for (Answer answer : scoreAnswers) {
				if (answer.getScore() > correctAnswerScore) {
					rank += 1;
				}
			}

			metric_mrr += 1.0 / rank;

			Answer correctAnswer = scoreAnswers.get(correctAnswerIndex);
			System.out.println(String.format(
					"Score : %f,\t rank=%d\t,rel=%d,qid=%d,%s",
					correctAnswer.getScore(), rank,
					correctAnswer.getRelevance(), correctAnswer.getQid(),
					correctAnswer.getSentText()));
		}

		metric_mrr /= allScoredAnswer.size();
		System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
	}

	/**
	 * Compute the MRR from the scored answers for all questions
	 * 
	 * @return mrr The computed MRR (Mean Reporical Rank)
	 */
	private double compute_mrr(List<List<Answer>> allScoredAnswers) {
		double metric_mrr = 0.0;

		// TODO :: compute Mean Reciprocal Rank (MRR) of the text collection
		for (List<Answer> scoredAnswers : allScoredAnswers) {
			for (int i = 0; i < scoredAnswers.size(); i++) {
				Answer scoreAnswer = scoredAnswers.get(i);
				if (scoreAnswer.getRelevance() == 1) {
					metric_mrr += 1.0 / (i + 1);
					break;
				}
			}
		}

		metric_mrr /= allScoredAnswers.size();
		return metric_mrr;
	}

}
