package search_of_article.repo.impl

import search_of_article.model._
import search_of_article.repo.ArticleRepo
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import doobie.{ConnectionIO, Transactor}
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.fragment.Fragment
import doobie.util.update.Update

import java.util.UUID

class ArticleRepoImpl(transactor: Transactor[IO])(implicit runtime: IORuntime) extends ArticleRepo {

  override def insertCategory(category: Category): IO[Unit] = toIO {
    sql"""insert into category_catalog (id, category) values (${category.categoryId}, ${category.name});"""
      .update.run.map(_ => ())
  }

  override def getAuxiliaryText(articleId: UUID): IO[Option[List[String]]] = toIO {
    sql"select array_agg(text) from auxiliary_text_table group by article_id having article_id = $articleId"
      .query[List[String]]
      .option
  }

  override def getCategoryStatistic: IO[List[CategoryStatistic]] = toIO {
    sql"""
    select category, count(article_id) from article_category_relation acr
    inner join category_catalog cc
    on(acr.category_id  = cc.id)
    group by category
    order by count(article_id);
    """.query[CategoryStatistic].to[List]
  }

  override def getCategoryListByArticleId(articleId: UUID): IO[List[Category]] = toIO {
    sql"""select category_id, category from article_category_relation acr
    inner join articles a
      on (a.id = acr.article_id)
    inner join category_catalog cc
      on (cc.id = acr.category_id)
    where article_id = $articleId"""
      .query[Category]
      .to[List]
  }

  override def getCategoryById(categoryId: UUID): IO[Category] = toIO {
    sql"select id, category from category_catalog where id = ${categoryId}"
      .query[Category]
      .unique
  }

  override def getArticles(title: String): IO[List[PartitionArticle]] = toIO {
    sql"""select id, title, create_time, timestamp, language, wiki
           from articles where LOWER(title) = ${title.toLowerCase}"""
      .query[PartitionArticle]
      .to[List]
  }


  override def deleteAuxiliaryText(articleId: UUID): IO[Unit] = toIO {
    sql" delete from auxiliary_text_table where article_id = $articleId".update.run.map(_ => ())
  }

  override def updateArticle(fullArticle: FullArticle): IO[Unit] = toIO {

    val deleteFullTableRelation =
      sql"""
        delete from article_category_relation where article_id = ${fullArticle.id};
         """

    val updateRelationFullTable =
      fullArticle.categories.map(category =>
        sql"""
          insert into article_category_relation (article_id, category_id)
           values (${fullArticle.id}, ${category.categoryId});""").fold(Fragment.empty)(_ ++ _)

    val updateArticle =
      sql"""
          update articles set title = ${fullArticle.title}, timestamp = ${fullArticle.timestamp}
          where id = ${fullArticle.id};
       """

    val deleteUselessCategory =
      sql"""
          delete from category_catalog where id in(
            select cc.id  from article_category_relation acr
            right outer join category_catalog cc
            on(acr.category_id  = cc.id)
            group by acr.article_id, cc.id, cc.category
            having article_id is null);
         """
    (updateArticle
      ++ deleteFullTableRelation
      ++ updateRelationFullTable
      ++ deleteUselessCategory
      ).update.run.map(_ => ())
  }


  override def getNumberOfArticlesByCategory(categoryId: UUID): IO[Int] = toIO {
    sql"select count (*) FROM article_category_relation where category_id = $categoryId"
        .query[Int]
        .unique
  }

  override def getCategoryByName(categoryName: String): IO[Option[Category]] = toIO {
    sql"select id, category from category_catalog where lower(category) = ${categoryName.toLowerCase}"
      .query[Category].option
  }


  override def getCategoryCatalog: IO[List[Category]] = toIO {
    sql"select * from category_catalog".query[Category].to[List]
  }

  override def insertArticle(listArticle: List[FullArticle]): IO[Unit] = toIO {
    listArticle.map{article =>
        sql"""insert into articles values (${article.id}, ${article.title}, ${article.createTime},
              ${article.timestamp}, ${article.language}, ${article.wiki});"""}.reduce(_ ++ _).update.run.map(_ => ())
//    val sql = "insert into articles (id, title, create_time, timestamp, language, wiki) values (?, ?, ?, ?, ?, ?)"
//    Update[PartitionArticle](sql).updateMany(listArticle)

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
    val sql = "insert into article_category_relation (article_id, category_id) values (?, ?)"
    Update[RelationArticleCategory](sql).updateMany(artCatRelateList)
  }

  private def toIO[T](query: ConnectionIO[T]): IO[T] =
    query.transact(transactor)

}
