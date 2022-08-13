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
import search_of_article.api.enpoints._
import search_of_article.model.CategoryStatistic

class ArticleApi(articleService: ArticleService) {

  val findArticleRoute: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]()
      .toRoutes(ArticleEndpoints.findArticle
        .serverLogic(title => articleService.find(title._1)
          .map(data => {
            val viewList = data.map(ArticleView.fromFullArticle)
            convertIntoStringOfFormat(viewList, title._2)
          }).attempt.map[Either[CustomError,String]]{
          case Left(notFound:IllegalArgumentException) => NotFound(notFound.getMessage).asLeft[String]
          case Left(error: Throwable) => ServerError(error.getMessage).asLeft[String]
          case Right(value) => value.asRight[CustomError]
        })
      )

  val counterByCategoryRoute: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]()
      .toRoutes(ArticleEndpoints.counterByCategory
        .serverLogic(categoryName =>
          articleService.counterByCategory(categoryName.getOrElse(""))
            .map(_.toString).attempt.map[Either[CustomError,String]]{
            case Left(notFound:IllegalArgumentException) => NotFound(notFound.getMessage).asLeft[String]
            case Left(error: Throwable) => ServerError(error.getMessage).asLeft[String]
            case Right(value) => value.asRight[CustomError]
          })
      )

  val categoryStatisticRoute: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]()
      .toRoutes(ArticleEndpoints.categoryStatistic
        .serverLogic(_ => articleService.statisticByCategories.attempt
          .map[Either[CustomError,List[CategoryStatistic]]]{
          case Left(error: Throwable) => ServerError(error.getMessage).asLeft[List[CategoryStatistic]]
          case Right(value) => value.asRight[CustomError]
        }))

  val updateArticleRoute: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]()
      .toRoutes(ArticleEndpoints.updateArticle
        .serverLogic(request => {
          val title = request._1
          val optCategoryList = request._3.map(_.split(",").toList)
          val optAuxiliaryText = request._4.map(_.split("\\.").toList)
          articleService
            .update(title, request._2, optCategoryList, optAuxiliaryText)
            .map(list => list.map(ArticleView.fromFullArticle))
        }.attempt.map[Either[CustomError,List[ArticleView]]]{
          case Left(notFound:IllegalArgumentException) => NotFound(notFound.getMessage).asLeft[List[ArticleView]]
          case Left(error: Throwable) => ServerError(error.getMessage).asLeft[List[ArticleView]]
          case Right(value) => value.asRight[CustomError]
        })
      )

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
