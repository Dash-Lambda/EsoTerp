package common

import scala.annotation.tailrec
import scala.util.Try

trait Transpiler extends EsoObj{
  val src: String
  val dst: String
  
  def id: (String, String) = (src, dst)
  def apply(config: Config)(progRaw: String): Try[String]
  
  def indent(prog: String): String = {
    @tailrec
    def ido(ind: Int, ac: Vector[String], src: LazyList[String]): String = src match{
      case l +: ls =>
        val s = l.count(_ == '{') - l.count(_ == '}')
        if(l.startsWith("}")) ido(ind + s, ac :+ s"${"  "*(ind - 1)}$l", ls)
        else ido(ind + s, ac :+ s"${"  "*ind}$l", ls)
      case _ => ac.mkString("\n")}
    val lines = prog.linesIterator.to(LazyList).map(_.dropWhile("\t ".contains(_)))
    ido(0, Vector(), lines)}
}
