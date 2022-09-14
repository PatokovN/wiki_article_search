package search_of_article.api.request

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class UpdateRequest(
                          oldTitle: String,
                          optNewTitle: Option[String],
                          optCategoryList: Option[List[String]],
                          optAuxiliaryText: Option[List[String]]
                        )

object UpdateRequest {
  implicit val updateEncoder: Decoder[UpdateRequest] = deriveDecoder[UpdateRequest]
  implicit val updateDecoder: Encoder[UpdateRequest] = deriveEncoder[UpdateRequest]
}
