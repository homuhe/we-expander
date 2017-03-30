package com.ir
import java.util.regex.Pattern
import scala.io.Source
import scala.collection.mutable
/**
  * Created by root on 28.03.17.
  */
object post_retrieval extends embeddingSpace{

  import java.io.File

  var index = mutable.HashMap[String, Set[Int]]()
  val docs2IDs = mutable.HashMap[String, Int]()
  /**
    * reads in a file in conll format, which contains the word in the first row, the docID in the 6th row
    * all delimiters are removed, all words are lowercased
    *
    * @param file
    * @return Iterator that contains Tuples with the word and the docID
    */
  def preprocessing(file: String): Iterator[String] = {
    val delimiter = "[ \t\n\r,.?!\\-:;()\\[\\]'\"/*#&$]+"

    var words = Iterator[String]()

    words = Source.fromFile(file).getLines()
      .filter(!_.isEmpty)
      .map(_.split("\t")(1).toLowerCase())
    val spacePattern = Pattern.compile(delimiter)
    words.filter(el => !spacePattern.matcher(el).find())
  }
  /**
    * updates an Inverted Index with the help of a file iterator,
    * index contains each word of the corpus as a key, and all documents as value
    * @param file
    */
  def updateInvertedIndex(file: Iterator[String], docid: Int): Unit = {
    while(file.hasNext) {
      val word = file.next()
          if (index.contains(word)) {
            val newvalue = index(word) + docid
            index.update(word, newvalue)
          }
          else {
            index.put(word, Set(docid))
          }
      }
    }

  /**
    * for a set of files in the conll format this method creates an inverted index with each word
    * as a key and all the documents that contain the word as a value
    * @param files
    */
  def createInvetedIndex(files: Array[File]): Unit = {

    var doc_id = 0

    for (file <- files) {
      val words = preprocessing(file.toString)
      val doc = file.toString.split("/").last//.replace(".conll", "").toInt

      //println("Reading doc " + doc + ", new docID: " + doc_id)//(files.indexOf(file)+1))
      updateInvertedIndex(words, doc_id)
      docs2IDs.put(doc, doc_id)

      doc_id += 1
    }
  }
  //---------------------------post retrieval knn approach----------------------------------------
  /** extracts all documents that contain any of the query words
    *
    * @param query
    * @return
    */
  def getRelevantDocuments(query: Array[String]): Set[Int] = {
    query.flatMap(word => index.getOrElse(word, Nil)).toSet
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

  /**
    * For a given query, this method extracts all relevant documents from the corpus and returns
    * the k best (in terms of similarity) query expansion candidates
    * @param input a query (as an Array of Strings)
    * @return the k best expansion candidates and with weight
    */
    def postRetrieval(input:Array[String]):Array[(String, Float)] = {
      var candidates = Array[String]()
      var newquery = input
      if (embeddings.contains(input.last)){
        println("your query was complete")
        candidates = getRelevantCandidates(getRelevantDocuments(input)).filter(embeddings.contains(_))
        val newembeddings = embeddings.filter({case (word, vec) => candidates.contains(word)})
        candidates = super.getCandidatesBykNN(input, newembeddings).map(_._1)
        }
      else {
        println("your query was incomplete")
        candidates = getRelevantCandidates(getRelevantDocuments(input.init)).filter(embeddings.contains(_))
        candidates = candidates.filter(_.startsWith(input.last))
        newquery = input.init
      }
      super.rank(newquery, candidates)
    }
//------------------use to run the program--------------------------------------------------------------------
  def main(args: Array[String]): Unit = {
    embeddings = read_embeddings(args(0))
    println("embeddings have been read")
    val files = new File(args(1)).listFiles
    createInvetedIndex(files)
    println("inverted index has been created")
    while(true){
      println("please write query")
      val input = scala.io.StdIn.readLine().toLowerCase().split(" ")
      if (input.length == 1) {
        embeddings.keys.filter(_.startsWith(input(0))).foreach(println(_))
      }
      else {
        val result = postRetrieval(input)
        result.foreach{case (word, s) => print(word, s)
        print("\n")}
      }
    }
  }

}
