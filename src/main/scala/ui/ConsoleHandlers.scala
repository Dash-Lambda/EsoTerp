package ui

import java.io.{File, FileWriter, PrintWriter}

import Compilers.BFCompiler
import assemblers.{Assembler, AssemblerException}
import translators._

import scala.annotation.tailrec
import scala.io.{Source, StdIn}
import scala.util.{Failure, Success, Try}
import interpreters.{BFManager, BFOptimizer, Interpreter, InterpreterException}

import scala.collection.{immutable, mutable}

case class HandlerException(info: String) extends Throwable

object ConsoleHandlers {
  def runHandler(BFTranslators: mutable.HashMap[String, BFTranslator],
                 interpreters: immutable.HashMap[String, Interpreter],
                 optimizing: Int, initTapeSize: Int, outputMaxLength: Int, dynamicTapeSize: Boolean,
                 log: Boolean, debug: Boolean)(args: Vector[String]): Unit = {
    args match{
      case lang +: fnam +: _ if lang == "BrainFuck" || BFTranslators.isDefinedAt(lang) || interpreters.isDefinedAt(lang) =>
        grabProg(fnam) match{
          case Success(prog) =>
            val interp: String => Try[String] = if(interpreters.isDefinedAt(lang)) interpreters(lang)(log, debug, outputMaxLength)
            else BFManager(BFTranslators, initTapeSize, outputMaxLength, optimizing, dynamicTapeSize, log, debug, lang)
            
            print(s"Running $fnam... ")
            if(log) println
            val tStart = System.currentTimeMillis
            val res = interp(prog)
            val tDur = System.currentTimeMillis - tStart
            res match{
              case Success(_) if log => println(s"\nProgram completed in ${tDur}ms.")
              case Success(str) => print(s"Done in ${tDur}ms.\n$str\n")
              case Failure(InterpreterException(info)) => println(s"Error: $info")
              case Failure(e) => println(s"Error: $e")
            }
          case Failure(e) => println(s"Error: $e")
        }
      case _ => println("Language not recognized.")
    }
  }
  
  def compileHandler(initTapeSize: Int, outputMaxLength: Int, dynamicTapeSize: Boolean, log: Boolean, debug: Boolean)(args: Vector[String]): Unit = args match{
    case inam +: onam +: _ =>
      print(s"Retrieving from $inam... ")
      Try{ Source.fromFile(inam).mkString } match{
        case Success(progRaw) =>
          println("Done.")
          BFCompiler(initTapeSize, outputMaxLength, dynamicTapeSize, log, debug)(progRaw) match{
            case Success(prog) =>
              print(s"Saving to $onam... ")
              val oFile = new PrintWriter(new File(onam))
              oFile.print(prog)
              oFile.close()
              println("Done.")
            case Failure(e) => println(s"Error: $e")
          }
        case Failure(e) => println(s"Error: $e")
      }
    case _ => println("Not enough arguments.")
  }
  
  def assembleHandler(assemblers: immutable.HashMap[String, Assembler], log: Boolean, rev: Boolean)(args: Vector[String]): Unit = args match{
    case lang +: srcNam +: dstNam +: _ if assemblers.isDefinedAt(lang) =>
      Try{
        val iFile = Source.fromFile(srcNam)
        val prog = iFile.mkString
        iFile.close
        prog
      }match{
        case Success(prog) =>
          println(s"Retrieved program from $srcNam.\nAssembling...\n")
          val res = if(rev) assemblers(lang).unapply(prog, log) else assemblers(lang)(prog.split("\n").toVector, log)
          res match{
            case Success(assembled) =>
              print(s"\nAssembled.\nSaving to $dstNam... ")
              val oFile = new PrintWriter(new File(dstNam))
              oFile.print(assembled)
              oFile.close()
              println("Done.\n")
            case Failure(AssemblerException(info)) => println(s"Error: $info")
            case Failure(e) => println(s"Error: $e")
          }
        case Failure(e) => println(s"Error: $e")
      }
    case _ => println("Invalid Arguments")
  }
  
  def optimizeHandler(args: Vector[String], debug: Boolean): Unit = args match {
    case inam +: onam +: _ =>
      print(s"Retrieving from $inam... ")
      Try {
        val iFile = Source.fromFile(inam)
        val progRaw = iFile.mkString
        iFile.close()
        progRaw
      } match {
        case Success(progRaw) =>
          print("Done.\nOptimizing... ")
          BFOptimizer(progRaw, debug) match {
            case Success((bops, prog)) =>
              print(s"Done.\nSaving to $onam... ")
              val oFile = new PrintWriter(new File(onam))
              oFile.print(
                s"""|Optimized: ${prog.map(_._1).mkString}
                    |BulkOps: ${bops.zipWithIndex.map { case (bop, i) => s"[$i, ${bop.shift}, ${bop.ops.map { case (ind, inc) => s"{$ind->$inc}" }.mkString(", ")}]" }.mkString(", ")}
                    |Optimized Detail:${prog.zipWithIndex.map { case ((c, n), ind) => s"\n$ind: $c $n" }.mkString}""".stripMargin)
              oFile.close()
              println("Done.")
            case Failure(e) => println(s"Error: $e")
          }
        case Failure(e) => println(s"Error: $e")
      }
    case inam +: _ =>
      print(s"Retrieving from $inam... ")
      Try {
        val iFile = Source.fromFile(inam)
        val progRaw = iFile.mkString
        iFile.close()
        progRaw
      } match {
        case Success(progRaw) =>
          print("Done.\nOptimizing... ")
          BFOptimizer(progRaw, debug) match {
            case Success((bops, prog)) =>
              println(
                s"""|Done.
                    |
                    |Optimized: ${prog.map(_._1).mkString}
                    |BulkOps: ${bops.zipWithIndex.map { case (bop, i) => s"[$i, ${bop.shift}, ${bop.ops.map { case (ind, inc) => s"{$ind->$inc}" }.mkString(", ")}]" }.mkString(", ")}
                    |Optimized Detail:${prog.zipWithIndex.map { case ((c, n), ind) => s"\n$ind: $c $n" }.mkString}
                    |""".stripMargin)
            case Failure(e) => println(s"Error: $e")
          }
        case Failure(e) => println(s"Error: $e")
      }
    case _ => println("Invalid Arguments.")
  }
  
  def translationHandler(translators: mutable.HashMap[String, BFTranslator])(args: Vector[String]): Unit = {
    def chkLang(lang: String): Boolean = (lang == "BrainFuck") || translators.isDefinedAt(lang)
    def transToBF(prog: String, srcLang: String): String = if(srcLang == "BrainFuck") prog else translators(srcLang)(prog)
    def transFromBF(prog: String, dstLang: String): String = if(dstLang == "BrainFuck") prog else translators(dstLang).unapply(prog)
    def translate(prog: String, slang: String, dlang: String): String = transFromBF(transToBF(prog, slang), dlang)
    
    args match{
      case lang1 +: lang2 +: src +: tail if chkLang(lang1) && chkLang(lang2) =>
        grabProg(src) match{
          case Success(prog) =>
            println(s"Program successfully retrieved from $src.")
            tail match{
              case dest +: _ =>
                print(s"Saving to $dest... ")
                val oFile = new PrintWriter(new File(dest))
                oFile.print(translate(prog, lang1, lang2))
                oFile.close()
                println("Done.")
              case _ =>
                println(
                  s"""|Source:
                      |$prog
                      |
                      |Translated:
                      |${translate(prog, lang1, lang2)}
                      |""".stripMargin)
            }
          case Failure(e) => println(s"Error: $e")
        }
      case _ => println("Arguments not recognized.")
    }
  }
  
  def loadBindingsHandler(fnam: String): Vector[(String, Vector[String])] = Try{
    val iFile = Source.fromFile(fnam)
    val dats = iFile.getLines.toVector
    iFile.close()
    dats.map{str => val vec = str.split(" ").toVector; (vec.head, vec.tail)}
  } match{
    case Success(vec) =>
      println("Successfully loaded bindings")
      vec
    case Failure(e) =>
      println(s"Load Bindings Error: $e")
      Vector[(String, Vector[String])]()
  }
  
  def saveBindingsHandler(bindings: mutable.HashMap[String, Vector[String]])(args: Vector[String]): Unit = {
    val fnam = args.headOption match{
      case Some(str) => str
      case None => "userBindings.txt"
    }
    val oFile = new PrintWriter(new File(fnam))
    oFile.print(bindings.map{case (k: String, v: Vector[String]) => s"$k ${v.mkString(" ")}"}.mkString("\n"))
    oFile.close()
  }
  
  def listBindingsHandler(bindings: mutable.HashMap[String, Vector[String]]): Unit = {
    println(
      s"""|User bindings...
          |${bindings.toVector.map{case (k, v) => s"- $k => ${v.mkString(" ")}"}.mkString("\n")}
          |""".stripMargin)
  }
  
  def loadBFLangsHandler(args: Vector[String]): Vector[(String, BFTranslator)] = {
    @tailrec
    def scrub(langs: Vector[String], ac: Vector[(String, BFTranslator)]): Vector[(String, BFTranslator)] = langs match{
      case tok +: tail if tok.startsWith("name=") =>
        val nam = tok.drop(5)
        val syn = tail.take(8)
        val kvs = syn.map(str => (str.head.toString, str.drop(2)))
        scrub(tail.dropWhile(str => !str.startsWith("name=")), ac :+ (nam, GenericBFTranslator(nam, kvs)))
      case _ +: tail => scrub(tail.dropWhile(str => !str.startsWith("name=")), ac)
      case _ => ac
    }
    
    args match{
      case fnam +: _ =>
        grabLines(fnam) match{
          case Success(langs) => val transVec = scrub(langs, Vector[(String, BFTranslator)]())
            println(s"Successfully loaded:\n${transVec.map(p => s"- ${p._1}").mkString("\n")}\n")
            transVec
          case Failure(e) => println(s"Error: $e")
            Vector[(String, BFTranslator)]()
        }
      case _ => println("Invalid file name.")
        Vector[(String, BFTranslator)]()
    }
  }
  
  def langCreationHandler: (String, BFTranslator) = {
    val keys = Vector("<", ">", "+", "-", "[", "]", ",", ".")
    val nam = grabStr("Language Name: ")
    val kvs = keys.map(k => (k, grabStr(s"$k: ")))
    println
    (nam, GenericBFTranslator(nam, kvs))
  }
  
  def bfLangSaveHandler(translators: mutable.HashMap[String, BFTranslator], defaults: Vector[BFTranslator])(args: Vector[String]): Unit = args match{
    case fnam +: _ =>
      val oFile = new FileWriter(new File(fnam), true)
      val natLangs = defaults.map(t => (t.name, t))
      for(lang <- translators.filterNot(natLangs.contains(_)).values){
        oFile.write(
          s"""|name=${lang.name}
              |${lang.kvPairs.map(p => s"${p._1}=${p._2}").mkString("\n")}
              |""".stripMargin)
      }
      oFile.close()
    case _ => println("Invalid arguments.")
  }
  
  def syntaxHandler(translators: mutable.HashMap[String, BFTranslator])(args: Vector[String]): Unit = args match{
    case lang +: _ if translators.contains(lang) =>
      val synStr = translators(lang).kvPairs.map{case (k, v) => s"$k: $v"}.mkString("\n")
      println(s"Syntax for $lang...\n$synStr\n")
    case _ => println("Not a recognized translator.")
  }
  
  def printLangsHandler(interpreters: immutable.HashMap[String, Interpreter], BFTranslators: mutable.HashMap[String, BFTranslator], assemblers: immutable.HashMap[String, Assembler]): Unit = {
    println(
      f"""|Parent Languages...
          |- BrainFuck
          |${interpreters.keys.map(nam => s"- $nam").mkString("\n")}
          |
          |BrainFuck Translators...
          |${BFTranslators.keys.map(nam => s"- $nam").mkString("\n")}
          |
          |Assemblers...
          |${assemblers.keys.map(nam => s"- $nam").mkString("\n")}
          |""".stripMargin)
  }
  
  def printVarsHandler(initTapeSize: Int, outputMaxLength: Int, BFOpt: Int, dynamicTapeSize: Boolean, log: Boolean, debug: Boolean): Unit = {
    val maxLen = Vector(initTapeSize, outputMaxLength, BFOpt, dynamicTapeSize, log, debug).map(_.toString.length).max
    println(
      s"""|Runtime parameters...
          |- initTapeSize     = %-${maxLen}d  (initial tape length for BF interpreters)
          |- outputMaxLength  = %-${maxLen}d  (maximum size of output string for BF interpreters, useful for non-terminating programs, -1 = infinite)
          |- dynamicTapeSize  = %-${maxLen}b  (resize tape as needed for BF interpreters, eliminates memory limitations but reduces speed)
          |- BFOpt            = %-${maxLen}d  (BrainFuck interpreter selection: 0=base, 1=optimized, 2=compiled)
          |- log              = %-${maxLen}b  (determines whether output is shown during or after runtime)
          |- debug            = %-${maxLen}b  (show runtime information, such as stack and heap states)
          |""".stripMargin.format(initTapeSize, outputMaxLength, dynamicTapeSize, BFOpt, log, debug))
  }
  
  def helpHandler(): Unit = print(
    """|Commands...
       |- run <language> <source file>
       |
       |- compile <source file> <destination file>
       |
       |- assemble <language> <source file> <destination file>
       |- disassemble <language> <source file> <destination file>
       |
       |- optimize <source file> <optional destination file>
       |
       |- translate <source language> <target language> <source file> <optional destination file>
       |- defineBFLang
       |- loadBFLangs <file>
       |- saveBFLangs <file>
       |- syntax <BF language>
       |
       |- bind <token> <binding>
       |- unbind <token>
       |- clrBindings
       |- loadBindings <binding file>
       |- saveBindings <destination file>
       |- listBindings
       |
       |- set <variable name> <new value>
       |
       |- listLangs
       |- listVars
       |- help
       |
       |- exit
       |
       |""".stripMargin)
  
  def setBoolHandler(arg: String, default: Boolean): Boolean = arg match{
    case "true" => true
    case "false" => false
    case _ =>
      println("Error: Invalid Argument")
      default
  }
  def setIntHandler(arg: String, default: Int): Int = Try{arg.toInt} match{
    case Success(n) => n
    case Failure(_) =>
      println("Error: Invalid Argument")
      default
  }
  
  def grabStr(prompt: String): String = {print(prompt); StdIn.readLine}
  def grabLines(fnam: String): Try[Vector[String]] = Try{
    val fin = Source.fromFile(fnam)
    val strs = fin.getLines.toVector
    fin.close()
    strs
  }
  def grabProg(fnam: String): Try[String] = Try{
    val fin = Source.fromFile(fnam)
    val str = fin.mkString
    fin.close()
    str
  }
  
  def mkMap[A, B](vec: Vector[(A, B)]): immutable.HashMap[A, B] = {
    val builder = immutable.HashMap.newBuilder[A, B]
    builder ++= vec
    builder.result
  }
}