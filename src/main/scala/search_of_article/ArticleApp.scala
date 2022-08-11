package search_of_article

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.unsafe.IORuntime
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import search_of_article.di.{ApiDI, RepoDI, ServiceDI}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ArticleApp extends IOApp {

  override implicit val runtime: IORuntime = IORuntime.global
  private implicit val ec: ExecutionContext = runtime.compute

  val repoDI = new RepoDI
  val serviceDI = new ServiceDI(repoDI)
  val api = new ApiDI(serviceDI)

  override def run(args: List[String]): IO[ExitCode] = {

    repoDI.init match {
      case Failure(e) =>
        IO.println(s"Failed to start an app due to exception $e").as(ExitCode.Error)
      case Success(_) =>
        serviceDI.articleService
          .fillTable("src/main/resources/directory_for_data_damp/datafile.json").unsafeRunSync()

        BlazeServerBuilder[IO]
          .withExecutionContext(ec)
          .bindHttp(8080, "localhost")
          .withHttpApp(Router("/" -> api.articleApi.articleRoutes).orNotFound)
          .resource
          .use { _ =>
              IO {
                println("Try out the API by opening the Swagger UI: http://localhost:8080/docs")
                scala.io.StdIn.readLine()
              }
          }.as(ExitCode.Success)
    }
  }
}