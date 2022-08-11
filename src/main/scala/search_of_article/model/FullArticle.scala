package search_of_article.model

import java.time.Instant
import java.util.UUID

case class FullArticle(id: String,
                       title: String,
                       createTime: Instant,
                       timestamp: Instant,
                       language: String,
                       wiki: String,
                       auxiliaryText: Option[List[String]],
                        categories: List[Category]
                      )

//case class AuxiliaryText(id: String, text: String)
//
//object AuxiliaryText {
//  def fromOptList(optList: Option[List[String]]): Option[List[AuxiliaryText]] =
//    optList.map(_.map(elem => AuxiliaryText(UUID.randomUUID().toString, elem)))
//}

case class Category(id: String, name: String)

case class PartitionArticle(id: String,
                            title: String,
                            createTime: Instant,
                            timestamp: Instant,
                            language: String,
                            wiki: String)