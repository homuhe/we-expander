package com.ir
import java.util.regex.Pattern
import scala.io.Source
import scala.collection.mutable
import java.io.File

/**
  * Created by root on 28.03.17.
  * In the post retrieval approach, first documents that are relevant to the query are extracted and k nearest
  * neighbours are only searched in words that are contained in that documents.
  * The expansion candidates are ranked by similarity to all query words
  */
object post_retrieval extends embeddingSpace{

  /**The inverted index is a HashMap that contains the word as a key and all Document it is found in as a valie
    * The docs2IDS HashMap contains a Mapping from documentnames to document ids starting by 1
    */
  var index = mutable.HashMap[String, Set[Int]]()
  val docs2IDs = mutable.HashMap[String, Int]()
  /**
    * This method reads in a document in the conll format which contains all words in collum 1.
    * It makes all words to lower case.
    * @param file : a path to the location of the corpus file, as a String
    * @return Iterator that contains all the words of a document
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
  /**This method takes a document Iterator and updates the inverted Index by this document.
    * It checks if the index already contains a word and adds it to the index if not, otherwise
    * it just adds the docID to the word.
    * @param file the Iterator that iterates over all words of a given file
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
    * This wrapper method creates an inverted index of a list of files.
    * It creates the mapping of document name and doc ID parallel.
    * @param files a list of File object, all files together form the corpus
    */
  def createInvetedIndex(files: Array[File]): Unit = {

    var doc_id = 0

    for (file <- files) {
      val words = preprocessing(file.toString)
      val doc = file.toString.split("/").last
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
    * @param documents a Set of document IDs
    * @return an Array that contains all words relevant, to the query
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

    /**
      * Helper method
      */
    def help(): Unit = {
      println("Usage: ./post_retrieval arg1 arg2")
      println("\t\targ1: WORD EMBEDDINGS DIRECTORY\t - directory with word embeddings, word end numbers separated by whitespace")
      println("\t\targ2: CORPUS  DIRECTORY\t - directory with corpus file, file must have conll format")
      sys.exit()
    }
    if (args.length < 2) help()
    else {
      embeddings = read_embeddings(args(0))
      println("embeddings have been read\n")
      val files = new File(args(1)).listFiles
      createInvetedIndex(files)
      println("inverted index has been created\n")
      while (true) {
        println("\npost retreival expander:")
        val input = scala.io.StdIn.readLine().toLowerCase().split(" ")
        if (input.length == 1) {
          embeddings.keys.filter(_.startsWith(input(0))).foreach(println(_))
        }
        else {
          val result = postRetrieval(input)
          result.foreach { case (word, s) => print(word, s)
            print("\n")
          }
        }
      }

    }
  }
}
