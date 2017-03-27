package com.ir
import scala.collection.mutable
import scala.io.Source

/**
  * Created by neele, holger on 26.03.17.
  */
object we_expander {

  var embeddings = mutable.HashMap[String, Array[Float]]()


  def read_embeddings(input: String): Unit = {
    val data = Source.fromFile(input).getLines()
    for (element <- data) {
      val embedding = element.split(" ")
      val word = embedding(0)
      val vector = embedding.tail.map(_.toFloat)
      embeddings.put(word, vector)
    }
  }


  // calculating the L2-norm for unit length normalization
  def L2Norm(vector: Array[Float]): Float = Math.sqrt(dotproduct(vector, vector)).toFloat


  def dotproduct(vec1: Array[Float], vec2: Array[Float]):Float = {
    vec1.zip(vec2).map(x => x._1 * x._2).sum
  }

  def cosine_similarity(vec1: Array[Float], vec2:Array[Float]):Float = {
    dotproduct(vec1, vec2)/(L2Norm(vec1)*L2Norm(vec2))
  }

  def kNN(vector: Array[Float]): Array[(String, Float)] = {
    embeddings.map(pair => (pair._1, cosine_similarity(vector, pair._2))).toArray.sortWith(_._2 > _._2).take(6)
  }

  def get_candidates(input: String): Array[(String, Float)] = {
    var candidates = Array[(String, Float)]()
    input.split(" ").foreach{word => candidates ++= kNN(embeddings(word))}
    candidates
  }

  def main(args : Array[String]) {
    println("Reading in embeddings...")

    val input = args(0)
    read_embeddings(input)

    println(s"Done with reading in of ${embeddings.size} embeddings!")

    val user_input = "tree city"

    val candidates = get_candidates(user_input)

    println("\nCandidates for <" + user_input + ">:")
    println(candidates.foreach(candidate => println(candidate._1)))
  }

}
