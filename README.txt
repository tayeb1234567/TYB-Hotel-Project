HOW TO USE THIS PROJECT:


first open your cmd prompt and paste these two commands on it :  

javac -cp ".;sqlite-jdbc-3.49.1.0.jar" Database.java LogDatabase.java BookingServer.java

java -cp ".;sqlite-jdbc-3.49.1.0.jar" BookingServer 


then go to http://localhost:8000 


and use the website,

HOW TO CHECK THE DATA OF THE WEBSITE:


after submitting the username and password, a file will be automatically created called logs.db that contains the credentials of the website

to check that data we open the cmd prompt again and type these commands: 


sqlite3 logs.db

.tables

SELECT * FROM tables;


______________________________________________
                                               |
THIS PROJECT WAS CREATED BY TAYEB EL MERABTE.  |
_______________________________________________|


