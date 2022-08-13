package search_of_article.di

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import com.zaxxer.hikari.HikariDataSource
import doobie.util.transactor.Transactor
import search_of_article.config.ArticleServiceConfig
import search_of_article.repo.LiquibaseMigrator
import search_of_article.repo.impl.ArticleRepoImpl

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class RepoDI(config: ArticleServiceConfig)(implicit runtime: IORuntime)  {

  lazy val repo = new ArticleRepoImpl(transactor)

  def init: IO[Unit] =
    IO.fromTry(liquibaseMigrator.runMigrations(config.liquibase.changelogPath))

  lazy val liquibaseMigrator: LiquibaseMigrator = new LiquibaseMigrator(hikariDs.getConnection)

  private lazy val hikariDs = {

    val ds = new HikariDataSource()

    Class.forName(config.db.driver)
    ds.setJdbcUrl(config.db.url)
    ds.setUsername(config.db.user)
    ds.setPassword(config.db.password)

    ds
  }


  private lazy val connectEC = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(32))

  private lazy val transactor = Transactor.fromDataSource[IO](hikariDs, connectEC)
}
