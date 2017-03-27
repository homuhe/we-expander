package com.ir
import scala.io.Source

/**
  * Created by neele, holger on 26.03.17.
  */
object we_expander {

  var embeddings = Map[String, Array[Float]]()


  def read_embeddings(input: String): Map[String, Array[Float]] = {
    Source.fromFile(input).getLines()
      .map(el => (el.split(" ")(0), el.split(" ")
        .tail
        .map(_.toFloat))).toMap
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
      candidates:+= can
    }
    candidates.flatten
  }
  def rank(query: String) : Array[String] = {
    val candidates = get_candidates(query)
    var similarities = Array[Array[(String, Float)]]()
    candidates.foreach{candidate =>
      val wordvec = embeddings(candidate)
      similarities:+=query.split(" ").map(word => (word,cosine_similarity(wordvec, embeddings(word))))
    }
    similarities.flatten.sortBy(_._2).reverse.take(3).map(_._1)
  }
  def main(args : Array[String]) {
    println( "Hello group member!" )

    val input = args(0)
    embeddings = read_embeddings(input)
    println("done with reading in.")
    val x = rank("tree city")
    println("done")
  }

}
