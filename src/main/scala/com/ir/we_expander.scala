package com.ir
import scala.io.Source

import scala.collection.mutable
import scala.io.Source

/**
  * Created by neele, holger on 26.03.17.
  */
object we_expander {

  def main(args : Array[String]) {
    println( "Hello group member!" )

    val wordEmbeddings = args(0)
    val map = Source.fromFile(wordEmbeddings).getLines()
                                              .map(el => (el.split(" ")(0), el.split(" ")
                                              .tail
                                              .map(_.toFloat))).toMap
    println("done")
  }

}
