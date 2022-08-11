package search_of_article.api.enpoints

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect._
import sttp.tapir._
import search_of_article.api.response.ArticleView
import ArticleView._
import io.circe.generic.auto._
import search_of_article.model.CategoryStatistic
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody

object ArticleEndpoints {

  private val baseEndpoint = endpoint.errorOut(stringBody).in("wiki")

  private val optCategory = query[Option[String]]("category")
    .description("If you want to check the number of articles without a category, leave this field empty")

  val findArticle: PublicEndpoint[(String, Option[String]), String, String, Any] = baseEndpoint.get
    .in(path[String]("title").description("input title is insensitive to case"))
    .in(query[Option[String]]("pretty")
      .description("format for json output. Available formats: noSpaces, spaces2, spaces4"))
    .out(stringBody)

  val counterByCategory: PublicEndpoint[Option[String], String, String, Any] = baseEndpoint.get
    .in("categories" / "search")
    .description("This service allows you to find the number of articles for a particular category")
    .in(optCategory)
    .out(stringBody)

  val categoryStatistic: PublicEndpoint[Unit, String, List[CategoryStatistic], Any] = baseEndpoint.get
    .in("categories" / "statistic").description("Service for statistic data: category -> count of articles")
    .out(jsonBody[List[CategoryStatistic]])

  val updateArticle: PublicEndpoint[(String, Option[String], Option[String], Option[String]), String, List[ArticleView], Any] = baseEndpoint.put
    .in("update")
    .in(path[String]("title"))
    .in(query[Option[String]]("new title"))
    .in(query[Option[String]]("list of categories")
      .description("write list of category here, for delimiter use ';' and don't use spaces")
      .example(Some("category1,category2")))
    .in(query[Option[String]]("auxiliary text")
      .description("write an auxiliary_text here, for delimiter use '.' and don't use spaces")
      .example(Some("First element of array.Second element of array")))
    .out(jsonBody[List[ArticleView]])

  val listOfEndpoints = List(findArticle, counterByCategory, categoryStatistic, updateArticle)

}