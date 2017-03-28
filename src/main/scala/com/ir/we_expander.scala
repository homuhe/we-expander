package com.ir
import scala.collection.mutable
import scala.io.Source
import java.util.regex.Pattern

/**
  * Created by neele, holger on 26.03.17.
  */
object we_expander {
  //------------------------------------reading and loading the corpus, the embeddings, the inverted index------------------------
  var embeddings = Map[String, Array[Float]]()
  var index = mutable.HashMap[String, Set[Int]]()
  val k = 5

  /**
    * reads in a file in conll format, which contains the word in the first row, the docID in the 6th row
    * all delimiters are removed, all words are lowercased
    *
    * @param file
    * @return Iterator that contains Tuples with the word and the docID
    */
  def preprocessing(file: String): Iterator[(String, String)] = {
    val delimiter = "[ \t\n\r,.?!\\-:;()\\[\\]'\"/*#&$]+"

    var words = Iterator[(String, String)]()

    words = Source.fromFile(file).getLines()
      .filter(!_.isEmpty)
      .map(line => (line.split("\t")(1).toLowerCase(), line.split("\t")(6).toLowerCase()))
    val spacePattern = Pattern.compile(delimiter)
    println("Done with preprocessing!")
    words filter { case (word, docid) => !spacePattern.matcher(word).find() }
  }

  /**
    * reads a file that contains word embeddings
    *
    * @param input should be a file that has the word in the first row and the vector in all the other collums
    *              separated by whitespace
    * @return
    */
  def read_embeddings(input: String): Map[String, Array[Float]] = {
    embeddings = Source.fromFile(input).getLines()
      .map(el => (el.split(" ")(0).toLowerCase(), el.split(" ")
        .tail
        .map(_.toFloat))).toMap
    println("Done with reading in embeddings!")
    embeddings
  }

  /**
    * creates an Inverted Index with the help of a file iterator,
    * index contains each word of the corpus as a key, and all documents as value
    *
    * @param file
    */
  def createInvertedIndex(file: Iterator[(String, String)]): Unit = {
    while(file.hasNext) {
      file.next() match {
        case (word, docId) =>
          val docidasint = Integer.parseInt(docId)
          if (index.contains(word)) {
            val newvalue = index(word) + docidasint
            index.update(word, newvalue)
          }
          else {
            index.put(word, Set(docidasint))
          }
      }
    }
    println("Done with creating inverted indices!")
  }

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

  //-----------------------------------pre-retrieval knn based approach-------------------------------------------
  /**
    * returns the k nearest neighbours of a vector and it's word meanings
    *
    * @param vector
    * @return k nearest neighbours as tuple of word and similarity weight
    */
  def kNN(vector: Array[Float]): Array[(String, Float)] = {
    embeddings.map(pair => (pair._1, cosine_similarity(vector, pair._2))).toArray.sortWith(_._2 > _._2).take(k)
  }

  //if the query is complete use this method to extract candidates---
  /**
    * returns the k best candidates for a query
    *
    * @param input
    * @return
    */
  def getCandidatesBykNN(input: String): Array[String] = {
    var candidates = Array[Array[String]]()
    input.split(" ").foreach { word =>
      val can = kNN(embeddings(word)).map(_._1)
      candidates :+= can.filter(_ != word)
    }
    candidates.flatten
  }

  //if the query is incomplete use this method to extract candidates
  /**
    * returns all words, that start with the incomplete beginning of the last word of a query
    *
    * @param Qt
    * @return
    */
  def getCandidatesForIncompleteQuery(Qt: String): Array[String] = {
    embeddings.keys.filter(_.startsWith(Qt.split(" ").last)).toArray
  }

  /**
    * calculates the similarity between each word of a query and each candidate that was suggested for that query
    *
    * @param query
    * @param candidates
    * @return the best k expansions for that query
    */
  def rank(query: String, candidates: Array[String]): Array[String] = {
    //durch die kandidaten iterieren, für jeden kandidaten errechnen wir similarity zwischen ihm und jedem wort
    //aus dem query.
    // danach summieren wir diese ähnlichkeiten auf. und normalisieren (rechen mal 1/ wortanzahl im query
    // val q = query length,
    var similarities = Array[(String, Float)]()
    val q = query.split(" ").length
    for (candidate <- candidates){
      val canditatevec = embeddings(candidate)
      val sim = query.split(" ").map(word => cosine_similarity(embeddings(word), canditatevec)).sum
      similarities:+= (candidate, (1/q)*sim)
    }

  }

  //---------------------------post retrieval knn approach----------------------------------------
  /** extracts all documents that contain any of the query words
    *
    * @param query
    * @return
    */
  def getRelevantDocuments(query: String): Set[Int] = {
    query.split(" ").flatMap(word => index(word)).toSet
  }

  /**
    * extracts all words, that are contained in the given list of documents
    * all words returned occur together with at least one query word in the document
    *
    * @param documents
    * @return
    */
  def getRelevantCandidates(documents: Set[Int]): Array[String] = {
    val retained = index filter { case (key, value) => documents.intersect(value).nonEmpty }
    retained.keys.toArray
  }

  def postRetrieval(query:String):Array[String] = {
    val candidates = getRelevantCandidates(getRelevantDocuments(query))
    println(candidates.length)
    rank(query, candidates)
  }
  //---------------------------------------------load and run the program-----------------------------
  def main(args: Array[String]) {
    createInvertedIndex(preprocessing(args(0)))
    println("inverted index has been created")
    read_embeddings(args(1))
    println("embeddings have been read")
    while (true) {
      print("query-expander: ")
      val input = scala.io.StdIn.readLine().toLowerCase()
      println("is your query complete?")
      val complete = scala.io.StdIn.readLine()
      if(complete == "complete"){
        println("which ansatz do you want to have? pre retrieval or else")
        val ansatz = scala.io.StdIn.readLine().toLowerCase()
        if(ansatz == "pre retrieval"){
        val candidates = we_expander.getCandidatesBykNN(input)
        val result = we_expander.rank(input, candidates)
        result.foreach{println}}
        else {
          val result = we_expander.postRetrieval(input)
          result.foreach{println}
        }
      }
      else {

        val candidates = we_expander.getCandidatesForIncompleteQuery(input)
        println(candidates.toSet)
        val result = we_expander.rank(input.split(" ").slice(0, input.split(" ").length).mkString(" ").trim, candidates)
        result.foreach{println}
      }
    }

  }
}
