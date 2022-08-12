package search_of_article.repo

import liquibase.{Contexts, Liquibase}
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor

import java.sql.Connection
import scala.util.{Failure, Success, Try}

class LiquibaseMigrator(connection: Connection) {

  private val liquibaseConnection = new JdbcConnection(connection)
  private val database = DatabaseFactory.getInstance.findCorrectDatabaseImplementation(liquibaseConnection)

  def runMigrations(changelogPath: String): Try[Unit] =
    Try {
      new Liquibase(changelogPath, new ClassLoaderResourceAccessor(), database).update(new Contexts())
    } match {
      case f @ Failure(_) =>
        f
      case s @ Success(_) =>
        s
    }

}
