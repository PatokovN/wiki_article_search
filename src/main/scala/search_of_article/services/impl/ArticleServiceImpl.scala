package search_of_article.services.impl

import cats.implicits._
import cats.effect._
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}
import io.circe.parser.parse
import search_of_article.model._
import search_of_article.repo.ArticleRepo
import search_of_article.services.ArticleService

import java.io.BufferedReader
import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.io.Source

class ArticleServiceImpl(repo: ArticleRepo)(implicit ec: ExecutionContext) extends ArticleService {

  implicit val decodeFullArticle: Decoder[FullArticle] = Decoder.forProduct7(
    "title", "create_timestamp", "timestamp", "language",
    "wiki", "category", "auxiliary_text")(
    (title: String, createTime: Instant, timestamp: Instant, language: String, wiki: String,
     categories: List[String], auxiliaryText: Option[List[String]])
    => FullArticle(UUID.randomUUID(), title, createTime, timestamp, language, wiki, auxiliaryText,
      categories.map(elem => Category(UUID.randomUUID(), elem))))

  implicit val encodeFullArticle: Encoder[FullArticle] = Encoder.forProduct8("id",
    "title", "create_timestamp", "timestamp", "language",
    "wiki", "category", "auxiliary_text")(art => (art.id, art.title, art.createTime,
    art.timestamp, art.language, art.wiki, art.categories, art.auxiliaryText))

  override def parsingOfFile(path: String): IO[Unit] = {
    val fullInput = IO(Source.fromFile(path))
    for {
      input <- fullInput
      unit <- readAndParse(input.bufferedReader())
    } yield unit
  }

  private def readAndParse(buffer: BufferedReader): IO[Unit] =
    if (buffer.ready()) {
      val articlesList = (1 to 4000)
        .map(_ =>
          if (buffer.ready()) {
            buffer.readLine()
            buffer.readLine()
          } else "")
        .takeWhile(_.nonEmpty)
        .map { textLine =>
          for {
            parsedLine <- IO.fromEither(parse(textLine))
            article <- IO.fromEither(parsedLine.as[FullArticle])
          } yield article
        }.toList.sequence
      insertTransaction2(buffer,articlesList)
    }else IO.unit

  private def insertTransaction2(buffer: BufferedReader,articleListIO: IO[List[FullArticle]]): IO[Unit] =
    for {
      articleList <- articleListIO
      categoryFromBase <- repo.getCategoryCatalog
      articlesAndCatalog = articleList.foldLeft(List[FullArticle](), categoryFromBase) {
        (articlesAndCatalog, art) =>
          val catalog = articlesAndCatalog._2
          val articles = articlesAndCatalog._1
          val newCategories = art.categories.map { category =>
            catalog.find(elem => elem.name == category.name).getOrElse(category)
          }
          val newCatalog = (newCategories ::: catalog).distinct
          val newArticle = art.copy(categories = newCategories)
          (newArticle :: articles, newCatalog)
      }
      updatedCatalog = articlesAndCatalog._2
      newCategories = updatedCatalog.diff(categoryFromBase)
      updatedArticles = articlesAndCatalog._1
      auxTextAll = updatedArticles.flatMap(article => auxTextRelate(article.id,article.auxiliaryText))
      fullDataRelate = updatedArticles.flatMap {
        article => article.categories.map(cat => RelationArticleCategory(article.id, cat.categoryId))
      }
      _ <- repo.insertArticle(updatedArticles)
      _ <- repo.insertCategoryList(newCategories)
      _ <- repo.insertAuxText(auxTextAll)
      _ <- repo.insertFullTable(fullDataRelate)
      unit <- readAndParse(buffer)
    } yield unit

  override def find(title: String): IO[List[FullArticle]] = {
    for {
      listPartArticle <- repo.getArticles(title)
      validateList = if (listPartArticle.isEmpty) articleNotFound(title) else IO.pure(listPartArticle)
      _ <- validateList
      listFullArticleIO = listPartArticle.map { partArticle =>
        for {
          listOfOpt <- repo.getAuxiliaryText(partArticle.id)
          listOfCategory <- repo.getCategoryListByArticleId(partArticle.id)
        } yield FullArticle(partArticle.id, partArticle.title, partArticle.createTime, partArticle.timestamp,
          partArticle.language, partArticle.wiki, listOfOpt, listOfCategory)
      }.sequence
      listFullArticle <- listFullArticleIO
    } yield listFullArticle
  }


  override def statisticByCategories: IO[List[CategoryStatistic]] = repo.getCategoryStatistic

  override def update(title: String,
                      newOptTitle: Option[String],
                      optCategoryList: Option[List[String]],
                      optAuxiliaryText: Option[List[String]]): IO[List[FullArticle]] = {

    for {
      listPartArticle <- repo.getArticles(title)
      validateList = if (listPartArticle.isEmpty) articleNotFound(title) else IO.pure(listPartArticle)
      _ <- validateList
      listFullArticleIO = listPartArticle.map {article =>
        for {
          auxText <- optAuxiliaryText.fold(repo.getAuxiliaryText(article.id)){text =>
            val auxText = auxTextRelate(article.id,Some(text))
            for {
              _ <- repo.deleteAuxiliaryText(article.id)
              _ <- repo.insertAuxText(auxText)
            }yield Some(text)
          }
          updatedCategoryList <- updateCategoryList(article.id, optCategoryList)
          newFullArticle = FullArticle(
            id = article.id,
            title = newOptTitle.getOrElse(article.title),
            createTime = article.createTime,
            timestamp = Instant.now(),
            language = article.language,
            wiki = article.wiki,
            auxiliaryText = auxText,
            categories = updatedCategoryList
          )
          _ <- repo.updateArticle(newFullArticle)
        } yield newFullArticle
      }.sequence
      listFullArticle <- listFullArticleIO
    } yield listFullArticle
  }

//   additional implementation - this method search the number of articles for the only one concrete category

  override def counterByCategory(categoryName: String): IO[Int] =
    for {
      optCategory <- repo.getCategoryByName(categoryName)
      category <- optCategory.fold(categoryNotFound(categoryName))(c => IO.pure(c))
      categoryId = category.categoryId
      numberOfArticlesByCategory <- repo.getNumberOfArticlesByCategory(categoryId)
    } yield numberOfArticlesByCategory

  private def auxTextRelate(articleId: UUID, auxiliaryText: Option[List[String]]): List[AuxTextLine] = {
    auxiliaryText match {
        case Some(textList) =>
          if(textList.nonEmpty) textList.map(text => AuxTextLine(articleId, text))
          else List(AuxTextLine(articleId, ""))
        case None => Nil
      }
  }

  private def updateCategoryList(articleId: UUID, optCategoryList: Option[List[String]]): IO[List[Category]] = {

    def updateBaseForNewListCategory(categoryList: List[String]): IO[List[Category]] = {
      val result = categoryList.map {category =>
        for {
          optCategory <- repo.getCategoryByName(category)
          updateCategory <- optCategory match {
            case None =>
              val newCategory = Category(UUID.randomUUID(), category)
              repo.insertCategory(newCategory).flatMap(_ => IO.pure(newCategory))

            case Some(someCategory) => IO.pure(someCategory)
          }
        } yield updateCategory
      }
      result.sequence
    }

    optCategoryList match {
      case Some(newList) => updateBaseForNewListCategory(newList.distinct)
      case None => repo.getCategoryListByArticleId(articleId)
    }
  }

  private def articleNotFound(title: String): IO[List[PartitionArticle]] = {
    IO.raiseError[List[PartitionArticle]](new IllegalArgumentException(s"title with name \"$title\" not found!"))
  }

  private def categoryNotFound(categoryName: String): IO[Category] = {
    IO.raiseError[Category](new IllegalArgumentException(s"Categories with name \"$categoryName\" not found"))
  }
}
