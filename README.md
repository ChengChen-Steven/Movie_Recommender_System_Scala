# Movie_Recommender_System_Scala

This is a course project which leveraged Spark to build a movie recommender system in Scala.

Scala is such a lazy (sexy) language. I am not an expert but definitely like this language. 

Two methods will be used:

1. Alternating Least Squares (ALS) algorithm in spark.mllib; 
2. Neighbourhood-based (item-based in this case) Collaborative Filtering from scratch; 

We will be discussing slightly about the speed and accuracy in this case. 


### 1. Overview of the Business Problem / Datasets

The Movie Lens datasets can be found in the following link: https://grouplens.org/datasets/movielens/.

Download the dataset: ml-latest-small.zip. Once, you extract the zip archive, you will find multiple data files. In this problem, we will only use ratings.csv. However, you can combine other files to improve the performance of your recommendation system.

You will also download testing file from Blackboard: testing_small.csv. The testing dataset is a subset of the original dataset, each containing two columns: <userId> and <movieId>. The file testing_small.csv (20256 records) is from ratings.csv in ml-latest-small. Your goal is to predict the ratings of every <userId> and <movieId> combination in the test files. You CANNOT use the ratings in the testing datasets to train your recommendation system. Specifically, you should first extract training data from the ratings.csv file downloaded from Movie Lens using the testing data. Then by using the training data, you will need to predict rate for movies in the testing dataset. You can use the testing data as your ground truth to evaluate the accuracy of your recommendation system.
  
 Example: Assuming ratings.csv contains 1 million records and the testing_small.csv contains two records: (12345, 2, 3) and (12345, 13, 4). You will need to first remove the ratings of user ID 12345 on movie IDs 2 and 13 from ratings.csv. You will then use the remaining records in the ratings.csv to train a recommendation system (1 million – 2 records). Finally, given the user ID 12345 and movie IDs 2 and 13, your system should produce rating predictions as close as 3 and 4, respectively.

For short, use training set to build an algorithm and test using the test set. 

I upload two datasets in this repository. 

* rating.csv: training set + test set (~100K)
Attributes: userId, movieId, rating, timestamp

* testing_small.csv: test set (~20K)
Attributes: userId, movieId


### 2. Model 1: Model-based Collaborative Filtering (CF) Algorithm

User the collaborative filtering algorithm in Spark MLlib. Collaborative filtering is commonly used for recommender systems. These techniques aim to fill in the missing entries of a user-item association matrix. spark.mllib currently supports model-based collaborative filtering, in which users and products are described by a small set of latent factors that can be used to predict missing entries. spark.mllib uses the alternating least squares (ALS) algorithm to learn these latent factors. 

* To learn more about collaborative filtering in Spark MLlib: 
http://spark.apache.org/docs/latest/mllib-collaborative-filtering.html

* To learn more about ALS Matrix Factorization: 
(1) Google for intros.
(2) If you want someone to go through this concept through video/teaching. I will recommend you to watch the video of the Machine Learning course (https://www.coursera.org/learn/machine-learning) taught by Andrew Ng. It discussses about the concept of recommender systems and walks you through the math on Week 09. 

In this project, we will
* import org.apache.spark.mllib.recommendation.ALS, 
* call the ALS.train(ratings: RDD[Rating], rank: Int, iterations: Int, lambda: Double, blocks: Int) directly.

Details about the input in the ALS.train() API: 
>> Train a matrix factorization model given an RDD of ratings by users for a subset of products. The ratings matrix is approximated as the product of two lower-rank matrices of a given rank (number of features). To solve for these features, ALS is run iteratively with a configurable level of parallelism.
* ratings: RDD of Rating objects with userID, productID, and rating
* rank: number of features to use (also referred to as the number of latent factors)
* numIterations: number of iterations of ALS
* lambda: regularization parameter
* blocks: level of parallelism to split computation into


### 3. Model 2: Item-based Collaborative Filtering (CF) Algorithm

We use item-based collabortive filtering instead of user-based collaborative filtering since in the case of movie recommender, the size of total movies are significantly smaller than the size of the users. Thinking about Netflix, they have over 100MM users and less than maybe 1000 movies. So using item-based collaborative filtering in this case will help reduce the computational time which is usually the biggest bottleneck of this kind of CF algorithm. 

* To learn more about user-based CF algorithm or item-based CF algorithm:
https://en.wikipedia.org/wiki/Collaborative_filtering (Memory-based section), espcially advantages & disadvantages;

You need to build everything by yourself to make this algorithm work. 


### 4. Model Evalutaion

We evaluate this model based on time and RMSE (Root Mean Squared Error) on the test set.

* RMSE: https://en.wikipedia.org/wiki/Root-mean-square_deviation

Compare the result of two algroithms:

* Model 1: Model-based Collaborative Filtering (CF) Algorithm
Absolute difference between predicted ratings and true ratings
>> case >=0 and <1: 14517; 
>> case >=1 and <2: 4857;
>> case >=2 and <3: 800
>> case >=3 and <4: 78
>> case >=4: 4
>> RSME = 0.9440353396410447
>> The total execution time taken is : 7.0894 sec.

* Model 2: Item-based Collaborative Filtering (CF) Algorithm
>> case >=0 and <1: 15201
>> case >=1 and <2: 4131
>> case >=2 and <3: 802
>> case >=3 and <4: 122
>> case >=4: 0
>> RMSE = 0.9365449062120714
>> The total execution time taken is : 23.8067 sec. (Very fast among peers)

* Thoughts here:
Comparing the performance of these two algorithms results based on the above results is not interesting because of the following reasons:
1. Each algorithm has a lot of parameters/hyperparameters which we do not make the efforts to optimize.
2. RMSE is simply one of the many metrics that would be suitable to this problem.  
3. The test set is not huge enough to tell the difference

* For more details about the comparsion of the two algorithms:
Please check https://en.wikipedia.org/wiki/Collaborative_filtering for more details.


### 5. Environment

* Set environment and start spark-submit first in Terminal:

  export SPARK_HOME=~/spark-1.6.1-bin-hadoop2.4/
  export PATH=$SPARK_HOME/BIN:$PATH
  spark-submit
  
* Enter following commands in Terminal to run the file:

  spark-submit --master local[*] --class Cheng_Chen_task1 jar_filepath ratings.csv_filepath testing_small.csv_filepath


### 6. Side Concepts

* Deal with the missing values

When there is a new item (movie in this case) which never appears in the training dataset, or there is a new user, the rating that corresponds to item-user pair would be assigned a constant number, e.g. 2.5, 3 or the mean(ratings). 

* Deal with predictions out of bound

Chance is that the algorithm would predict a value which has a rating out of the bound [0, 5] (actually [0.5, 5] is more accurate). We cap those out-of-bound ratings to the bound.

* Build artifacts - Jar file

Please browse the web for more details...











