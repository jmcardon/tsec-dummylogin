package io.github.jmcardon.tseclogin


import cats.data.OptionT
import cats.effect.IO
import io.circe._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder
import tsec.authentication._
import tsec.mac.imports.HMACSHA256
import cats.syntax.all._
import io.circe.syntax._
import io.circe.generic.auto._
import tsec.authentication.credentials.RawCredentials
import fs2.{Stream, StreamApp}

import scala.concurrent.duration._

case class User(id: Int, name: String)

object HelloWorldServer extends StreamApp[IO] with Http4sDsl[IO] {

  val userStore = new DummyBackingStore[IO, Int, User](_.id) {}
  val myPStore  = new PStore[IO, Int, User](_.id)            {}

  implicit val loginDecoder = jsonOf[IO, RawCredentials[String]]

  val authenticator =
    JWTAuthenticator.stateless(
      10.minutes,
      None,
      userStore,
      HMACSHA256.generateKeyUnsafe()
    )

  val Handler = SecuredRequestHandler(authenticator)

  val authedEndpoint = Handler {
    case GET -> Root / "hi" asAuthed user =>
      Ok(s"hello ${user.name}")
  }

  val loginRoute = HttpService[IO] {
    case request @ POST -> Root / "login" =>
      val response = for {
        rawCreds <- OptionT.liftF(request.as[RawCredentials[String]])
        u        <- userStore.find(_.name == rawCreds.identity)
        isAuthed <- OptionT.liftF(myPStore.authenticate(RawCredentials(u.id, rawCreds.rawPassword)))
        _        <- if (isAuthed) OptionT.pure[IO](()) else OptionT.none[IO, Unit]
        newAuth  <- authenticator.create(u.id)
      } yield authenticator.embed(Response[IO](), newAuth)

      response
        .getOrElse(
          Response[IO](Status.Unauthorized)
        )
        .handleError(_ => Response[IO](Status.Unauthorized))
  }

  val service = HttpService[IO] {
    case GET -> Root / "hello" / name =>
      Ok(Json.obj("message" -> Json.fromString(s"Hello, $name")))
  }

  def stream(args: List[String], requestShutdown: IO[Unit]) = {
    val exit = BlazeBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      .mountService(service, "/")
      .mountService(loginRoute, "/")
      .mountService(authedEndpoint, "/loggedIn")
      .serve

    for {
      _        <- Stream.eval(userStore.put(User(1, "Bob")))
      _        <- Stream.eval(myPStore.putCredentials(RawCredentials(1, "123456")))
      exitCode <- exit
    } yield exitCode

  }
}
