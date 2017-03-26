package com.ir

import scala.collection.mutable
import scala.io.Source

/**
  * Created by neele, holger on 26.03.17.
  */
object we_expander {

  def main(args : Array[String]) {
    println( "Hello group member!" )



    val embeddings = mutable.HashMap[String, Array[Float]]()

    val  lines = Source.fromFile(args(0)).getLines()

    for (line <- lines) {
      val elements = line.split(" ")
      val word = elements(0)
      val embedding = elements.tail.map(_.toFloat)

      embeddings.put(word, embedding)
    }
  }

}
