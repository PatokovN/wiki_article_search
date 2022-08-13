# Wiki_search_service

## required dependencies installation
1) Install Java 11 JDK, Scala 2.13.8, the Scala build tool

2) Create PostgreSQL DB server with settings below:
    - host: localhost
    - port: 5432
    - database: postgres
    - user: postgres
    - password: postgres
    - create schema "wiki"
3) Put your file with json information in folder "src/main/resources/directory_for_data_damp" 
4) Rename file with json information into "datafile.json"
5) To run the application go to "src/main/scala/search_of_article/ArticleApp.scala"
6) After program's run you should wait a bit before in console you will see "http://localhost:8080/docs". Click on this link and test the application

## restart of application(option)
if you don't want load a file to database (for example you did it earlier):
1) Go to "application.conf" file in resources folder
2) Find field with name "data-damp" and constant with name "readable" on it
3) Change value of "readable" to false (by default it has a "true" value)