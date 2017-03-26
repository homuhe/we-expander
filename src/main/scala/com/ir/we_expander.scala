package com.ir

/**
  * Created by neele, holger on 26.03.17.
  */
object we_expander {

  def main(args : Array[String]) {
    import scala.io.Source
    println( "Hello group member!" )
    val wordEmbeddings = args(0)
    val map = Source.fromFile(wordEmbeddings).getLines()
                                              .map(el => (el.split(" ")(0), el.split(" ")
                                              .tail
                                              .map(_.toFloat))).toMap
    println("done")
  }

}
