package search_of_article

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.unsafe.IORuntime
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import search_of_article.config.ArticleServiceConfig
import search_of_article.di.{ApiDI, RepoDI, ServiceDI}
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource

import scala.concurrent.ExecutionContext

object ArticleApp extends IOApp {

  override implicit val runtime: IORuntime = IORuntime.global
  private implicit val ec: ExecutionContext = runtime.compute

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      config <- ConfigSource.default.loadF[IO, ArticleServiceConfig]
      repoDI = new RepoDI(config)
      serviceDI = new ServiceDI(repoDI)
      api = new ApiDI(serviceDI)
      _ <- repoDI.init.onError(e => IO.println(s"Failed to start an app due to exception $e"))
      _ <- if (config.dataDamp.readable)
            serviceDI.articleService.parsingOfFile(config.dataDamp.path)
            else IO.unit
      _ <- BlazeServerBuilder[IO]
        .withExecutionContext(ec)
        .bindHttp(8080, "localhost")
        .withHttpApp(Router("/" -> api.articleApi.articleRoutes).orNotFound)
        .resource
        .use { _ =>
          IO {
            println("Try out the API by opening the Swagger UI: http://localhost:8080/docs")
            scala.io.StdIn.readLine()
          }
        }
    } yield ExitCode.Success
  }
}