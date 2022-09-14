package search_of_article.model

import java.time.Instant
import java.util.UUID


case class FullArticle(id: UUID,
                       title: String,
                       createTime: Instant,
                       timestamp: Instant,
                       language: String,
                       wiki: String,
                       auxiliaryText: Option[List[String]],
                       categories: List[Category])

case class Category(categoryId: UUID, name: String)
case class PartitionArticle(id: UUID,
                            title: String,
                            createTime: Instant,
                            timestamp: Instant,
                            language: String,
                            wiki: String
                           )

case class AuxTextLine(articleId: UUID, text: String)

case class RelationArticleCategory(articleId: UUID, categoryId: UUID)
