package com.ir
import scala.io.Source

/**
  * Created by neele, holger on 26.03.17.
  */
object we_expander {
  //------------------------------------reading and loading the corpus, the embeddings, the inverted index------------------------
  var embeddings = Map[String, Array[Float]]()
  var index = scala.collection.mutable.Map[String, Set[Int]]()

  /**
    * reads in a file in conll format, which contains the word in the first row, the docID in the 6th row
    * all delimiters are removed, all words are lowercased
    *
    * @param file
    * @param format should be conll format
    * @return Iterator that contains Tuples with the word and the docID
    */
  def preprocessing(file: String, format: String): Iterator[(String, String)] = {
    import java.util.regex.Pattern
    val delimiter = "[ \t\n\r,.?!\\-:;()\\[\\]'\"/*#&$]+"

    var words = Iterator[(String, String)]()

    if (format == "conll") {
      words = Source.fromFile(file).getLines()
        .filter(!_.isEmpty)
        .map(line => (line.split("\t")(1).toLowerCase(), line.split("\t")(6).toLowerCase()))
    }
    val spacePattern = Pattern.compile(delimiter)
    words filter { case (word, docid) => !spacePattern.matcher(word).find() }
    print("done with preprocessing")
    words
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
    embeddings.map(pair => (pair._1, cosine_similarity(vector, pair._2))).toArray.sortBy(_._2).reverse.take(5)
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
    index.keys.filter(_.startsWith(Qt.split(" ").last)).toArray
  }

  /**
    * calculates the similarity between each word of a query and each candidate that was suggested for that query
    *
    * @param query
    * @param candidates
    * @return the best k expansions for that query
    */
  def rank(query: String, candidates: Array[String]): Array[String] = {
    var similarities = Array[Array[(String, Float)]]()
    candidates.foreach { candidate =>
      var wordvec = Array[Float]()
      if (embeddings.contains(candidate)) {
        wordvec = embeddings(candidate)
      }
      else {
        wordvec = Array.fill[Float](300)(0)
      }
      similarities :+= query.split(" ").map(word => (candidate, cosine_similarity(wordvec, embeddings(word))))
    }
    similarities.flatten.sortBy(_._2).reverse.take(3).map(_._1)
  }

  //---------------------------post retrieval knn approach----------------------------------------
  /** extracts all documents that contain any of the query words
    *
    * @param query
    * @return
    */
  def getRelevantDocuments(query: String): Set[Int] = {
    query.split(" ").map(word => index(word)).flatten.toSet
  }

  /**
    * extracts all words, that are contained in the given list of documents
    * all words returned occur together with at least one query word in the document
    *
    * @param documents
    * @return
    */
  def getRelevantCandidates(documents: Set[Int]): Array[String] = {
    val retained = index filter { case (key, value) => !documents.intersect(value).isEmpty }
    retained.keys.toArray
  }


  //---------------------------------------------load and run the program-----------------------------
  def main(args: Array[String]) {
    def loadData = {
      createInvertedIndex(preprocessing(args(0), "conll"))
      println("inverted index has been created")
      read_embeddings(args(1))
      println("embeddings have been read")
    }
    loadData
    while (true) {
      print("query-expander: ")

      val input = scala.io.StdIn.readLine()
      println("is your query complete?")
      val complete = scala.io.StdIn.readLine()
      if(complete == "complete"){
        val candidates = we_expander.getCandidatesBykNN(input)
        val result = we_expander.rank(input, candidates)
        result.foreach{println}
      }
      else {
        val candidates = we_expander.getCandidatesForIncompleteQuery(input)
        val result = we_expander.rank(input.replace(input.split(" ").last, ""), candidates)
        result.foreach{println}
      }
    }

  }
}
