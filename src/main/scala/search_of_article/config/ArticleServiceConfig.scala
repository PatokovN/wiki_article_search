package search_of_article.config

final case class ArticleServiceConfig(db: DbConfig, liquibase: LiquibaseConfig, dataDamp: DataDampConfig)

final case class DbConfig(driver: String, url: String, user: String, password: String)

final case class LiquibaseConfig(changelogPath: String)

final case class DataDampConfig(path: String, readable: Boolean)
