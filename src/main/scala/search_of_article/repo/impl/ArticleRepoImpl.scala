package search_of_article.repo.impl

import search_of_article.model.{AuxTextLine, Category, CategoryStatistic, FullArticle, PartitionArticle, RelationArticleCategory}
import search_of_article.repo.ArticleRepo
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import doobie.{ConnectionIO, Transactor}
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.fragment.Fragment
import doobie.util.update.Update

class ArticleRepoImpl(transactor: Transactor[IO])(implicit runtime: IORuntime) extends ArticleRepo {

  override def insertCategory(category: Category): IO[Unit] = toIO {
    sql"""insert into category_catalog (id, category) values (${category.id}, ${category.name});"""
      .update.run.map(_ => ())
  }

  override def getAuxiliaryText(articleId: String): IO[List[Option[String]]] = toIO {
    sql"select text from auxiliary_text_table where article_id = $articleId"
      .query[Option[String]]
      .to[List]
  }

  override def getCategoryStatistic: IO[List[CategoryStatistic]] = toIO {
    sql"""
    select category, count(article_id) from full_info_table fit
    right outer join category_catalog cc
    on(fit.category_id  = cc.id)
    group by category
    order by count(article_id);
    """.query[CategoryStatistic].to[List]
  }

  override def getCategoryIdByArticleId(articleId: String): IO[List[String]] = toIO {
    sql"select category_id from full_info_table where article_id = $articleId"
      .query[String]
      .to[List]
  }

  override def getCategoryById(categoryId: String): IO[Category] = toIO {
    sql"select id, category from category_catalog where id = ${categoryId}"
      .query[Category]
      .unique
  }

  override def getArticle(title: String): IO[List[PartitionArticle]] = toIO {
    sql"""select id, title, create_time, timestamp, language, wiki
           from articles where LOWER(title) = LOWER($title)"""
      .query[PartitionArticle]
      .to[List]
  }


  override def updateArticle(fullArticle: FullArticle): IO[Unit] = toIO {

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


  override def getNumberOfArticlesByCategory(categoryId: String): IO[Int] = toIO {
    sql"select count (*) FROM full_info_table where category_id = $categoryId"
        .query[Int]
        .unique
  }

  override def getCategoryByName(categoryName: String): IO[Option[Category]] = toIO {
    sql"select id, category from category_catalog where category = $categoryName".query[Category].option
  }

  override def insertArticle(listPartArticle: List[PartitionArticle]): IO[Int] = toIO {
    val sql = "insert into articles (id, title, create_time, timestamp, language, wiki) values (?, ?, ?, ?, ?, ?)"
    Update[PartitionArticle](sql).updateMany(listPartArticle)

  }

  override def insertCategoryList(categoryList: List[Category]): IO[Int] = toIO {
    val sql = "insert into category_catalog (id, category) values (?, ?)"
    Update[Category](sql).updateMany(categoryList)
  }

  override def insertAuxText(auxTextFullList: List[AuxTextLine]): IO[Int] = toIO {
    val sql = "insert into auxiliary_text_table (article_id, text) values (?, ?)"
    Update[AuxTextLine](sql).updateMany(auxTextFullList)
  }

  override def insertFullTable(artCatRelateList: List[RelationArticleCategory]): IO[Int] = toIO {
    val sql = "insert into full_info_table (article_id, category_id) values (?, ?)"
    Update[RelationArticleCategory](sql).updateMany(artCatRelateList)
  }

  private def toIO[T](query: ConnectionIO[T]): IO[T] =
    query.transact(transactor)

}
