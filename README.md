# Movie_Recommender_System_Scala

This is a course project which leveraged Spark to build a movie recommender system in Scala.

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

In this project, 
we import org.apache.spark.mllib.recommendation.ALS and 
 call the ALS.train(ratings, rank, numIterations, 0.01) .
In this project, we import org.apache.spark.mllib.recommendation.ALS and directly call the ALS.train(ratings, rank, numIterations, 0.01)directly.


### 3. Model 2: Item-based Collaborative Filtering (CF) Algorithm

We use item-based collabortive filtering instead of user-based collaborative filtering since in the case of movie recommender, the size of total movies are significantly smaller than the size of the users. Thinking about Netflix, they have over 100MM users and less than maybe 1000 movies. So using item-based collaborative filtering in this case will help reduce the computational time which is usually the biggest bottleneck of this kind of CF algorithm. 

* To learn more about user-based CF algorithm or item-based CF algorithm:
https://en.wikipedia.org/wiki/Collaborative_filtering (Memory-based section)

You need to build everything by yourself to make this algorithm work. 


### 4. Model Evalutaion

We evaluate this model based on time and RMSE (Root Mean Squared Error) on the test set.

\begin{equation*}
\left( \sum_{k=1}^n a_k b_k \right)^2 \leq \left( \sum_{k=1}^n a_k^2 \right) \left( \sum_{k=1}^n b_k^2 \right)
\end{equation*}


### 5. Environment














