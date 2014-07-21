package geotrellis.raster.op.focal

import geotrellis.raster._

trait FocalMethods extends TileMethods {

  /** Computes the minimum value of a neighborhood */
  def focalMin(n: Neighborhood, bounds: Option[GridBounds] = None): Tile = {
    Min(tile, n, bounds)
  }

  /** Computes the maximum value of a neighborhood */
  def focalMax(n: Neighborhood, bounds: Option[GridBounds] = None): Tile = {
    Max(tile, n, bounds)
  }

  /** Computes the mode of a neighborhood */
  def focalMode(n: Neighborhood, bounds: Option[GridBounds] = None): Tile = {
    Mode(tile, n, bounds)
  }

  /** Computes the median of a neighborhood */
  def focalMedian(n: Neighborhood, bounds: Option[GridBounds] = None): Tile = {
    Median(tile, n, bounds)
  }

  /** Computes the mean of a neighborhood */
  def focalMean(n: Neighborhood, bounds: Option[GridBounds] = None): Tile = {
    Mean(tile, n, bounds)
  }

  /** Computes the sum of a neighborhood */
  def focalSum(n: Neighborhood, bounds: Option[GridBounds] = None): Tile = {
    Sum(tile, n, bounds)
  }

  /** Computes the standard deviation of a neighborhood */
  def focalStandardDeviation(n: Neighborhood, bounds: Option[GridBounds] = None): Tile = {
    StandardDeviation(tile, n, bounds)
  }

  /** Computes the next step of Conway's Game of Life */
  def focalConway(n: Neighborhood, bounds: Option[GridBounds] = None): Tile = {
    Conway(tile, n, bounds)
  }

  /**
   * Calculates the slope of each cell in a raster.
   * @param   cs         cellSize of the raster
   * @param   zFactor    Number of map units to one elevation unit.
   * @see [[Slope]]
   */
  def slope(cs: CellSize, zFactor: Double, bounds: Option[GridBounds] = None): Tile = {
    Slope(tile, Square(1), bounds, cs, zFactor)
  }

  /**
   * Calculates the aspect of each cell in a raster.
   * @param   cs          cellSize of the raster
   * @see [[Aspect]]
   */
  def aspect(cs: CellSize, bounds: Option[GridBounds] = None): Tile = {
    Aspect(tile, Square(1), bounds, cs)
  }

  /**
   * Computes Hillshade (shaded relief) from a raster.
   * @see [[Hillshade]]
   */
  def hillshade(cs: CellSize, azimuth: Double, altitude: Double, zFactor: Double, bounds: Option[GridBounds] = None) = {
    Hillshade(tile, Square(1), bounds, cs, azimuth, altitude, zFactor)
  }

  /** Calculates spatial autocorrelation of cells based on the similarity to neighboring values.
   * @see [[TileMoransICalculation]]
   */
  def tileMoransI(n: Neighborhood, bounds: Option[GridBounds] = None): Tile = {
    TileMoransICalculation(tile, n, bounds)
  }

  /** Calculates global spatial autocorrelation of a raster based on the similarity to neighboring values.
   * @see [[ScalarMoransICalculation]]
   */
  def scalarMoransI(n: Neighborhood, bounds: Option[GridBounds] = None): Double = {
    ScalarMoransICalculation(tile, n, bounds)
  }
}
