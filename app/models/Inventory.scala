package models

case class InventoryTemp(
                          temp: String
                        )

case class Review(
                 reviewProfileName: String,
                 reviewProfilePic: String,
                 reviewDate: String,
                 rating: Double,
                 comment: String
                 )

case class CompanyOverallReview(
                 companyRating: Double,
                 reviewCount: Int,
                 reviews: List[Review]
                 )