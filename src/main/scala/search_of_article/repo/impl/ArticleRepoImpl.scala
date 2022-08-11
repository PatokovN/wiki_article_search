package search_of_article.repo.impl

import search_of_article.model.{Category, CategoryStatistic, FullArticle, PartitionArticle}
import search_of_article.repo.ArticleRepo
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import doobie.{ConnectionIO, Transactor}
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.fragment.Fragment

class ArticleRepoImpl(transactor: Transactor[IO])(implicit runtime: IORuntime) extends ArticleRepo {

  override def insertArticle(article: FullArticle): IO[Unit] = unsafeRun {
    sql"""insert into articles (id, title, create_time, timestamp, language, wiki)
            values (${article.id}, ${article.title}, ${article.createTime},
            ${article.timestamp}, ${article.language}, ${article.wiki})"""
      .update.run.map(_ => ())
  }

  override def insertAuxiliaryText(articleId: String, auxiliaryText: Option[List[String]]): IO[Unit] = unsafeRun {
    val insertText = auxiliaryText match {
      case None => Fragment.empty
      case Some(list) => list.map(text =>
        sql"""insert into auxiliary_text_table (article_id, text)
            values (${articleId}, $text);""").reduce(_ ++ _)
    }

    insertText.update.run.map(_ => ())
  }

  override def insertCategory(category: Category): IO[Unit] = unsafeRun {
    sql"""insert into category_catalog (id, category) values (${category.id}, ${category.name});"""
      .update.run.map(_ => ())
  }

  override def insertFullInfo(article: FullArticle): IO[Unit] = unsafeRun {
    article.categories.map(category =>
      sql"""insert into full_info_table (article_id, category_id)
           values (${article.id}, ${category.id});""").reduce((res, a) => res ++ a)
      .update.run.map(_ => ())
  }

  override def getAuxiliaryText(articleId: String): IO[List[String]] = unsafeRun {
    sql"select text from auxiliary_text_table where article_id = $articleId"
      .query[String]
      .to[List]
  }

  override def getCategoryStatistic: IO[List[CategoryStatistic]] = unsafeRun {
    sql"""
    select category, count(article_id) from full_info_table fit
    right outer join category_catalog cc
    on(fit.category_id  = cc.id)
    group by category
    order by count(article_id);
    """.query[CategoryStatistic].to[List]
  }

  override def getCategoryIdByArticleId(articleId: String): IO[List[String]] = unsafeRun {
    sql"select category_id from full_info_table where article_id = $articleId"
      .query[String]
      .to[List]
  }

  override def getCategoryById(categoryId: String): IO[Category] = unsafeRun {
    sql"select id, category from category_catalog where id = ${categoryId}"
      .query[Category]
      .unique
  }

  override def getArticle(title: String): IO[List[PartitionArticle]] = unsafeRun {
    sql"""select id, title, create_time, timestamp, language, wiki
           from articles where LOWER(title) = LOWER($title)"""
      .query[PartitionArticle]
      .to[List]
  }


  override def updateArticle(fullArticle: FullArticle): IO[Unit] = unsafeRun {

    val deleteFullTableRelation =
      sql"""
        delete from full_info_table where article_id = ${fullArticle.id};
         """

    val insertNewRelationFullTable = fullArticle.categories.map(category =>
      sql"""
          insert into full_info_table (article_id, category_id)
           values (${fullArticle.id}, ${category.id});""").reduce(_ ++ _)

    val deleteAuxText = sql" delete from auxiliary_text_table where article_id = ${fullArticle.id};"

    val insertNewAuxText = fullArticle.auxiliaryText match {
      case None => Fragment.empty
      case Some(list) => list.map(text =>
        sql"""insert into auxiliary_text_table (article_id, text)
           values (${fullArticle.id}, ${text});"""
      ).reduce(_ ++ _)
    }

    val updateArticle =
      sql"""
          update articles set title = ${fullArticle.title}, timestamp = ${fullArticle.timestamp}
          where id = ${fullArticle.id};
       """

    val deleteUselessCategory =
      sql"""
          delete from category_catalog where id in(
            select cc.id  from full_info_table fit
            right outer join category_catalog cc
            on(fit.category_id  = cc.id)
            group by fit.article_id, cc.id, cc.category
            having article_id is null);
         """

    (updateArticle
      ++ deleteFullTableRelation
      ++ deleteAuxText
      ++ insertNewRelationFullTable
      ++ insertNewAuxText
      ++ deleteUselessCategory
      ).update.run.map(_ => ())
  }

  private def unsafeRun[T](query: ConnectionIO[T]): IO[T] =
    query.transact(transactor)

  override def getNumberOfArticlesByCategory(categoryId: String): IO[Int] = unsafeRun {
    sql"select count (*) FROM full_info_table where category_id = $categoryId"
      .query[Int]
      .unique
  }

  override def getCategoryByName(categoryName: String): IO[Option[Category]] = unsafeRun {
    sql"select id, category from category_catalog where category = $categoryName".query[Category].option
  }
}
