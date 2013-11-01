package edu.cmu.lti.f13.hw4.hw4_kartikgo.utils;

import java.util.List;
import java.util.Map;

public class Candidate implements Comparable<Candidate> {
  private double score;

  private int relevance;

  private int qid;

  private String sentText;

  private Map<String, Integer> vector;
  public Candidate(int qid, int relevance, String sentText, Map<String, Integer> vector) {
    this.qid = qid;
    this.vector = vector;
    this.relevance = relevance;
    this.sentText = sentText;
  }

  @Override
  public int compareTo(Candidate o) {
    // TODO Auto-generated method stub
    if (o == null) {
      return 1;
    } else {
      if (this.score == o.score) {
        return 0;
      } else if (this.score > o.score) {
        return 1;
      } else {
        return -1;
      }
    }
    
  }

  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }

  public int getRelevance() {
    return relevance;
  }

  public void setRelevance(int relevance) {
    this.relevance = relevance;
  }

  public int getQid() {
    return qid;
  }

  public void setQid(int qid) {
    this.qid = qid;
  }

  public Map<String, Integer> getVector() {
    return vector;
  }

  public void setVector(Map<String, Integer> vector) {
    this.vector = vector;
  }

  public String getSentText() {
    return sentText;
  }

  public void setSentText(String sentText) {
    this.sentText = sentText;
  }

}
