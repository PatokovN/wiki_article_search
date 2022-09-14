package search_of_article.api.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import search_of_article.model.FullArticle
import io.scalaland.chimney.dsl._

case class ArticleView(
                        title: String,
                        createTime: Long,
                        timestamp: Long,
                        language: String,
                        wiki: String,
                        categories: List[String],
                        auxiliaryText: Option[List[String]])

object ArticleView {
  implicit val articleEncoder: Decoder[ArticleView] = deriveDecoder[ArticleView]
  implicit val articleDecoder: Encoder[ArticleView] = deriveEncoder[ArticleView]

  def fromFullArticle(fullArticle: FullArticle): ArticleView = {
    fullArticle.into[ArticleView]
      .withFieldComputed(_.timestamp, article => article.timestamp.toEpochMilli)
      .withFieldComputed(_.createTime, article => article.createTime.toEpochMilli)
      .withFieldComputed(_.categories, article => article.categories.map(_.name))
      .withFieldComputed(_.auxiliaryText, article => article.auxiliaryText
        .map(_.flatMap(elem => if (elem.isEmpty) List.empty else List(elem))))
      .transform
  }

}

