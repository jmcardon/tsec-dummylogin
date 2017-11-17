package io.github.jmcardon

import cats.data.OptionT
import cats.effect.Sync
import tsec.authentication.BackingStore
import tsec.authentication.credentials.{RawCredentials, SCryptPasswordStore}
import tsec.passwordhashers._
import tsec.passwordhashers.imports._
import cats.syntax.all._

import scala.collection.mutable

package object tseclogin {

  abstract class DummyBackingStore[F[_], I, A](getId: A => I)(
      implicit F: Sync[F])
      extends BackingStore[F, I, A] {
    val dummyStore = mutable.HashMap[I, A]()

    def put(elem: A): F[A] = F.delay {
      dummyStore.put(getId(elem), elem)
      elem
    }

    def get(id: I): OptionT[F, A] = {
      OptionT.fromOption(dummyStore.get(id))
    }

    def update(v: A): F[A] = {
      F.delay {
        dummyStore.update(getId(v), v)
        v
      }
    }

    def delete(id: I): F[Unit] = F.delay {
      dummyStore.remove(id)
    }

    def find(p: A => Boolean): OptionT[F, A] =
      OptionT.fromOption[F](dummyStore.find(u => p(u._2)).map(_._2))

  }

  abstract class PStore[F[_], I, A](getId: A => I)(implicit F: Sync[F])
      extends SCryptPasswordStore[F, I] {
    private val otherTable = mutable.HashMap[I, SCrypt]()

    def retrievePass(id: I): F[SCrypt] = {
      otherTable.get(id) match {
        case Some(s) =>
          F.pure(s)
        case None =>
          F.raiseError(new IllegalArgumentException)
      }
    }

    def putCredentials(credentials: RawCredentials[I]): F[Unit] = F.delay {
      otherTable.put(credentials.identity,
                     credentials.rawPassword.hashPassword[SCrypt])
    }

    def updateCredentials(credentials: RawCredentials[I]): F[Unit] = F.delay {
      otherTable.update(credentials.identity,
                        credentials.rawPassword.hashPassword[SCrypt])
    }

    def removeCredentials(credentials: RawCredentials[I]): F[Unit] =
      F.ensure(retrievePass(credentials.identity))(new IllegalArgumentException)(
        credentials.rawPassword.checkWithHash(_)) *> F.delay(
        otherTable.remove(credentials.identity)) *> F.unit
  }

}
