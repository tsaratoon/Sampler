/*
* Copyright (c) Oliver Tearne (tearne at gmail dot com)
*
* This program is free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version
* 3 of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along with this program.
* If not, see <http://www.gnu.org/licenses/>.
*/

package sampler.example

import java.nio.file.Files
import java.nio.file.Paths

import scala.annotation.tailrec

import sampler.Implicits._
import sampler.data.Distribution
import sampler.data.SerialSampler
import sampler.math.Probability
import sampler.math.Random
import sampler.r.QuickPlot

/*
 * Given a breeding hierarchy where donor traits are to be 
 * introduced (heterozygously) into a preferred variety, what 
 * is the distribution of proportion of preferred variety is 
 * present in the final plant?
 */

object Breeding extends App{
	import Biology._
	
	//Number of centimorgan per chromosome
	val species = Species(100,200,300)
	
	val prefVar = RootPlant("PV", species)
	val donor1  = RootPlant("D1", species)
	val donor2  = RootPlant("D2", species)
	
	//Traits which will be selected for
	val traits = Seq(
			Trait(0,30, donor1, species),	//First Chromosome, 31st cM
			Trait(1,60, donor2, species),
			Trait(1,120, donor1, species)
			//Nothing on third chromosome
	)
	
	def cross (a: RootPlant, b: RootPlant): IndexedSeq[Offspring] = {
		SerialSampler(
			Distribution[Offspring](a.crossAndSelect(b, Nil))
		)(_.size == 10000).toIndexedSeq
	}
	
	def backCross (a: Distribution[Offspring], traits: Seq[Trait] = Nil): IndexedSeq[Offspring] = {
		SerialSampler(
			a.filter(_.isInstanceOf[Successful]).map(a1 =>a1.crossAndSelect(prefVar, traits))
		)(_.size == 10000).toIndexedSeq
	}
	
	
	val d1d2_f1   	= cross(donor1, donor2)								//Cross without selection
	val d1d2pV_f1 	= backCross(Distribution.uniform(d1d2_f1), traits)	//Back cross with selection
	val bc1			= backCross(Distribution.uniform(d1d2pV_f1), traits)//Back cross with selection

	val proportionPV = bc1.collect{case s: Successful => 
		s.countOf(prefVar).toDouble / species.numGenes
	}
	
	val probSuccess = 1 - bc1.toEmpiricalSeq.probabilityTable(Failure)
	println(s"Probability of sekectuib success in final cross $probSuccess")
	
	val wd = Paths.get("results","Breeding")
	Files.createDirectories(wd)
	QuickPlot.writeDensity(
		wd,
		"Proportion",
		proportionPV.continuous("Proportion PV in plant")
	)
}

object Biology{
	implicit val r = Random
	
	case class Species(chromosomeLengths: Int*){
		val numChromosomes = chromosomeLengths.size
		val numGenes = 2 * chromosomeLengths.sum
	}
	
	case class Locus(chromosome: Int, leftTid: Boolean, cM: Int, species: Species){
		import species._
		assert(cM >= 0 && cM <= chromosomeLengths(chromosome))
		val nextCM = if(cM  == chromosomeLengths(chromosome) - 1) None else Some(cM + 1)
	}

	//Always assume heterozygous selection
	case class Trait(chromosome: Int, cM: Int, gene: RootPlant, species: Species)
	
	case class Chromatid(genes: Seq[RootPlant]){
		val size = genes.size
		def countOf(gene: RootPlant) = genes.count(_ == gene)
		override def toString = genes.map(_.name).toString
	}
	object Chromatid{
		def ofGenes(gene: RootPlant, length: Int) = Chromatid(Seq.fill(length)(gene))
	}
	
	case class Chromosome(left: Chromatid, right: Chromatid){
		assert(left.size == right.size)
		def countOf(gene: RootPlant) = left.countOf(gene) + right.countOf(gene)
		def satisfiesTrait(tr: Trait) = 
			left.genes(tr.cM) == tr.gene || right.genes(tr.cM) == tr.gene
	}
	object Chromosome {
		def ofGenes(gene: RootPlant, length: Int) = Chromosome(
			Chromatid.ofGenes(gene, length),
			Chromatid.ofGenes(gene, length)
		)
	}
	
	trait Plant{
		val species: Species
		def geneAt(l: Locus): RootPlant
		
		private def recombinationModel = Distribution.bernouliTrial(Probability(0.1))
		private def coin = Distribution.coinToss
		
		def crossAndSelect(that: Plant, traits: Seq[Trait]): Offspring = {
			assert(this.species == that.species)
			@tailrec
			def gameteBuilder(parent: Plant, locus: Locus, acc: Seq[RootPlant] = Nil): Chromatid = {
				val amOnLeftChromosome = (locus.leftTid ^ recombinationModel.sample)
				val nextGene = parent.geneAt(locus)
				
				locus.nextCM match{
					case None => 
						val allGenes = nextGene +: acc
						assert(allGenes.size == this.species.chromosomeLengths(locus.chromosome), acc.size)
						Chromatid(allGenes)
					case Some(nextCM) =>
						val nextLocus = locus.copy(leftTid = amOnLeftChromosome, cM = nextCM)
						gameteBuilder(parent, nextLocus, nextGene +: acc)
				}
			}
			
			@tailrec
			def loop(acc: IndexedSeq[Chromosome] = IndexedSeq.empty[Chromosome], idx: Int = 0): Offspring = {
				val coin1 = coin.sample
				val coin2 = coin.sample
				val g1 = gameteBuilder(this, Locus(idx, coin1, 0, species))
				val g2 = gameteBuilder(that, Locus(idx, coin2, 0, species))
				val newChromosome = Chromosome(g1,g2)
				val newAcc = acc :+ newChromosome
				
				if(idx == species.numChromosomes - 1) Successful(newAcc, species)
				else if(traits.filter(_.chromosome == idx).exists(tr => !newChromosome.satisfiesTrait(tr))) Failure
				else loop(newAcc, idx + 1)
			}
			
			loop()
		}
	}
	case class RootPlant(name: String, species: Species) extends Plant{
		def geneAt(l: Locus) = this
	}
	trait Offspring extends Plant
	case class Successful(chromosomes: Seq[Chromosome], species: Species) extends Offspring{
		def geneAt(locus: Locus) = 
			if(locus.leftTid) chromosomes(locus.chromosome).left.genes(locus.cM)
			else chromosomes(locus.chromosome).right.genes(locus.cM)

		def countOf(gene: RootPlant) = 
			chromosomes.foldLeft(0)(_ + _.countOf(gene))
	}
	object Failure extends Offspring{
		val species: Species = null
		def geneAt(l: Locus) = throw new RuntimeException("TODO, this is ugly")
	}

	implicit class RootPlantAsDistribution(p: RootPlant) extends Distribution[RootPlant]{
		def sample() = p
	}
	
	object Selector{
		def satisfiesTrait(p: Plant, tr: Trait) = 
			p.geneAt(Locus(tr.chromosome, true, tr.cM, tr.species)) == tr.gene || p.geneAt(Locus(tr.chromosome, false, tr.cM, tr.species)) == tr.gene
		def satisfiesTraits(p: Plant, traits: Seq[Trait]) = 
			!traits.exists(t => !satisfiesTrait(p, t) || t.species != p.species)
		
		def apply(plant: Plant, traits: Seq[Trait]): Option[Plant] = {
			if(satisfiesTraits(plant, traits)) Some(plant)
			else None
		}
	}
}







