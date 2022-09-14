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

  def errorHandler[A](response: IO[A]): IO[Either[CustomError, A]] =
    response.attempt.map[Either[CustomError, A]] {
      case Left(notFound: IllegalArgumentException) => NotFound(notFound.getMessage).asLeft[A]
      case Left(error: Throwable) => ServerError(error.getMessage).asLeft[A]
      case Right(value) => value.asRight[CustomError]
  }
  def errorHandlerList[A](response: IO[List[A]]): IO[Either[CustomError, List[A]]] =
    response.attempt.map[Either[CustomError, List[A]]] {
      case Left(notFound: IllegalArgumentException) => NotFound(notFound.getMessage).asLeft[List[A]]
      case Left(error: Throwable) => ServerError(error.getMessage).asLeft[List[A]]
      case Right(value) => value.asRight[CustomError]
  }

  val findArticleRoute: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]()
      .toRoutes(ArticleEndpoints.findArticle
        .serverLogic {title =>
          val response = articleService.find(title._1)
            .map { data =>
              val viewList = data.map(ArticleView.fromFullArticle)
              convertIntoStringOfFormat(viewList, title._2)
            }
          errorHandler(response)
          }
      )

  val counterByCategoryRoute: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]()
      .toRoutes(ArticleEndpoints.counterByCategory
        .serverLogic(categoryName =>
          errorHandler(articleService.counterByCategory(categoryName).map(_.toString)))
      )

  val categoryStatisticRoute: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]()
      .toRoutes(ArticleEndpoints.categoryStatistic
        .serverLogic(_ =>
          errorHandlerList(articleService.statisticByCategories))
      )

  val updateArticleRoute: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]()
      .toRoutes(ArticleEndpoints.updateArticle
        .serverLogic(request =>
          errorHandlerList(articleService
            .update(request.oldTitle, request.optNewTitle, request.optCategoryList, request.optAuxiliaryText)
            .map(list => list.map(ArticleView.fromFullArticle)))
        )
      )

  val swaggerUIRoutes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(
    SwaggerInterpreter().fromEndpoints[IO](ArticleEndpoints.listOfEndpoints,
      "Search_of_article_from_Wikipedia", "1.0.1")
  )

  val articleRoutes = findArticleRoute <+> counterByCategoryRoute <+>
    categoryStatisticRoute <+> swaggerUIRoutes <+> updateArticleRoute

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
