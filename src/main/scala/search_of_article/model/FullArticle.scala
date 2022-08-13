package search_of_article.model

import java.time.Instant

case class FullArticle(id: String,
                       title: String,
                       createTime: Instant,
                       timestamp: Instant,
                       language: String,
                       wiki: String,
                       auxiliaryText: Option[List[String]],
                        categories: List[Category]
                      )

case class Category(id: String, name: String)

case class DataLine(part: PartitionArticle,text: Option[List[String]], category: Category)

object DataLine {

  def fromFullArticle(fullArticle: FullArticle): List[DataLine] =
    for {
      category <- fullArticle.categories
    } yield DataLine(PartitionArticle.fromFullArticle(fullArticle), fullArticle.auxiliaryText, category)
}

case class PartitionArticle(id: String,
                            title: String,
                            createTime: Instant,
                            timestamp: Instant,
                            language: String,
                            wiki: String
                           )

object PartitionArticle {

  def fromFullArticle(fullArticle: FullArticle): PartitionArticle =
    PartitionArticle(fullArticle.id, fullArticle.title, fullArticle.createTime, fullArticle.timestamp,
      fullArticle.language, fullArticle.wiki)
}

case class AuxTextLine(articleId: String, text: Option[String])

case class RelationArticleCategory(articleId: String, categoryId: String)
