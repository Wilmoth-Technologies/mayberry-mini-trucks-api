package models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

case class Review(
                   name: String,
                   date: String,
                   profilePic: String,
                   rating: Int,
                   text: String)

case class ReviewCount(
                        count: Int)
