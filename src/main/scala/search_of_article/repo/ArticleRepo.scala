package search_of_article.repo

import cats.effect.IO
import search_of_article.model._


trait ArticleRepo {

  def insertArticle(listPartArticle: List[PartitionArticle]): IO[Int]

  def insertCategoryList(categories: List[Category]): IO[Int]

  def insertAuxText(auxTextFullList: List[AuxTextLine]): IO[Int]

  def insertFullTable(artCatRelateList: List[RelationArticleCategory]): IO[Int]

  def insertCategory(category: Category): IO[Unit]

  def getAuxiliaryText(articleId: String): IO[List[String]]

  def getArticle(title: String): IO[List[PartitionArticle]]

  def getCategoryIdByArticleId(articleId: String): IO[List[String]]

  def getCategoryById(categoryId: String) : IO[Category]

  def getCategoryStatistic: IO[List[CategoryStatistic]]

  def updateArticle(fullArticle: FullArticle): IO[Unit]


  def getCategoryByName(categoryName: String): IO[Option[Category]]

  def getNumberOfArticlesByCategory(categoryId: String): IO[Int]
}
