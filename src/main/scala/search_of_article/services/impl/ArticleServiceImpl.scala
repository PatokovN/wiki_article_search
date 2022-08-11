package search_of_article.services.impl

import cats.implicits._
import cats.effect._
import io.circe.{DecodingFailure, Json}
import io.circe.parser.parse
import search_of_article.model.{Category, CategoryStatistic, FullArticle, PartitionArticle}
import search_of_article.repo.ArticleRepo
import search_of_article.services.ArticleService

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.io.Source
import scala.util.{Failure, Success, Try}

class ArticleServiceImpl(repo: ArticleRepo)(implicit ec: ExecutionContext) extends ArticleService {

  override def fillTable(path: String): IO[Unit] =
    Try {
      val fullJson = Source.fromFile(path)
      val readFile = fullJson.getLines().toList
      val onlySecondLines = readFile.zipWithIndex.filter(elem => elem._2 % 2 != 0).map(_._1)
      onlySecondLines.map(str => {
        for {
          line <- parse(str)
          listParsedValue <- customParserDataLine(line)
        } yield listParsedValue
      }
      )
    } match {
      case Failure(exception) => IO.raiseError(exception)
      case Success(listOfData) => insertTransaction(listOfData)
    }

  private def insertTransaction(list: List[Either[Throwable, FullArticle]]): IO[Unit] =
    list.map {
      case Left(_) => IO.unit
      case Right(article) =>
        val validatedCategories: IO[List[Category]] = article.categories.map { category =>
          for {
            optCategory <- repo.getCategoryByName(category.name)
            _ <- optCategory.fold(repo.insertCategory(category))(_ => IO.unit)
          } yield optCategory.getOrElse(category)
        }.sequence

        for {
          newCategories <- validatedCategories
          newArticle = article.copy(categories = newCategories)
          _ <- repo.insertArticle(newArticle)
          _ <- repo.insertAuxiliaryText(article.id, article.auxiliaryText)
          result <- repo.insertFullInfo(newArticle)
        } yield result
    }.sequence_


  private def customParserDataLine(value: Json): Either[DecodingFailure, FullArticle] = {
    val cursor = value.hcursor
    for {
      title <- cursor.downField("title").as[String]
      createTime <- cursor.downField("create_timestamp").as[Instant]
      timestamp <- cursor.downField("timestamp").as[Instant]
      language <- cursor.downField("language").as[String]
      wiki <- cursor.downField("wiki").as[String]
      categoryListOpt <- cursor.downField("category").as[List[String]]
      auxiliaryTextOnly <- cursor.downField("auxiliary_text").as[Option[List[String]]]

      categoryList = if (categoryListOpt.isEmpty) List("") else categoryListOpt
      auxiliaryTextValidEmpty = auxiliaryTextOnly.map(list => if (list.isEmpty) List("") else list)
    } yield {
      val articleId = UUID.randomUUID().toString
      val categories = categoryList.map(string => Category(UUID.randomUUID().toString, string))
      FullArticle(articleId, title, createTime, timestamp, language, wiki, auxiliaryTextValidEmpty, categories)
    }
  }

  override def find(title: String): IO[List[FullArticle]] = {
    for {
      listPartArticle <- repo.getArticle(title)
      validateList: IO[List[PartitionArticle]] =
        if (listPartArticle.isEmpty) articleNotFound(title) else IO.pure(listPartArticle)
      _ <- validateList
      listFullArticleIO = listPartArticle.map { partArticle =>
        for {
          auxText <- repo.getAuxiliaryText(partArticle.id)
          optAuxText = if (auxText.isEmpty) None else Some(auxText)
          listOfCategoryId <- repo.getCategoryIdByArticleId(partArticle.id)
          categoryListIO = listOfCategoryId.map(id => repo.getCategoryById(id)).sequence
          categoryList <- categoryListIO
        } yield FullArticle(partArticle.id, partArticle.title, partArticle.createTime, partArticle.timestamp,
          partArticle.language, partArticle.wiki, optAuxText, categoryList)
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
      listPartArticle <- repo.getArticle(title)
      validateList: IO[List[PartitionArticle]] =
        if (listPartArticle.isEmpty) articleNotFound(title) else IO.pure(listPartArticle)
      _ <- validateList
      listFullArticleIO = listPartArticle.map { article =>
        for {
          auxText <- optAuxiliaryText.fold(repo.getAuxiliaryText(article.id))(elem => IO.pure(elem))
          optAuxText = if (auxText.isEmpty) None else Some(auxText)
          updatedCategoryList <- updateCategoryList(article.id, optCategoryList)
          fullArticle = FullArticle(
            id = article.id,
            title = newOptTitle.getOrElse(article.title),
            createTime = article.createTime,
            timestamp = Instant.now(),
            language = article.language,
            wiki = article.wiki,
            auxiliaryText = optAuxText,
            categories = updatedCategoryList
          )
          _ <- repo.updateArticle(fullArticle)
        } yield fullArticle
      }.sequence
      listFullArticle <- listFullArticleIO
    } yield listFullArticle
  }

  private def updateCategoryList(articleId: String, optCategoryList: Option[List[String]]): IO[List[Category]] = {

    def updateBaseForNewListCategory(categoryList: List[String]): IO[List[Category]] = {
      val result = categoryList.map { category =>
        for {
          optCategory <- repo.getCategoryByName(category)
          updateCategory <- optCategory match {
            case None =>
              val newCategory = Category(UUID.randomUUID().toString, category)
              repo.insertCategory(newCategory).flatMap(_ => IO.pure(newCategory))

            case Some(someCategory) => IO.pure(someCategory)
          }
        } yield updateCategory
      }
      result.sequence
    }

    def fetchExistedListCategory(articleId: String): IO[List[Category]] = {
      for {
        listOfCategoryId <- repo.getCategoryIdByArticleId(articleId)
        categoryListIO = listOfCategoryId.map(id => repo.getCategoryById(id)).sequence
        categoryList <- categoryListIO
      } yield categoryList
    }

    optCategoryList match {
      case Some(newList) => updateBaseForNewListCategory(newList)
      case None => fetchExistedListCategory(articleId)
    }
  }

  private def articleNotFound(title: String): IO[List[PartitionArticle]] = {
    IO.raiseError[List[PartitionArticle]](new RuntimeException(s"Article with title - $title not found"))
  }

  private def categoryNotFound(categoryName: String): IO[Category] = {
    IO.raiseError[Category](new RuntimeException(s"Categories with name $categoryName not found"))
  }

  // additional implementation - this method search the number of articles for the only one concrete category

  override def counterByCategory(categoryName: String): IO[Int] =
    for {
      optCategory <- repo.getCategoryByName(categoryName)
      category <- optCategory.fold(categoryNotFound(categoryName))(c => IO.pure(c))
      categoryId = category.id
      numberOfArticlesByCategory <- repo.getNumberOfArticlesByCategory(categoryId)
    } yield numberOfArticlesByCategory
}
