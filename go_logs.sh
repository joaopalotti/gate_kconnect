#javac -cp $CLASSPATH:jars/*:/home/palotti/Dropbox/kconnect/gate_khresmoi/GATE_Developer_8.1/lib/* BatchProcessLogs.java

#java -cp $CLASSPATH:jars/*:/home/palotti/Dropbox/kconnect/gate_khresmoi/GATE_Developer_8.1/lib/* BatchProcessLogs -g /home/palotti/Dropbox/kconnect/gate_khresmoi/khresmoi-app/khresmoi-applications/application-noedit.xgapp  -a Disease -a Drug -a Investigation -a Anatomy -i /home/palotti/Dropbox/kconnect/allan/allan_allclicks_fixed.csv -o outputs/

java -cp $CLASSPATH:jars/*:/home/palotti/Dropbox/kconnect/gate_khresmoi/GATE_Developer_8.1/lib/* BatchProcessLogs -g  /home/palotti/Dropbox/kconnect/gate_khresmoi/khresmoi-app/khresmoi-applications/application-noedit.xgapp  -a Disease -a Drug -a Investigation -a Anatomy -i /home/palotti/Dropbox/kconnect/trip/clickstream_allusers_sinceJan2014.csv  -o output.csv

#java -cp $CLASSPATH:jars/*:/home/palotti/Dropbox/kconnect/gate_khresmoi/GATE_Developer_8.1/lib/* BatchProcessLogs -g  /home/palotti/Dropbox/kconnect/gate_khresmoi/khresmoi-app/khresmoi-applications/application-noedit.xgapp  -a Disease -a Drug -a Investigation -a Anatomy -i /home/palotti/Dropbox/kconnect/trip/h10 -o output.csv

# Other -a not used: DrugBank

