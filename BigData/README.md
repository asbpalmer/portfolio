# Big Data Projects
This shows the work that I did for my Big Data class, which covered techniques from Distributed Databases and Computing (using AWS, Spark, and other techiniques), Exploratory Data Analysis, Statistical Data Analysis, Database Queries, and Data Cleaning and Preparation.

Some of these projects really don't display my finest work, but I believe that, as a whole, they paint a story of my rapid growth in the field as I become more and more accustomed to the demands of Big Data.

I've included the datasets (except for the Fire Department calls, which was too big for GitHub) in case anyone wants to work with the data themselves.

### BigDataBrooklynHousing
This project demonstrated my initial techniques to EDA and data analysis using a dataset based on housing sales in Brooklyn.

Even on just a cursory review through my code, I found plenty of bits and pieces that I now know to change or implement better. However, we all have to start somewhere, and this shows a solid base to work off of and improve with.

Namely, the data presentation in question 9 isn't insightful, so I would want to tinker with some additional parameters, maybe break up the data more to garner usable insights.

### BigDataSparkDDMovies
This project was the class' first foray into distributed computing and databases.

It ended up being very involved in terms of data manipulation, and, while I am very pleased with the algorithms I implemented to reach the end goal, I now have some extra insight and familiarity that allows me to take a more advanced approach to cleaning the data.

Most evidently, I would want to use a regex filter to pick out the individual countries rather than iterating over them. And, while I do remember that Spark RDD's are not the most friendly when it comes to this, I would, in future implementations, just get the "country" header out of the final count, and likely shift the data to a better structure.

### BigDataSparkDFFireData
Continuing the class' work with distributed computing, this project demonstrated the techniques used to analyze call data using Spark DataFrames, which were much nicer to use than RDD's (though RDD's do excel at tasks which require certain kinds of data manipulation, such as parsing lists and counting items from them, the map function is an amazingly powerful feature).

Most of the changes that I would make would probably be considered mostly stylistic but would minimize overhead by replacing a lot of the simpler function declarations with lambda functions. There's also some additional data analysis that I would do to analyze what types of calls have the biggest delays and estimate, based on their frequency, how the department should best allocate resources to improve their responsiveness.

### BigDataAWSNoSQLHurricaneData
Shifting completely out of the comfort zone of the past instruction with this project, I was tasked with learning and implementing the completely new techniques for storing, studying, and manipulating data with varying structure in AWS' DynamoDB rather than in Spark.

As much as I remember disliking certain parts of this project (having to wait for the data to reupload everytime I needed to tweak something), I really did enjoy implementing the algorithms, and I think some of the small touches that I added to the project are really indicative of the quality I like to code for.

### BigDataMongoDBHurricaneData
This project was to redo the BigDataAWSNoSQLHurricaneData, but to this time use MongoDB, which uses different methods and techniques to achieve the same goals. This project shows how I was able to both adapt to the new technique as well as improve upon the algorithms that I previously implemented.