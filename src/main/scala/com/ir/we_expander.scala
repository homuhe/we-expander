package com.ir
import scala.io.Source

/**
  * Created by neele, holger on 26.03.17.
  */
object we_expander {

  var embeddings = Map[String, Array[Float]]()
  var index = scala.collection.mutable.Map[String, Set[Int]]()

  def preprocessing(file: String, format: String): Array[String] = {
    import java.util.regex.Pattern
    val delimiter = "[ \t\n\r,.?!\\-:;()\\[\\]'\"/*#&$]+"

    var words = Iterator[String]()

    if (format == "conll") {
      words = Source.fromFile(file).getLines()
        .filter(!_.isEmpty)
        .map(_.split("\t")(1))
        .map(_.toLowerCase())
    }
    else {
      words = Source.fromFile(file.toString).getLines()
        .filter(!_.isEmpty)
        .map(_.toLowerCase()).mkString
        .split(" ").toIterator
    }

    val spacePattern = Pattern.compile(delimiter)
    words.filter(!spacePattern.matcher(_).find()).toArray
  }

  def read_embeddings(input: String): Map[String, Array[Float]] = {
    embeddings = Source.fromFile(input).getLines().take(10000)
      .map(el => (el.split(" ")(0).toLowerCase(), el.split(" ")
        .tail
        .map(_.toFloat))).toMap
    embeddings
  }


  // calculating the L2-norm for unit length normalization
  def L2Norm(vector: Array[Float]): Float = Math.sqrt(dotproduct(vector, vector)).toFloat


  def dotproduct(vec1: Array[Float], vec2: Array[Float]):Float = {
    vec1.zip(vec2).map(el => el._1*el._2).sum
  }

  def cosine_similarity(vec1: Array[Float], vec2:Array[Float]):Float = {
    dotproduct(vec1, vec2)/(L2Norm(vec1)*L2Norm(vec2))
  }

  def kNN(vector: Array[Float]): Array[(String, Float)] = {
    embeddings.map(pair => (pair._1, cosine_similarity(vector, pair._2))).toArray.sortBy(_._2).reverse.take(5)
  }

  def get_candidates(input: String): Array[String] = {
    var candidates = Array[Array[String]]()
    input.split(" ").foreach{word =>
      val can = kNN(embeddings(word)).map(_._1)
      candidates:+= can.filter(_!=word)
    }
    candidates.flatten
  }
  def rank(query: String,candidates:Array[String]) : Array[String] = {
    var similarities = Array[Array[(String, Float)]]()
    candidates.foreach{candidate =>
      var wordvec = Array[Float]()
      if (embeddings.contains(candidate)){
      wordvec = embeddings(candidate)}
      else {
        wordvec = Array.fill[Float](300)(0)
      }
      similarities:+=query.split(" ").map(word => (candidate,cosine_similarity(wordvec, embeddings(word))))
    }
    similarities.flatten.sortBy(_._2).reverse.take(3).map(_._1)
  }

  def readFile(input: String): Iterator[(String, String)] = {
    Source.fromFile(input).getLines().take(5000).filter(!_.isEmpty).map(el => (el.split("\t")(1).toLowerCase(), el.split("\t")(6)))
  }

  def createInvertedIndex(file: Iterator[(String, String)]) : Unit= {
    file.foreach{case (word, docId) =>
        if (index.contains(word)){
          val doclist = index(word)
          val newvalue = doclist+Integer.parseInt(docId)
          index.update(word, newvalue)
        }
        else {
          index.put(word, Set(Integer.parseInt(docId)))
        }
    }
  }

  def getRelevantDocuments(query: String):Set[Int] = {
    query.split(" ").map(word => index(word)).flatten.toSet
  }
  def getRelevantCandidates(documents: Set[Int]) :Array[String]= {
      val retained = index filter {case(key, value) => !documents.intersect(value).isEmpty}
      retained.keys.toArray
  }
  def getCandidatesForIncompleteQuery(Qt: String): Array[String] ={
    index.keys.filter(_.startsWith(Qt.split(" ").last)).toArray}

  def main(args : Array[String]) {
    println( "Hello group member!" )

    val corpus = args(0)
    val wordEmbeddings = args(1)
    createInvertedIndex(readFile(corpus))
    println("done")
    val r = read_embeddings(wordEmbeddings)
    println("done with reading in.")
    val candidates_from_relevant = getCandidatesForIncompleteQuery("demokratie a")
    val re = rank("demokratie", candidates_from_relevant)
    println("done")
  }

}
