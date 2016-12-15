<p align="center">Advance Computer Systems</br>CSE 622 - Fall 2016
---------------

Goal
---------------
Improve the android database cloud sync functionality. Basically we are wrapping sqlite db methods to provide 3 different sycing functionalities: -</br>
1. Operations Log Approach </br>
2. Multiple Database Tables Approach </br>
3. File Chunking Approach </br>


Note
---------------
BlueMountainDroidsDB is a Database Implementation of BlueMountainSource and will be eventually moved to the BlueMountainSource repo </br>

This repo contains **Android App Code** as well as the **Django Web Server (Python based Web Framework)** </br>

Command Line Argument to run the Django Server is **python manage.py runserver**


Credits
-------
This project is an extension of code base intially developed by [**Sharath Chandrashekhara**](http://www.cse.buffalo.edu/~sc296/) along with [**Networked Systems Research Group**](https://nsr.cse.buffalo.edu) at **[University of Buffalo, The State University of New York](http://www.cse.buffalo.edu)**.

We acknowledge and grateful to [**Professor Steve ko**](https://nsr.cse.buffalo.edu/?page_id=272) and [**Professor Karthik Dantu**](http://www.cse.buffalo.edu/faculty/kdantu/) for their continuous support
throughout this project.

We also acknowledge the developers of [**humpty-dumpty-android**](https://github.com/Pixplicity/humpty-dumpty-android) for their simple file dump android utility which we are using the easily extract
database files

Usage for **humpty-dumpty-android** </br>
> chmod +x humpty.sh </br>
> ./humpty.sh -d edu.buffalo.rms.bluemountain.localapp databases/bm_droids_db_local </br>


Developers
---------
Aniruddh Adkar (aadkar@buffalo.edu)</br>
Ajay Partap Singh Chhokar (ajaypart@buffalo.edu)</br>
Ramanpreet Singh Khinda (rkhinda@buffalo.edu)</br>



