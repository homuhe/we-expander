package com.ir
import scala.io.Source

/** Author:       Holger Muth-Hellebrandt,
  *               Neele Witte
  *
  * Task:         IR Project WS16/17
  * Description:  Query expander as (two) semantic ranking approach(es)
  *               pre-retrieval - cosine sim, knn
  *               post-retrieval - relevant doc filtering, cosine sim, knn
  */

/**
  * Vector calculation functions, needed in pre- and post-retrieval
  */
trait vectorFunctions {
  /**
    * calculates the L2 norm of a vector
    * @param vector Array of floating point numbers
    * @return norm
    */
  def L2Norm(vector: Array[Float]): Float = Math.sqrt(dotproduct(vector, vector)).toFloat

  /**
    * calculates the dot product of two vectors, vec1 & vec2
    * @param vec1 floating point vector 1
    * @param vec2 floating point vector 2
    * @return dot product of vec1 & vec2
    */
  def dotproduct(vec1: Array[Float], vec2: Array[Float]): Float = {
    vec1.zip(vec2).map(el => el._1 * el._2).sum
  }

  /**
    * calculates the similarity of two vectors by cosine similarity
    * @param vec1 floating point vector 1
    * @param vec2 floating point vector 2
    * @return cosine similarity of vec1 & vec2
    */
  def cosine_similarity(vec1: Array[Float], vec2: Array[Float]): Float = {
    dotproduct(vec1, vec2) / (L2Norm(vec1) * L2Norm(vec2))
  }
}

/**
  * Class VectorSpace including read-in function of embeddings, accessing knn candidates and rank function
  */
class VectorSpace extends vectorFunctions {

  //contains a word as a key and it's word vector as value
  var embeddings = Map[String, Array[Float]]()

  //k of kNN
  val k = 10

  /**
    * reads in file with word embeddings
    * @param input file containing a word in 1st row & floating point vector in all other collums,
    *                                separated by whitespace.
    * @return mapping of  word to vector
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

  /**
    * returns k most similar candidates aka k nearest neighbours for a query.
    * @param input query as an Array of String
    * @return Array containing query expansion candidates and their similarity weight
    */
  def getCandidatesBykNN(input: Array[String], wordembeddings:Map[String, Array[Float]]): Array[(String, Float)] = {
    val query_as_vector = input.map(word => (word, embeddings(word)))
    val similarities = Array.ofDim[Array[(String, Float)]](wordembeddings.size)
    var i = 0

    for ((embedding, vec) <- wordembeddings) {
      similarities(i) = query_as_vector.map({case
                      (queryword, queryvec) => (embedding, cosine_similarity(vec, queryvec))})
      i+=1
    }
    similarities.flatten.filter(_._2 > 0).sortWith(_._2 > _._2).take(k)
    }

  /**
    * ranks knn candidates according to cosine simitarity with other query tokens.
    * Similarities for each candidate are summed up and the candidates with the highest
    * similarity weights are returned.
    * @param query Array of String that contains user query tokens
    * @param candidates Array of Strings that contains all possible candidates
    * @return best k expansions for that query with their weight
    */
  def rank(query: Array[String], candidates: Array[String]): Array[(String, Float)] = {
    var similarities = Array[(String, Float)]()
    val q = query.length.toFloat
    for (candidate <- candidates){
      val canditatevec = embeddings(candidate)
      val sim = query.map(word => cosine_similarity(embeddings(word), canditatevec)).sum
      similarities :+= (candidate, (1/q) * sim)
    }
    similarities.sortWith(_._2>_._2)
  }
}

object pre_retrieval extends VectorSpace {

  /**
    * returns all candidates that start with the incomplete beginning of the last word of a query
    *
    * @param Qt an incomplete word
    * @return all candidates starting with the incomplete user input
    */
  def getCandidatesForIncompleteQuery(Qt: String): Array[String] = {
    embeddings.keys.filter(_.startsWith(Qt.split(" ").last)).toArray
  }

  /**
    * If the query doesn't contain any incomplete words, the k nearest neighbours of all
    * query words are extracted
    *
    * @param query the query, as an Array of Strings
    * @return the candidates which are the k nearest neighbours of all query words
    */
  def getCandidatesForCompleteQuery(query: Array[String]): Array[String] = {
    getCandidatesBykNN(query, embeddings).map(_._1)
  }

  /**
    * For a given query which my contain only completed words or which may have an incomplete word
    * as the last word this method extracts possible query expansion candidates and ranks them by
    * semantic similarity to the query
    *
    * @param input user input as Array of strings
    * @return an Array that contains the most similar words and their similarities to the query
    */
  def pre_retrieval(input: Array[String]): Array[(String, Float)] = {
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


  def main(args: Array[String]) {

    if (args.length < 1) help()
    else {

      embeddings = read_embeddings(args(0))
      print("embeddings have been read\n")

      while (true) {
        print("\npre retrieval expander: ")
        val input = scala.io.StdIn.readLine().toLowerCase()
        val query_words = input.split(" ")
        if (query_words.length == 1 && !embeddings.contains(query_words.last)) {
          getCandidatesForIncompleteQuery(query_words.last).foreach(println)
        }
        else {
          val result = pre_retrieval(query_words)
          result.foreach(rank => println(rank._1 + ", " + rank._2))
        }
      }
    }

    /**
      * Helper method
      */
    def help(): Unit = {
      println("Usage: ./pre-retrieval arg1")
      println("\t\targ1: WORD EMBEDDINGS DIRECTORY\t - directory with word embeddings, separated by whitespace")
      sys.exit()
    }
  }
}