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
  def L2Norm(vector: Vector[Float]): Float = Math.sqrt(vector.map(x => x*x).sum).toFloat



  def main(args : Array[String]) {
    println( "Hello group member!" )

    val input = args(0)
    embeddings = read_embeddings(input)

    println("done")
  }

}
