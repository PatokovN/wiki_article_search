package search_of_article.api

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect._
import cats.syntax.all._
import org.http4s.HttpRoutes
import search_of_article.api.response.ArticleView
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import search_of_article.services.ArticleService
import ArticleView._
import io.circe.syntax.EncoderOps
import search_of_article.api.enpoints.ArticleEndpoints


class ArticleApi(articleService: ArticleService) {

  val findArticleRoute: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]()
      .toRoutes(ArticleEndpoints.findArticle
        .serverLogicSuccess(title => articleService.find(title._1)
          .map(data => {
            val viewList = data.map(ArticleView.fromFullArticle)
            convertIntoStringOfFormat(viewList, title._2)
          })
        )
      )

  val counterByCategoryRoute: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]()
      .toRoutes(ArticleEndpoints.counterByCategory
        .serverLogicSuccess(categoryName =>
          articleService.counterByCategory(categoryName.getOrElse("")).map(_.toString))
      )

  val categoryStatisticRoute: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]()
      .toRoutes(ArticleEndpoints.categoryStatistic
        .serverLogicSuccess(_ =>
          articleService.statisticByCategories))

  val updateArticleRoute: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]()
      .toRoutes(ArticleEndpoints.updateArticle
        .serverLogicSuccess(request => {
          val title = request._1
          val optCategoryList = request._3.map(_.split(",").toList)
          val optAuxiliaryText = request._4.map(_.split("\\.").toList)
          articleService
            .update(title, request._2, optCategoryList, optAuxiliaryText)
            .map(list => list.map(ArticleView.fromFullArticle))
        }))

  val swaggerUIRoutes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(
    SwaggerInterpreter().fromEndpoints[IO](ArticleEndpoints.listOfEndpoints,
      "Search_of_article_from_Wikipedia", "1.0.0 ha")
  )

  val articleRoutes = findArticleRoute <+> counterByCategoryRoute <+>
    categoryStatisticRoute <+> updateArticleRoute <+> swaggerUIRoutes

  private def convertIntoStringOfFormat(listArticle: List[ArticleView], prettyFormat: Option[String]): String = {

    prettyFormat match {
      case Some(format) => {
        format match {
          case "noSpaces" => listArticle.asJson.noSpaces
          case "spaces2" => listArticle.asJson.spaces2
          case "spaces4" => listArticle.asJson.spaces4
          case _ => "wrong pretty format!"
        }
      }
      case None => listArticle.asJson.noSpaces
    }
  }
}
