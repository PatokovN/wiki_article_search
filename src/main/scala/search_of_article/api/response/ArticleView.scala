package search_of_article.api.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import search_of_article.model.FullArticle

case class ArticleView(title: String,
                  createTime: Long,
                  timestamp: Long,
                  language: String,
                  wiki: String,
                  categories: List[String],
                  auxiliaryText: Option[List[String]])

object ArticleView {
  implicit val articleEncoder: Decoder[ArticleView] = deriveDecoder[ArticleView]
  implicit val articleDecoder: Encoder[ArticleView] = deriveEncoder[ArticleView]

  def fromFullArticle(fullArticle: FullArticle): ArticleView =
    new ArticleView(fullArticle.title,
                    fullArticle.createTime.toEpochMilli,
                    fullArticle.timestamp.toEpochMilli,
                    fullArticle.language,
                    fullArticle.wiki,
                    fullArticle.categories.map(_.name),
                    fullArticle.auxiliaryText)

}

