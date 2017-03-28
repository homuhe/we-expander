package com.ir
import java.util.regex.Pattern
import scala.io.Source
import scala.collection.mutable
/**
  * Created by root on 28.03.17.
  */
object post_retrieval {


  var index = mutable.HashMap[String, Set[Int]]()
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
    words filter { case (word, docid) => !spacePattern.matcher(word).find() }
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
  /*
    def postRetrieval(query:String):Array[(String, Float)] = {
      val candidates = getRelevantCandidates(getRelevantDocuments(query))
      println(candidates.length)
      rank(query, candidates)
    }*/

}
