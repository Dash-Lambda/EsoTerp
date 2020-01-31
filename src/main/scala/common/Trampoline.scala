package common

import org.typelevel.jawn.ast.{JNull, JValue}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

sealed trait Trampoline[A]
case class Done[A](res: A) extends Trampoline[A]
case class More[A](call: () => Trampoline[A]) extends Trampoline[A]

trait FlatOp[A] extends Trampoline[A]{
  def collapse(): Option[Trampoline[A]]}
case class DoOrOp[A, B](inp: Option[A], err: String)(f: A => Trampoline[B]) extends FlatOp[B]{
  def collapse(): Option[Trampoline[B]] = inp match{
    case Some(a) => Some(More(() => f(a)))
    case None =>
      println(s"Error: $err")
      None}}
case class DoOrErr[A, B](inp: Try[A])(f: A => Trampoline[B]) extends FlatOp[B]{
  def collapse(): Option[Trampoline[B]] = inp match{
    case Success(a) => Some(More(() => f(a)))
    case Failure(e) =>
      e match{
        case EsoExcep(info) => println(s"Error: common.EsoExcep ($info)")
        case _ => println(s"Error: $e")}
      None}}
case class DoOrNull[B](inp: JValue, err: String)(f: JValue => Trampoline[B]) extends FlatOp[B]{
  def collapse(): Option[Trampoline[B]] = inp match{
    case JNull =>
      println(s"Error: $err")
      None
    case _ => Some(f(inp))}}

object Trampoline{
  def apply[A](initTramp: Trampoline[A]): A = {
    @tailrec
    def tdo(tramp: Trampoline[A]): A = tramp match{
      case Done(a) => a
      case More(call) => tdo(call())
      case fop: FlatOp[A] => tdo(fop.collapse().get)}
  tdo(initTramp)}
  
  def doOrElse[A](default: A)(initTramp: Trampoline[A]): A = {
    @tailrec
    def tdo(tramp: Trampoline[A]): A = tramp match{
      case Done(a) => a
      case More(call) => tdo(call())
      case fop: FlatOp[A] => fop.collapse() match{
        case Some(nxt) => tdo(nxt)
        case None => default}}
    tdo(initTramp)}
}