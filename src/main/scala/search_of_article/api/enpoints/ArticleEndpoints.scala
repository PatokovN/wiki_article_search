package search_of_article.api.enpoints

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect._
import sttp.tapir._
import search_of_article.api.response.ArticleView
import ArticleView._
import io.circe.generic.auto._
import search_of_article.api.request.UpdateRequest
import search_of_article.model.CategoryStatistic
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.model.StatusCode


sealed trait CustomError
case class NotFound(error: String) extends CustomError
case class ServerError(error: String) extends CustomError

object ArticleEndpoints {

  private val baseEndpoint = endpoint.errorOut(
    oneOf[CustomError](
      oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFound].description("not found"))),
      oneOfDefaultVariant(jsonBody[ServerError].description("server error"))
    )
  ).in("wiki")

  val findArticle: PublicEndpoint[(String, Option[String]), CustomError, String, Any] = baseEndpoint.get
    .in(path[String]("title").description("input title is insensitive to case"))
    .in(query[Option[String]]("pretty")
      .description("format for json output. Available formats: noSpaces, spaces2, spaces4"))
    .out(stringBody)

  val counterByCategory: PublicEndpoint[String, CustomError, String, Any] = baseEndpoint.get
    .in("categories" / "search")
    .description("This service allows you to find the number of articles for a particular category")
    .in(path[String]("category_name"))
    .out(stringBody)

  val categoryStatistic: PublicEndpoint[Unit, CustomError, List[CategoryStatistic], Any] = baseEndpoint.get
    .in("categories" / "statistic").description("Service for statistic data: category -> count of articles")
    .out(jsonBody[List[CategoryStatistic]])

  val updateArticle: PublicEndpoint[UpdateRequest, CustomError, List[ArticleView], Any] = baseEndpoint.put
    .in("update")
    .in(jsonBody[UpdateRequest])
    .out(jsonBody[List[ArticleView]])

  val listOfEndpoints = List(findArticle, counterByCategory, categoryStatistic, updateArticle)

}
