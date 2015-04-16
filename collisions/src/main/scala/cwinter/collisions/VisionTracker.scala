package cwinter.collisions

import Positionable.PositionableOps

class VisionTracker[T: Positionable](
  val xMin: Int,
  val xMax: Int,
  val yMin: Int,
  val yMax: Int,
  val radius: Int
) {
  assert((xMax - xMin) % radius == 0)
  assert((yMax - yMin) % radius == 0)

  val width = (xMax - xMin) / radius
  val height = (yMax - yMin) / radius

  private[this] val elementMap = collection.mutable.Map.empty[T, Element]
  private[this] val cells = Array.fill(width + 2, height + 2)(Set.empty[Element])


  def insert(obj: T): Unit = {
    val elem = new Element(obj)
    val (x, y) = elem.cell

    for (other <- nearbyElements(x, y)) {
      if (contains(elem, other)) {
        elem.inSight += other
        other.inSight += elem
      }
    }

    cells(x)(y) += elem
    elementMap.put(obj, elem)
  }


  def remove(obj: T): Unit = {
    val elem = elementMap(obj)
    for (e <- elem.inSight) e.inSight -= elem
    val (x, y) = elem.cell
    cells(x)(y) -= elem
    elementMap -= obj
  }


  def updateAll() = {
    for (elem <- elementMap.values) {
      val actualCell = computeCell(elem)
      if (elem.cell != actualCell) {
        changeCell(elem, actualCell)
      }
    }

    for (elem <- elementMap.values) {
      val (x, y) = elem.cell
      elem.inSight = {
        for {
          other <- nearbyElements(x, y)
          if contains(elem, other)
        } yield other
      }.toSet
    }
  }

  private def changeCell(elem: Element, newCell: (Int, Int)): Unit = {
    val (x1, y1) = elem.cell
    cells(x1)(y1) -= elem

    val (x2, y2) = newCell
    cells(x2)(y2) += elem

    elem.cell = newCell
  }


  def getVisible(obj: T) =
    elementMap(obj).inSight.map(_.elem)

  private def contains(elem1: Element, elem2: Element): Boolean = {
    val diff = elem1.position - elem2.position
    (diff dot diff) <= radius * radius
  }

  private def computeCell(elem: Element): (Int, Int) = {
    val cellX = 1 + (elem.position.x.toInt - xMin) / radius
    val cellY = 1 + (elem.position.y.toInt - yMin) / radius
    (cellX, cellY)
  }

  private def nearbyElements(x: Int, y: Int): Iterator[Element] =
    cells(x - 1)(y + 1).iterator ++
      cells(x + 0)(y + 1).iterator ++
      cells(x + 1)(y + 1).iterator ++
      cells(x - 1)(y + 0).iterator ++
      cells(x + 0)(y + 0).iterator ++
      cells(x + 1)(y + 0).iterator ++
      cells(x - 1)(y - 1).iterator ++
      cells(x + 0)(y - 1).iterator ++
      cells(x + 1)(y - 1).iterator

  private final class Element(
    val elem: T
  ) {
    var inSight = Set.empty[Element]
    var cell = computeCell(this)


    def position = elem.position
  }

}
