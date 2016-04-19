package farrington.example

import java.nio.file.Paths
import farrington.core.json.JSON
import farrington.core.outbreak.OutbreakData
import farrington.core.script.CreateRScript
import farrington.core.simulate.SimulateOutbreakData
import play.api.libs.json.Json
import sampler.r.script.RScript
import farrington.core.simulate.SimulatedData

object GenerateData extends App {
  
  //=======================
  // User-defined parameters
  
  // Number of months for which to simulate data:
  val nData = 462
  val endYear = 2014 
  
  // Choose "short" or "long" outbreaks
  // outbreakLength = "short"
  val outbreakLength = "long"
  
  // Choose log-Normal or epidemic curve outbreak
  // val outbreakShape = "logNormal"
  val outbreakShape = "epidemicCurve"
  
  // Define end of each period
  //Baseline -> Pre-outbreak -> Outbreak -> Post-outbreak
  val endBaseline = 146
  val endPreOutbreak = 182
  val endOutbreak = 282
  
  // Identifiers for results files
  val csvName_simulated = "simData.csv" // CSV file to store simulated data from Scala
  val csvName_outbreak = "outbreakData.csv" // CSV file to store simulated data from Scala
  val jsonName_simulated = "simData.json"
  val jsonName_outbreak = "outbreakData.json"
  val scriptName = "plotSimData.r" // R script to import the CSV and plot the data
  val pdfName = "simulatedOutbreakData.pdf" // PDF containing the plots
  
  // Choose directory to place resulting plot
  val outDir = Paths.get("data")
  
  //=======================
  // Simulate data and write SimulatedData to json and csv files
  val simData = SimulateOutbreakData.run(nData, endYear, outbreakShape, outbreakLength, endPreOutbreak, endOutbreak, 1)
  SimulatedData.writeToFile(simData, outDir, csvName_simulated)
  val json_simulated = Json.toJson(simData)
  JSON.writeJSON(json_simulated, outDir.resolve(jsonName_simulated))
  
  //=======================
  // Write OutbreakData to json and csv files 
  val outbreakData = OutbreakData(simData.year, simData.month, simData.counts)
  OutbreakData.writeToFile(outbreakData, outDir, csvName_outbreak)
  val json_outbreak = Json.toJson(outbreakData)
  JSON.writeJSON(json_outbreak, outDir.resolve(jsonName_outbreak))
  
  //=======================
  // Write R script, run in R and save the resulting PDF in the results directory
  val rScript = CreateRScript.plotSimulatedData(csvName_simulated, pdfName)
  RScript.apply(rScript, outDir.resolve(scriptName))

}