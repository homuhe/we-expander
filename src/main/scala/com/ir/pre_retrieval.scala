package com.ir
import scala.io.Source

/**
  * Created by neele, holger on 26.03.17.
  */

trait vectorFunctions{

  //-------------------------------vector calculation methods-------------------------------------------------
  /**
    * calculates the L2 norm of a vector
    *
    * @param vector
    * @return the norm as a floating number
    */
  def L2Norm(vector: Array[Float]): Float = Math.sqrt(dotproduct(vector, vector)).toFloat

  /**
    * calculates the dotproduct of 2 vectors
    *
    * @param vec1
    * @param vec2
    * @return the dotproduct as a floating number
    */
  def dotproduct(vec1: Array[Float], vec2: Array[Float]): Float = {
    vec1.zip(vec2).map(el => el._1 * el._2).sum
  }

  /**
    * calculates the similarity of two vectors by using cosine similiarity
    *
    * @param vec1
    * @param vec2
    * @return
    */
  def cosine_similarity(vec1: Array[Float], vec2: Array[Float]): Float = {
    dotproduct(vec1, vec2) / (L2Norm(vec1) * L2Norm(vec2))
  }
}

class embeddingSpace extends vectorFunctions{
  //------------------------------------reading and loading the corpus, the embeddings, the inverted index------------------------
  var embeddings = Map[String, Array[Float]]()
  val k = 10

  /**
    * reads a file that contains word embeddings
    *
    * @param input should be a file that has the word in the first row and the vector in all the other collums
    *              separated by whitespace
    * @return
    */
  def read_embeddings(input: String):Map[String, Array[Float]]= {
    val words = Source.fromFile(input).getLines()
      words
      .filter(_.split(" ")(0).length > 1)
      .map(el =>
        (el.split(" ")(0).toLowerCase(), el.split(" ")
        .tail
        .map(_.toFloat))).toMap
  }



  //-----------------------------------knn methods-------------------------------------------


  //if the query is complete use this method to extract candidates---
  /**
    * returns the k best candidates for a query
    *
    * @param input
    * @return
    */
  def getCandidatesBykNN(input: Array[String], wordembeddings:Map[String, Array[Float]]): Array[(String, Float)] = {
    val query_as_vector = input.map(word => (word, embeddings(word)))
    val similarites = Array.ofDim[Array[(String, Float)]](wordembeddings.size)
    var i = 0

    for ((embedding, vec) <- wordembeddings) {
      similarites(i) = query_as_vector.map({case
                      (queryword, queryvec) => (embedding, cosine_similarity(vec, queryvec))})
      i+=1
    }
    //similarities.sortWith(_._2>_._2).take(k)
    similarites.flatten.filter(_._2<0.86).sortWith(_._2 > _._2).take(k)
    }

  /**
    * calculates the similarity between each word of a query and each candidate that was suggested for that query
    *
    * @param query
    * @param candidates
    * @return the best k expansions for that query
    */
  def rank(query: Array[String], candidates: Array[String]): Array[(String, Float)] = {
    //durch die kandidaten iterieren, für jeden kandidaten errechnen wir similarity zwischen ihm und jedem wort
    //aus dem query.
    // danach summieren wir diese ähnlichkeiten auf. und normalisieren (rechen mal 1/ wortanzahl im query
    // val q = query length,
    var similarities = Array[(String, Float)]()
    val q = query.length.toFloat
    for (candidate <- candidates){
      val canditatevec = embeddings(candidate)
      val sim = query.map(word => cosine_similarity(embeddings(word), canditatevec)).sum
      similarities:+= (candidate, (1/q)*sim)
    }
    similarities.sortWith(_._2>_._2).take(k)
  }


}
object pre_retrieval extends embeddingSpace {

  /**
    * returns all words, that start with the incomplete beginning of the last word of a query
    * @param Qt
    * @return
    */
  def getCandidatesForIncompleteQuery(Qt: String): Array[String] = {
    embeddings.keys.filter(_.startsWith(Qt.split(" ").last)).toArray
  }
  def getCandidatesForCompleteQuery(query:Array[String]):Array[String] = {
    getCandidatesBykNN(query, embeddings).map(_._1)
  }
  def pre_retrieval(input: Array[String]): Array[(String, Float)] ={
    var candidates = Array[String]()
    var query_words = input
    //query = complete
    if (embeddings.contains(input.last)) {
      candidates = getCandidatesForCompleteQuery(input)
    }
    //query = incomplete
    else {
      candidates = getCandidatesForIncompleteQuery(input.last)
      query_words = query_words.init
    }
    rank(query_words, candidates)
  }

  //---------------------------------------------load and run the program-----------------------------
  def main(args: Array[String]) {
    embeddings = read_embeddings(args(0))
    print("embeddings have been read\n")
    while (true) {
      println("please write query")
      val input = scala.io.StdIn.readLine().toLowerCase()
      val query_words = input.split(" ")
      val result = pre_retrieval(query_words)
      result.foreach{case (word, value) =>
      print(word, value)
          print("\n")
      }
    }
  }
}