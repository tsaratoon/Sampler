package farrington.example

import sampler.r.script.RScript
import farrington.core.simulate.SimulateOutbreakData
import java.nio.file.Paths
import farrington.core.algorithm.Farrington
import farrington.core.script.CreateRScript
import farrington.core.algorithm.EDS
import java.nio.file.Files
import java.nio.charset.Charset
import sampler.r.rserve.RServeHelper
import farrington.core.json.JSON
import farrington.core.outbreak.OutbreakData
import play.api.libs.json.Json

object RunEDS extends App {
  
  //=======================
  // Location of input data file
  val inDir = Paths.get("data") 
  val inJSON = "outbreakData.json"
   
  //=======================
  // Location of output data file
  val outDir = Paths.get("results", "compareEDS")
  val csvCompare = "compareEDS.csv"
  val scriptCompare = "compareEDS.r" // R script to import the CSV and plot the data
  val pdfCompare = "compareEDS.pdf" // PDF containing the plots
  
  //=======================
  // Load simulated outbreak data
  val jsonIn = Json.parse(JSON.readJSON(inDir.resolve(inJSON)))
  val data = OutbreakData(jsonIn)
  
  //=======================
  // Choose point at which baseline data ends:
  val endBaseline = 120
  
  //=======================
  // Run the Early Detection System for all modes (using defaults)
  
  // Run EDS for each mode
  RServeHelper.ensureRunning()  
  val result_apha = EDS.run(data, endBaseline, Farrington.APHA)
  val result_farNew = EDS.run(data, endBaseline, Farrington.FarNew)
  val result_stl = EDS.run(data, endBaseline, Farrington.Stl)
  RServeHelper.shutdown
  
  Files.createDirectories(outDir)
  
  val numResults = Math.min(Math.min(result_apha.results.threshold.length, result_farNew.results.threshold.length), 100)
  val APHA_thresh = result_apha.results.threshold.takeRight(numResults)
  val FarNew_thresh = result_farNew.results.threshold.takeRight(numResults)
  val Stl_thresh = result_stl.results.threshold.takeRight(numResults)
  
  // Write times to CSV file
  val writerEDS = Files.newBufferedWriter(outDir.resolve(csvCompare), Charset.defaultCharset())
  writerEDS.write("month, count, APHA, FarNew, Stl")
  writerEDS.newLine
  for (i <- 0 until numResults) {
    writerEDS.write(s"${result_apha.results.date(i).idx.toString}, ${result_apha.results.actual(i).toString}, ${APHA_thresh(i).toString}, ${FarNew_thresh(i).toString}, ${Stl_thresh(i).toString}")
    writerEDS.newLine
  }
  writerEDS.close
  println("EDS results written to " + csvCompare)
  
  //=======================
  // Write R script to plot comparison of three EDS modes against observed data
  val rScript_comparison = CreateRScript.plotComparison(csvCompare, pdfCompare)
  RScript.apply(rScript_comparison, outDir.resolve(scriptCompare))
  println("EDS comparison plots written to " + pdfCompare)

}