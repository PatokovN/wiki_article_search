package search_of_article.config

final case class ArticleServiceConfig(db: DbConfig,
                                      liquibase: LiquibaseConfig,
                                      dataDamp: ReadDataConfig,
                                      serverBuild: ServerBuildConfig)

final case class DbConfig(driver: String, url: String, user: String, password: String)

final case class LiquibaseConfig(changelogPath: String)
final case class ReadDataConfig(path: String, readable: Boolean)

final case class ServerBuildConfig(port: Int, host: String)
