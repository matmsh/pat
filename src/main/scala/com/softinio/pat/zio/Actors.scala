package com.softinio.pat.zio

import zio.actors.Actor.Stateful
import zio.actors.{ActorSystem, Context, Supervisor}
import zio.{RIO, ZIO, ZIOAppDefault,Console, Duration}

object Actors extends ZIOAppDefault {

  def run: ZIO[Any, Throwable, Unit] = program

  val subscriber: Stateful[Any, Unit, Protocol] = new Stateful[Any,Unit, Protocol] {
    override def receive[A](
        state: Unit,
        protocol: Protocol[A],
        context: Context
    ): RIO[Any, (Unit, A)] =
      protocol match {
        case message: Message =>
          for {
            _ <- Console.printLine(
              s"Validating ${message.firstName} ${message.lastName} with email ${message.emailAddress}!"
            )
            valid <- message.isValid
            self <- context.self[Protocol]
            _ <- message.replyTo ! SubscribedMessage(1L, self)
            _ <- ZIO.when(valid)(message.db ! message).map(_.getOrElse(()))
          } yield ((), ().asInstanceOf[A])
        case _ => ZIO.fail(InvalidEmailException("Failed"))
      }
  }

  val datastore: Stateful[Any, Unit, Protocol] = new Stateful[Any, Unit, Protocol] {
    override def receive[A](
        state: Unit,
        protocol: Protocol[A],
        context: Context
    ): RIO[Any, (Unit, A)] =
      protocol match {
        case message: Message =>
          for {
            _ <- Console.printLine(s"Processing Command")
            _ <- message.command match {
              case Add =>
                Console.printLine(s"Adding message with email: ${message.emailAddress}")
              case Remove =>
                Console.printLine(s"Removing message with email: ${message.emailAddress}")
              case Get =>
                Console.printLine(s"Getting message with email: ${message.emailAddress}")
            }
          } yield ((), ().asInstanceOf[A])
        case _ => ZIO.fail(InvalidEmailException("Failed"))
      }
  }

  val reply: Stateful[Any, Unit, Protocol] = new Stateful[Any, Unit, Protocol] {
    override def receive[A](
        state: Unit,
        protocol: Protocol[A],
        context: Context
    ): RIO[Any, (Unit, A)] =
      protocol match {
        case message: SubscribedMessage =>
          for {
            _ <- Console.printLine(s"Got Reply: ${message.subscriberId}")
          } yield ((), ().asInstanceOf[A])
        case _ => ZIO.fail(InvalidEmailException("Failed"))
      }
  }

  val program: ZIO[Any, Throwable, Unit] = for {
    actorSystemRoot <- ActorSystem("salarTestActorSystem")
    subscriberActor <- actorSystemRoot.make("subscriberActor", Supervisor.none, (), subscriber)
    //zio://salarTestActorSystem@0.0.0.0:0000/subscriberActor
    _ <- ZIO.logInfo(s"subscriberActor.path=${subscriberActor.path}")
    datastoreActor <- actorSystemRoot.make("datastoreActor", Supervisor.none, (), datastore)
    replyActor <- actorSystemRoot.make("replyActor", Supervisor.none, (), reply)
    _ <- subscriberActor ! Message(
      "Salar",
      "Rahmanian",
      "code@softinio.com",
      Add,
      datastoreActor,
      replyActor
    )
    _ <- zio.Clock.sleep(Duration.Infinity)
  } yield ()
}
