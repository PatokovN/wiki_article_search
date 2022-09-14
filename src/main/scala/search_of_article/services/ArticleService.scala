package search_of_article.services

import cats.effect.IO
import search_of_article.model.{CategoryStatistic, FullArticle}

trait ArticleService {
  def parsingOfFile(path: String): IO[Unit]

  def find(title: String): IO[List[FullArticle]]

  def statisticByCategories: IO[List[CategoryStatistic]]

  def update(title: String,
             newOptTitle: Option[String],
             optCategoryList: Option[List[String]],
             optAuxiliaryText: Option[List[String]]): IO[List[FullArticle]]

  def counterByCategory(categoryName: String): IO[Int]

}
