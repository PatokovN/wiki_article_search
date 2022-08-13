package search_of_article.services.impl

import cats.implicits._
import cats.effect._
import io.circe.{DecodingFailure, Json}
import io.circe.parser.parse
import search_of_article.model._
import search_of_article.repo.ArticleRepo
import search_of_article.services.ArticleService

import java.time.Instant
import java.util.UUID
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.{Failure, Success, Try}

class ArticleServiceImpl(repo: ArticleRepo)(implicit ec: ExecutionContext) extends ArticleService {

  override def parsingOfFile(path: String): IO[Unit] =
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

  private def insertTransaction(list: List[Either[Throwable, FullArticle]]): IO[Unit] = {
    val invalidArtListIO: IO[List[FullArticle]] = list.map(IO.fromEither).sequence
    for {
      invalidArtList <- invalidArtListIO
      validDataListIO = formingCategoryCatalog(invalidArtList)
      validDataList <- validDataListIO
      partialArticle = validDataList.map(_.part).distinct
      categories = validDataList.map(_.category).distinct
      auxTextAll = auxTextRelate(validDataList).distinct
      fullDataRelate = validDataList.map(dataLine => RelationArticleCategory(dataLine.part.id, dataLine.category.id))
      _ <- repo.insertArticle(partialArticle)
      _ <- repo.insertCategoryList(categories)
      _ <- repo.insertAuxText(auxTextAll)
      unit <- repo.insertFullTable(fullDataRelate)
    } yield unit

  }

  override def find(title: String): IO[List[FullArticle]] = {
    for {
      listPartArticle <- repo.getArticle(title)
      validateList: IO[List[PartitionArticle]] =
        if (listPartArticle.isEmpty) articleNotFound(title) else IO.pure(listPartArticle)
      _ <- validateList
      listFullArticleIO = listPartArticle.map { partArticle =>
        for {
          listOfOpt <- repo.getAuxiliaryText(partArticle.id)
          optAuxText: Option[List[String]] = convertListOption(listOfOpt)
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
          auxText <- optAuxiliaryText
            .fold(repo.getAuxiliaryText(article.id)
              .map(l => convertListOption(l)))(elem => IO.pure(Some(elem)))
          updatedCategoryList <- updateCategoryList(article.id, optCategoryList)
          fullArticle = FullArticle(
            id = article.id,
            title = newOptTitle.getOrElse(article.title),
            createTime = article.createTime,
            timestamp = Instant.now(),
            language = article.language,
            wiki = article.wiki,
            auxiliaryText = auxText,
            categories = updatedCategoryList
          )
          _ <- repo.updateArticle(fullArticle)
        } yield fullArticle
      }.sequence
      listFullArticle <- listFullArticleIO
    } yield listFullArticle
  }

  // additional implementation - this method search the number of articles for the only one concrete category

  override def counterByCategory(categoryName: String): IO[Int] =
    for {
      optCategory <- repo.getCategoryByName(categoryName)
      category <- optCategory.fold(categoryNotFound(categoryName))(c => IO.pure(c))
      categoryId = category.id
      numberOfArticlesByCategory <- repo.getNumberOfArticlesByCategory(categoryId)
    } yield numberOfArticlesByCategory


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

  private def auxTextRelate(listData: List[DataLine]): List[AuxTextLine] = {
    listData.flatMap(dataLine => {
      dataLine.text match {
        case Some(textLines) => {
          textLines.map(text => AuxTextLine(dataLine.part.id, Some(text)))
        }
        case None => List(AuxTextLine(dataLine.part.id, None))
      }
    })
  }

  private def formingCategoryCatalog(listData: List[FullArticle]): IO[List[DataLine]] = {
    val dataLinesInvalid = listData.flatMap(DataLine.fromFullArticle)

    @tailrec
    def innerFormCategoryCatalog(listDataLines: List[DataLine],
                                 accumForCategory: Map[String, Category],
                                 validDatalines: List[DataLine]): List[DataLine] = {
      listDataLines match {
        case Nil => validDatalines
        case h :: tail =>
          if (accumForCategory.contains(h.category.name)) {
            val validDataLine = DataLine(h.part, h.text, accumForCategory(h.category.name))
            innerFormCategoryCatalog(tail, accumForCategory, validDataLine :: validDatalines)
          }
          else
            innerFormCategoryCatalog(tail, accumForCategory + (h.category.name -> h.category), h :: validDatalines)
      }
    }

    val validDataLines: Future[List[DataLine]] =
      Future {
        innerFormCategoryCatalog(dataLinesInvalid, Map.empty, List.empty)
      }

    IO.fromFuture(IO(validDataLines))
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
      case Some(newList) => updateBaseForNewListCategory(newList.distinct)
      case None => fetchExistedListCategory(articleId)
    }
  }

  @tailrec
  private def convertListOption(list: List[Option[String]], acc: Option[List[String]] = None): Option[List[String]] = list match {
    case Nil => acc.map(_.reverse)
    case h :: t =>
      h match {
        case Some(value) => convertListOption(t, Some(value :: acc.getOrElse(Nil)))
        case None => acc.map(_.reverse)
      }
  }


private def articleNotFound (title: String): IO[List[PartitionArticle]] = {
  IO.raiseError[List[PartitionArticle]] (new IllegalArgumentException (s"title with name \"$title\" not found!") )
  }

  private def categoryNotFound (categoryName: String): IO[Category] = {
  IO.raiseError[Category] (new IllegalArgumentException (s"Categories with name \"$categoryName\" not found") )
  }
  }
