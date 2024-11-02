package models

import com.fasterxml.jackson.annotation.JsonProperty

case class Result(
                   result: BusinessDetails
                 )

case class BusinessDetails(
                            @JsonProperty("rating") rating: Double,
                            @JsonProperty("user_ratings_total") reviewCount: Int,
                            @JsonProperty("reviews") reviews: List[Review]
                          )

case class Review(
                   @JsonProperty("author_name") authorName: String,
                   @JsonProperty("rating") rating: Int,
                   @JsonProperty("text") text: String,
                   @JsonProperty("relative_time_description") time: String
                 )