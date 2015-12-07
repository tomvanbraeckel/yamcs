Parameter archive on top of RocksDB.

== Design considerations ==
The parameter archive has in principle to store for each parameter pairs of (ti, vi) where:
ti - is a timestamp when the sample has been taken
vi - is the value of the parameter at that timestamp.

We can notice that for many parameters, that do not change very often (like an ON/OFF status),
the space required to store the timestamp can greatly exceed in size the space required for storing the value (if simple compression is used).
In fact since the timestamps are 8 bytes long, they are almost in all cases bigger than the parameter values.

Some parameter archives store the values just when they change.
It could be considered than in between the changes the exact timestamps are not very important. Of course, one has always to take care
that gaps in the data are not mistaken for non-changing parameter values.

However, in the space domain (and other domains as well), parameters are not sent individually but in packets, and all the parameters from one packet share the same timestamp.
Usually some of those parameters will be some counters or other things that change at each sample. It follows that at least for storing those values, one has to store the timestamp anyway.
 
So what we do is to store once the timestamps in one record and make reference to that record from all the parameters sharing those timestamps.


== Database structure ==
Each (parameter,type) combination is given an unique 4 bytes parameter_id. The parameter is identified by its fully qualified xtce name.



TimeArray - an ordered sequence of timestamps stored as a t0,dt1,dt2... where t0 is some initial timestamp and dt0,dt1.. are deltas from the previous timetamp - so ti = t0+dt1+dt2+...dti 
t0 is itself a delta from the beginning of a segment. The TimeArray is stored as a compacted VarInt array.


ParameterGroup - represents a list of parameter_id which are received together (share the same timestamp).


Each ParameterGroup is given a ParameterPartition_id

Column Families
- one CF named "metadata" contains:  
  - definition of parameter_id
  - definition of Parameter Groups
  
  
- for each ParameterGroup one CF named "g" + ParameterGroup_id
    contains the timestamp information
- for each Parameter_id one CF named "p" + Paramete_id 
   contains the parameter values



The data is segmented in hourly segments. All values of the parameters into one segment are stored in one record.




