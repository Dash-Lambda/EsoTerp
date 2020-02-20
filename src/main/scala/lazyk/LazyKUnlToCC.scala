package lazyk

import common.{Config, Translator}
import lazyk.LazyKFuncs._
import lazyk.LazyKParsers._

import scala.util.Try

object LazyKUnlToCC extends Translator{
  val name: String = "LazyK_Unlambda"
  val baseLang: String = "LazyK_CC"
  
  def apply(config: Config)(prog: String): Try[String] = {
    val expr = unlParser.parseOne(prog)
    def mkstr(exp: Expr): String = exp match{
      case AppExpr(e1, e2) => s"(${mkstr(e1)}${mkstr(e2)})"
      case `sexp` => "S"
      case `kexp` => "K"
      case `iexp` => "I"}
    Try{mkstr(expr)}}
  
  def unapply(config: Config)(prog: String): Try[String] = {
    val expr = combParser.parseOne(prog)
    def mkstr(exp: Expr): String = exp match{
      case AppExpr(e1, e2) => s"`${mkstr(e1)}${mkstr(e2)}"
      case `sexp` => "s"
      case `kexp` => "k"
      case `iexp` => "i"}
    Try{mkstr(expr)}}
}
