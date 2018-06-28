import java.io._
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.Rating

object Cheng_Chen_task1 {
  def main(arguments: Array[String]) {
    Logger.getLogger("org").setLevel(Level.ERROR)
    // set environment
    val conf = new SparkConf().setAppName("JIMMY_CHEN")
    val sc = new SparkContext(conf)
    // default argument
    var Rating_Path = "data/ratings.csv"
    var Test_Path = "data/testing_small.csv"
    // extract claimed argument
    if (arguments.length > 0) {
      Rating_Path = arguments(0)
      Test_Path = arguments(1)
    }

    // Time
    val t2 = System.nanoTime

    // Load a whole text file as an RDD of strings using SparkContext.textFile()
    val WholeSet: RDD[String] = sc.textFile(Rating_Path)
    val header1 = WholeSet.first()
    val Data: RDD[((String, String), Float)] = WholeSet.filter(row => row!= header1)
      .map(line=>line.split(","))
      .map(x=>((x(0),x(1)),x(2).toFloat))

    // Load a test set text file as an RDD of strings using SparkContext.textFile()
    val TestSet: RDD[String] = sc.textFile(Test_Path)
    val header2 = TestSet.first()
    val Test = TestSet.filter(row => row!= header2)
      .map(line=>line.split(","))
      .map(x=>((x(0),x(1)),1))
    val Test_withRating = Test.leftOuterJoin(Data).map(x=>Rating(x._1._1.toInt, x._1._2.toInt, x._2._2.get.toDouble))

    // Generate the training dataset
    val Train = Data.subtractByKey(Test)
    val ratings: RDD[Rating] = Train.map(x=> Rating(x._1._1.toInt, x._1._2.toInt, x._2.toDouble))

    // Build the recommendation model using ALS
    val rank = 8 // rank is the number of features to use (also referred to as the number of latent factors).
    val numIterations = 20 // iterations is the number of iterations of ALS to run. ALS typically converges to a reasonable solution in 20 iterations or less.
    val model = ALS.train(ratings, rank, numIterations, 0.18) // alpha is a parameter applicable to the implicit feedback variant of ALS that governs the baseline confidence in preference observations.


    // Evaluate the model on rating data

    // Extract Training (user, product)
    val usersProducts = ratings.map { case Rating(user, product, rate) =>
      (user, product)
    }

    // Predict the result of Traing Set
    val predictions =
      model.predict(usersProducts).map { case Rating(user, product, rate) =>
        ((user, product), rate)
      }

    // (True Rating, Prediction Rating)
    val ratesAndPreds: RDD[((Int, Int), (Double, Double))] = ratings.map { case Rating(user, product, rate) =>
      ((user, product), rate)
    }.join(predictions)

    // Training Set MSE
    val MSE = ratesAndPreds.map { case ((user, product), (r1, r2)) =>
      val err = (r1 - r2)
      err * err
    }.mean()

    // Test Set
    val usersProducts_withRating = Test_withRating.map { case Rating(user, product, rate) =>
      (user, product)
    } // 20256

    val prediction_test: RDD[((Int, Int), Double)] =
      model.predict(usersProducts_withRating).map { case Rating(user, product, rate) =>
        ((user, product), rate)
      } // 18733

    val ratesAndPreds_test: RDD[((Int, Int), (Double, Double))] = Test_withRating.map { case Rating(user, product, rate) =>
      ((user, product), rate)
    }.join(prediction_test)//18733

    val ratesAndPreds_test2: RDD[((Int, Int), (Double, Option[Double]))] = Test_withRating.map { case Rating(user, product, rate) =>
      ((user, product), rate)
    }.leftOuterJoin(prediction_test)//20256

    val ratesAndPreds_test2_noNull: RDD[((Int, Int), (Double, Double))] = ratesAndPreds_test2.map(x=> {
      if(x._2._2 isEmpty) {
        ((x._1),(x._2._1, 3.0))
      } else if (x._2._2.get > 5) {
        ((x._1), (x._2._1, 5.toDouble))
      } else if (x._2._2.get < 0){
        ((x._1), (x._2._1, 0.toDouble))
      } else ((x._1), (x._2._1, x._2._2.get))
    })

    val absDiff: Array[Double] = ratesAndPreds_test2_noNull.map(x=>Math.abs(x._2._2-x._2._1)).collect()
    val count_result = Array(0,0,0,0,0)

    absDiff.foreach({x=>
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

    val RMSE = ratesAndPreds_test2_noNull.map { case ((user, product), (r1, r2)) =>
      val err = (r1 - r2)
      err * err
    }.mean()

    println("RSME = " + math.sqrt(RMSE))

    // print the time
    println("The total execution time taken is : %1.4f".format((System.nanoTime - t2)/1000000000.0) + " sec." )

    val save_result = ratesAndPreds_test2_noNull.collect().sortBy(_._1)
      .map(x=> x._1._1.toString +  "," + x._1._2.toString + "," + x._2._2.toString)
    val output_path = "Cheng_Cheng_result_task1.txt"
    val file = new FileWriter(output_path)
    file.write("UserId,MovieId,Pred_rating" + "\n")
    save_result.map(line => file.write(line + "\n"))
    file.close()
  }
}


