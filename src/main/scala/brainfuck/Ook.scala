package brainfuck

import scala.annotation.tailrec
import scala.collection.mutable

/** Syntax:
  *
  * [ => Ook! Ook?
  *
  * ] => Ook? Ook!
  *
  * + => Ook. Ook.
  *
  * - => Ook! Ook!
  *
  * > => Ook. Ook?
  *
  * < => Ook? Ook.
  *
  * . => Ook! Ook.
  *
  * , => Ook. Ook!
  */

object Ook extends BFTranslator{
  val name: String = "Ook"
  val baseLang: String = "BrainFuck"
  val kvPairs: Vector[(String, String)] = Vector[(String, String)](
    (".", "Ook! Ook."),
    (",", "Ook. Ook!"),
    (">", "Ook. Ook?"),
    ("<", "Ook? Ook."),
    ("+", "Ook. Ook."),
    ("-", "Ook! Ook!"),
    ("[", "Ook! Ook?"),
    ("]", "Ook? Ook!"))
  
  def transFrom(bools: mutable.HashMap[String, (Boolean, String)], nums: mutable.HashMap[String, (Int, String)])(prog: String): String = {
    @tailrec
    def tHelper(log: String, src: String): String = vals.find(_ == src.take(9)) match{
      case Some(tok) => tHelper(log ++ revSyntax(tok), src.drop(9))
      case None if src.sizeIs > 9 => tHelper(log :+ src.head, src.tail)
      case None => log ++ src
    }
    
    tHelper("", prog)
  }
  def transTo(bools: mutable.HashMap[String, (Boolean, String)], nums: mutable.HashMap[String, (Int, String)])(prog: String): String = keys.foldLeft(prog){case (str, key) => str.replaceAllLiterally(key, syntax(key))}
}