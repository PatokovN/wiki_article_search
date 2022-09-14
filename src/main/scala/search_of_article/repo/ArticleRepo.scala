package search_of_article.repo

import cats.effect.IO
import search_of_article.model._

import java.util.UUID


trait ArticleRepo {

  def insertArticle(listArticle: List[FullArticle]): IO[Unit]

  def insertCategoryList(categories: List[Category]): IO[Int]

  def insertAuxText(auxTextFullList: List[AuxTextLine]): IO[Int]

  def insertFullTable(artCatRelateList: List[RelationArticleCategory]): IO[Int]

  def insertCategory(category: Category): IO[Unit]

  def getAuxiliaryText(articleId: UUID): IO[Option[List[String]]]

  def getArticles(title: String): IO[List[PartitionArticle]]

  def getCategoryListByArticleId(articleId: UUID): IO[List[Category]]

  def getCategoryCatalog: IO[List[Category]]

  def getCategoryById(categoryId: UUID) : IO[Category]

  def getCategoryStatistic: IO[List[CategoryStatistic]]

  def updateArticle(fullArticle: FullArticle): IO[Unit]

  def deleteAuxiliaryText(articleId: UUID): IO[Unit]


  def getCategoryByName(categoryName: String): IO[Option[Category]]

  def getNumberOfArticlesByCategory(categoryId: UUID): IO[Int]
}
