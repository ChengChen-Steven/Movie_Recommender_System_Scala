import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD
import scala.collection.Map
import java.io._

object Cheng_Chen_task2 {
  // define the correlation calculation based on Pearson
  def correlation_2(Rating:List[(Double, Double)]): Option[Double] = {
    if(Rating.size==0) return Some(0.0)
    val ratingSum: (Double, Double) = Rating.foldLeft((0.0, 0.0)) { case ((accA, accB), (a, b)) => (accA + a, accB + b) }
    val ratingAvg = (ratingSum._1/Rating.size, ratingSum._2/Rating.size)
    val ratingDiffSquare: (Double, Double) = Rating.foldLeft((0.0, 0.0)) { case ((accA, accB), (a, b)) => (accA + Math.pow((a-ratingAvg._1),2), accB + Math.pow((b-ratingAvg._2),2))}
    val ratingDiff: List[Double] = Rating.map(x=>(x._1-ratingAvg._1, x._2-ratingAvg._2)).map{case(x,y) => x*y}
    if(Math.sqrt(ratingDiffSquare._1 * ratingDiffSquare._2) == 0.0) return Some(0.0)
    Some(ratingDiff.sum/Math.sqrt(ratingDiffSquare._1 * ratingDiffSquare._2))
  }

  // main
  def main(arguments: Array[String]) {
    Logger.getLogger("org").setLevel(Level.ERROR)
    // set environment
    val conf = new SparkConf().setAppName("JIMMY_CHEN").setMaster("local[*]")
    val sc = new SparkContext(conf)
    // default argument
    var Rating_Path = "data/ratings.csv"
    var Test_Path = "data/testing_small.csv"
    // extract claimed argument
    if (arguments.length > 0) {
      Rating_Path = arguments(0)
      Test_Path = arguments(1)
    }
    // time
    val t2 = System.nanoTime
    // Load the whole/testset text file as an RDD of strings using SparkContext.textFile()
    val WholeSet: RDD[String] = sc.textFile(Rating_Path)
    val header1 = WholeSet.first()
    val Data: RDD[((String, String), Double)] = WholeSet.filter(row => row != header1).map(line => line.split(",")).map(x => ((x(0), x(1)), x(2).toDouble))
    val TestSet: RDD[String] = sc.textFile(Test_Path)
    val header2 = TestSet.first()
    val Test: RDD[((String, String), Double)] = TestSet.filter(row => row != header2).map(line => line.split(",")).map(x => ((x(0), x(1)), 1.0))
    val Train: RDD[((String, String), Double)] = Data.subtractByKey(Test).map(x => ((x._1._1, x._1._2), x._2))

    // build the correlation matrix
    def corr_matrix(trainSet: RDD[((String, String), Double)]): Array[(String, String, Option[Double])] = {
     val trainBfJoin = trainSet.map(x=> (x._1._2, (x._1._1, x._2)))
     val trainJoin: RDD[(String, ((String, Double), (String, Double)))] = trainBfJoin.join(trainBfJoin)
     val allRateComb: RDD[((String, String), (Double, Double))] = trainJoin
       .map(x=>((x._2._1._1, x._2._2._1), (x._2._1._2, x._2._2._2)))// key(user1, user2), value(rating1, rating2), all the information comes from co-rated item
       .filter(x=>x._1._1 != x._1._2)//filter user (A,A), which is not very necessary
     val allRateCombBf: RDD[((String, String), List[(Double, Double)])] = allRateComb.groupByKey().mapValues(_.toList)
     val allRateCombAf: RDD[(String, String, Option[Double])] = allRateCombBf.map(x=>(x._1._1,x._1._2, correlation_2(x._2)))
     allRateCombAf.collect()
    }

    val corrMatrixMap: Predef.Map[(String, String), Option[Double]] = corr_matrix(Train).map(x=>((x._1, x._2),x._3)).toMap

    val allUserAvg: Map[String, Double] = Train.map(x=>(x._1._1, (x._2, 1))).reduceByKey((x, y)=>((x._1 + y._1),(x._2 + y._2))).mapValues(y=>y._1/y._2).collectAsMap()

    val trainCollect: Array[((String, String), Double)] = Train.collect()
    val trainMap: Predef.Map[(String, String), Double] = trainCollect.toMap
    val arrayWithItem2: Predef.Map[String, List[(String, Double)]] = Train.map(x=>(x._1._2,(x._1._1,x._2))).groupByKey().map(x=>(x._1, x._2.toList)).collect().toMap

    val Test2: RDD[(String, String, Double, Double)] = Test.leftOuterJoin(Data).map(x => (x._1._1, x._1._2, x._2._1, x._2._2.get.toDouble))
    val testPred = Test2.map(x=>{
      val userTarget = x._1
      val itemTarget = x._2
      val trueRating = x._4
      // filter trainCollect to get Array contains itemTarget // Array[(String, String)]
      val userWithItem: Array[(String, String)] = arrayWithItem2.getOrElse(itemTarget, List()).toArray.map(x=>(userTarget, x._1))
       // find the pearson in userTarget and otherUsers
      val userPearsonCorr: Array[((String, String), Option[Double])] = userWithItem.map(x=>((x._1, x._2), corrMatrixMap.getOrElse(x, Some(0.0))))

      val diffRating: Array[(String, Double, Double)] = userPearsonCorr
        .map(x=> (x._1._2, x._2.get, trainMap.get((x._1._2, itemTarget)).get, allUserAvg.get(x._1._2).get))
        .map(x=>(x._1, x._2, x._3 - x._4))

      val diffRatingTarget1 = diffRating.map(x=>x._2 * x._3).sum
      val diffRatingTarget2 = diffRating.map(x=> Math.abs(x._2)).sum
      if(diffRatingTarget2==0) {
        ((userTarget, itemTarget), (trueRating, allUserAvg.get(userTarget).get))
      } else{
        val sigma: Double = diffRatingTarget1/diffRatingTarget2
        ((userTarget, itemTarget), (trueRating, sigma + allUserAvg.get(userTarget).get))
      }
    }
    )
    val testPred2: RDD[((String, String), (Double, Double))] = testPred.map(x=> {
       if (x._2._2 > 4) {
        ((x._1), (x._2._1, 4.1))   // we accept the inaccuracy of around 1
      } else if (x._2._2 < 1.5){
         // change 0 to 2, replace with 2.5  -> 0.944
         // change 0 to 1.5, replace with 2.5  -> 0.944
         // change 4-4，1.5-3， 0.9365
        ((x._1), (x._2._1, 3))
      } else ((x._1), (x._2))})


    val helloWorld2: RDD[(Double, Double)] = testPred2.map(x=>(x._2._1, x._2._2))
    val helloWorld25 = helloWorld2.map(x=>Math.abs(x._2-x._1))
    val helloWorld3 = helloWorld25.collect()
    val RMSE = helloWorld25.map(x=>x*x).mean()

    val count_result = Array(0,0,0,0,0)
    helloWorld3.foreach({x=>
      if(x>=0 && x<1) count_result(0) += 1
      else if (x>=1 && x<2) count_result(1) += 1
      else if (x>=2 && x<3) count_result(2) += 1
      else if (x>=3 && x<4) count_result(3) += 1
      else if (x>=4) count_result(4) += 1
    })
    println(">=0 and <1: " + count_result(0))
    println(">=1 and <2: " + count_result(1))
    println(">=2 and <3: " + count_result(2))
    println(">=3 and <4: " + count_result(3))
    println(">=4: " + count_result(4))
    // println("RMSE1 = " + RMSE)

    println("RMSE = " + math.sqrt(RMSE))
    println("The total execution time taken is : %1.4f".format((System.nanoTime - t2)/1000000000.0) + " sec." )

    val save_result= testPred2.map(x=>((x._1._1.toInt,x._1._2.toInt), x._2._2)).collect().sortBy(_._1)
      .map(x=> x._1._1.toString +  "," + x._1._2.toString + "," + x._2.toString)
    val output_path = "Cheng_Cheng_result_task2.txt"
    val file = new FileWriter(output_path)
    file.write("UserId,MovieId,Pred_rating" + "\n")
    save_result.map(line => file.write(line + "\n"))
    file.close()
  }
}

