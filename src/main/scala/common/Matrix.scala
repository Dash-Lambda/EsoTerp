package common

case class Matrix[T](vec: Vector[T], xdim: Int, ydim: Int){
  def apply(x: Int, y: Int): T = {
    require(isDefinedAt(x, y), "Index Out Of Bounds")
    vec(getInd(x, y))
  }
  def get(x: Int, y: Int): Option[T] = {
    if(isDefinedAt(x, y)) Some(vec(getInd(x, y)))
    else None
  }
  def apply(v: Vec2D[Int]): T = apply(v.x, v.y)
  def get(v: Vec2D[Int]): Option[T] = get(v.x, v.y)
  
  def coordOf(e: T): Option[Vec2D[Int]] = vec.indexOf(e) match{
    case -1 => None
    case n => Some(getCoord(n))
  }
  
  def updated(v: Vec2D[Int], e: T): Matrix[T] = updated(v.x, v.y, e)
  def updated(x: Int, y: Int, e: T): Matrix[T] = {
    require(isDefinedAt(x, y), "Index Out Of Bounds")
    Matrix[T](vec.updated(getInd(x, y), e), xdim, ydim)
  }
  
  def transpose: Matrix[T] = {
    val trans = vec
      .grouped(xdim)
      .toVector
      .transpose
      .flatten
    Matrix[T](trans, ydim, xdim)
  }
  
  def padWith(x0: Int, x1: Int, y0: Int, y1: Int, e: T): Matrix[T] = {
    val padder = Vector.fill(x0)(e)
    val nvec = vec
      .grouped(xdim)
      .flatMap(v => padder ++ v.padTo(xdim + x1, e))
      .toVector
    val pvec = Vector.fill((x0 + xdim + x1)*y0)(e) ++ nvec ++ Vector.fill((x0 + xdim + x1)*y1)(e)
    Matrix[T](pvec, x0 + xdim + x1, y0 + ydim + y1)
  }
  def padTo(x: Int, y: Int, e: T): Matrix[T] = {
    val x0 = if(x < 0) x.abs else 0
    val x1 = if(x >= xdim) x + 1 - xdim else 0
    val y0 = if(y < 0) y.abs else 0
    val y1 = if(y >= ydim) y + 1 - ydim else 0
    padWith(x0, y0, x1, y1, e)
  }
  def padToAbs(x: Int, y: Int, e: T): Matrix[T] = {
    val nx = math.max(xdim, x)
    val ny = math.max(ydim, y)
    padWith(0, nx - xdim, 0, ny - ydim, e)
  }
  def padToAbs(v: Vec2D[Int], e: T): Matrix[T] = padToAbs(v.x, v.y, e)
  
  def isDefinedAt(v: Vec2D[Int]): Boolean = isDefinedAt(v.x, v.y)
  def isDefinedAt(x: Int, y: Int): Boolean = 0 <= x && x < xdim && 0 <= y && y < ydim
  def getInd(x: Int, y: Int): Int = (y*xdim) + x
  def getCoord(i: Int): Vec2D[Int] = Vec2D(i%xdim, (i - (i%xdim))/xdim)
  
  def denseString: String = vec.grouped(xdim).map(_.mkString).mkString("\n")
}
object Matrix{
  def apply[T](vecs: Seq[Seq[T]]): Matrix[T] = {
    require(vecs.forall(v => v.sizeIs == vecs.head.size), "Matrix Must Be Rectangular")
    val xdim = vecs.head.size
    val ydim = vecs.size
    new Matrix[T](vecs.toVector.flatten, xdim, ydim)
  }
  def fill[T](xdim: Int, ydim: Int)(e: T): Matrix[T] = new Matrix(Vector.fill(xdim*ydim)(e), xdim, ydim)
  def fromString(str: String): Matrix[Char] = {
    val rows = str.linesIterator.toVector
    val ydim = rows.length
    val xdim = rows.map(_.length).max
    new Matrix[Char](rows.flatMap(_.padTo(xdim, ' ')), xdim, ydim)
  }
  
  def charString(mat: Matrix[Int]): String = mat.vec.map(_.toChar).grouped(mat.xdim).map(_.mkString).mkString("\n")
}