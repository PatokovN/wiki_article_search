package search_of_article.di

import search_of_article.api.ArticleApi

import scala.concurrent.ExecutionContext

class ApiDI(serviceDI: ServiceDI)(implicit ec: ExecutionContext) {

  lazy val articleApi: ArticleApi = new ArticleApi(serviceDI.articleService)
}
