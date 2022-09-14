package search_of_article.di

import search_of_article.services.ArticleService
import search_of_article.services.impl.ArticleServiceImpl

import scala.concurrent.ExecutionContext

class ServiceDI(repoDI: RepoDI)(implicit ec: ExecutionContext) {

  import repoDI._

  lazy val articleService: ArticleService = new ArticleServiceImpl(repo)

}
