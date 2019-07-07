## Some useful examples of Hibernate & JPA with various DBs

**Prepare MySQL**:
```sql
CREATE USER 'test'@'localhost' IDENTIFIED BY 'test';
GRANT ALL PRIVILEGES ON *.* TO 'test'@'localhost';
```
1.  [Kundera JPA framework with Cassandra](https://github.com/dredwardhyde/hibernate-examples/blob/master/src/test/java/com/jpa/example/KunderaCassandra.java)

2.  [@ManyToMany example - persist/update/add new child](https://github.com/dredwardhyde/hibernate-examples/blob/master/src/test/java/com/jpa/example/ManyToManyExample.java)

3.  [One entity and multiple EntityManagers](https://github.com/dredwardhyde/hibernate-examples/blob/master/src/test/java/com/jpa/example/MultipleEM.java)

4.  [Automatic table partitioning in PostreSQL 10-11 using trigger & view](https://github.com/dredwardhyde/hibernate-examples/blob/master/src/test/java/com/jpa/example/PostgreSQLPartitioning.java)
