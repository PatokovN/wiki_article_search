package search_of_article.repo

import cats.effect.IO
import search_of_article.model.{Category, CategoryStatistic, FullArticle, PartitionArticle}


trait ArticleRepo {

  def insertArticle(article: FullArticle): IO[Unit]

  def insertCategory(category: Category): IO[Unit]

  def insertAuxiliaryText(articleId: String, auxiliaryText: Option[List[String]]): IO[Unit]

  def insertFullInfo(article: FullArticle): IO[Unit]

  def getAuxiliaryText(articleId: String): IO[List[String]]

  def getArticle(title: String): IO[List[PartitionArticle]]

  def getCategoryIdByArticleId(articleId: String): IO[List[String]]

  def getCategoryById(categoryId: String) : IO[Category]

  def getCategoryStatistic: IO[List[CategoryStatistic]]

  def updateArticle(fullArticle: FullArticle): IO[Unit]


  def getCategoryByName(categoryName: String): IO[Option[Category]]

  def getNumberOfArticlesByCategory(categoryId: String): IO[Int]
}
