package com.ir
import java.util.regex.Pattern
import scala.io.Source
import scala.collection.mutable
import java.io.File

/**
  * Post-Retrieval Semantic Approach:
  * First, pre-selecting documents that are relevant to query are extracted,
  * then k nearest neighbours that are contained in that documents are searched.
  * The expansion candidates are ranked by similarity to all query words.
  */
object post_retrieval extends VectorSpace {

  //Inverted index aka HashMap that contains the word as a key and posting list as value.
  var index = mutable.HashMap[String, Set[Int]]()

  /**
    * Reads in a document in the conll format which contains all words in collum 1.
    * Normalizes to lower-case.
    * @param file : location of the corpus file as a String
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

  /** Takes a document Iterator and updates the inverted index by this document.
    * @param file Iterator which iterates over all words of a given file
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
    * Wrapper method which creates an inverted index of a list of files.
    * @param files a list of File objects, all files together form the corpus
    */
  def createInvertedIndex(files: Array[File]): Unit = {

    var doc_id = 0

    for (file <- files) {
      val words = preprocessing(file.toString)
      updateInvertedIndex(words, doc_id)
      doc_id += 1
    }
  }

  /**
    * Extracts all words that are contained in the set of docIDs aka posting list of all query tokens.
    * All words returned occur together with at least one query word in the document
    *
    * @param query user query as Array of Strings
    * @return an Array that contains all words relevant, to the query
    */
  def getRelevantCandidates(query: Array[String]): Array[String] = {
    val documents = query.flatMap(word => index.getOrElse(word, Nil)).toSet
    index.filter({ case (key, value) => documents.intersect(value).nonEmpty }).keys.toArray
  }

  /**
    * For a given query, this method extracts all relevant documents from the corpus
    * and returns query expansion candidates
    * @param input a query (as an Array of Strings)
    * @return expansion candidates
    */
    def postRetrieval(input: Array[String]): Array[String] = {
      var candidates = Array[String]()

      if (embeddings.contains(input.last)) {
        candidates = getRelevantCandidates(input).filter(embeddings.contains(_))
        val new_embeddings = embeddings.filter({case (word, vec) => candidates.contains(word)})
        super.getCandidatesBykNN(input, new_embeddings).map(_._1)
      }
      else getRelevantCandidates(input).filter(embeddings.contains(_))
                                                    .filter(_.startsWith(input.last))
    }


  def main(args: Array[String]): Unit = {

    if (args.length < 2) help()
    else {

      embeddings = read_embeddings(args(0))
      println("embeddings have been read!")

      val files = new File(args(1)).listFiles
      createInvertedIndex(files)
      println("inverted index has been created!")

      while (true) {
        print("\npost-retrieval expander: ")
        val input = scala.io.StdIn.readLine().toLowerCase().split(" ")
        var ranks = Array[(String, Float)]()

        if (input.length == 1) ranks = super.rank(input, postRetrieval(input))
        else ranks = super.rank(input.init, postRetrieval(input))

        ranks.foreach(rank => println(rank._1 + ", " + rank._2))
      }
    }

    /**
      * Helper method
      */
    def help(): Unit = {
      println("Usage: ./post-retrieval arg1 arg2")
      println("\t\targ1: WORD EMBEDDINGS DIRECTORY\t  - directory with word embeddings, separated by whitespace")
      println("\t\targ2: CORPUS DIRECTORY\t           - directory with corpus file, file must have conll format")
      sys.exit()
    }
  }
}
