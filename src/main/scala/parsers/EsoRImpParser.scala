package parsers

import scala.util.control.TailCalls.{TailRec, tailcall}

class EsoRImpParser[+A, +B](parser1: => EsoParser[A], parser2: => EsoParser[B]) extends EsoParser[B] {
  private lazy val p = parser1
  private lazy val q = parser2
  
  def apply(inp: String): EsoParseRes[B] = applyByTramp(inp)
  
  override def tramp[AA >: B, C](inp: EsoParserInput, start_ind: Int)(cc: ParserContinuation[AA, C]): TailRec[ParseTrampResult[C]] = {
    tailcall(
      p.tramp(inp, start_ind){
        case EsoParsedTramp(_, s1, e1) =>
          tailcall(
            q.tramp(inp, e1) {
              case EsoParsedTramp(tok2, _, e2) => tailcall(cc(EsoParsedTramp(tok2, s1, e2)))
              case EsoParseFailTramp => tailcall(cc(EsoParseFailTramp))})
        case EsoParseFailTramp => tailcall(cc(EsoParseFailTramp))})}
}
object EsoRImpParser{
  def apply[A,B](p: => EsoParser[A], q: => EsoParser[B]): EsoRImpParser[A,B] = new EsoRImpParser(p, q)
}