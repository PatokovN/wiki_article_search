package search_of_article.di

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import com.zaxxer.hikari.HikariDataSource
import doobie.util.transactor.Transactor
import search_of_article.repo.LiquibaseMigrator
import search_of_article.repo.impl.ArticleRepoImpl

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.util.Try

class RepoDI(implicit runtime: IORuntime)  {

  lazy val repo = new ArticleRepoImpl(transactor)

  def init: Try[Unit] = liquibaseMigrator.runMigrations("liquibase.changelog/changelog-all.xml")

  lazy val liquibaseMigrator: LiquibaseMigrator = new LiquibaseMigrator(hikariDs.getConnection)

  private lazy val hikariDs = {

    val ds = new HikariDataSource()

    Class.forName("org.postgresql.Driver")
    ds.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres?currentSchema=wiki")
    ds.setUsername("postgres")
    ds.setPassword("postgres")

    ds
  }


  private lazy val connectEC = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(32))

  private lazy val transactor = Transactor.fromDataSource[IO](hikariDs, connectEC)
}
