cd /home/$(whoami)/ECE428_mp3/src
rm ece428/mp1/*.class
javac -cp . ece428/mp1/*.java
java -cp . ece428/mp1/Main