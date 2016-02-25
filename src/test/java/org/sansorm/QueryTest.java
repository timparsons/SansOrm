package org.sansorm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.common.XAResourceProducer;
import com.zaxxer.sansorm.SqlClosure;
import com.zaxxer.sansorm.SqlClosureElf;
import com.zaxxer.sansorm.TransactionElf;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QueryTest
{
   @BeforeClass
   public static void setup() throws Throwable
   {
      System.setProperty("org.slf4j.simpleLogger.log.bitronix.tm", "WARN");

      // We don't actually need the transaction manager to journal, this is just for testing
      System.setProperty("bitronix.tm.journal", "null");
      System.setProperty("bitronix.tm.serverId", "test");

      Properties props = new Properties();
      props.setProperty("resource.ds.className", "org.h2.jdbcx.JdbcDataSource");
      props.setProperty("resource.ds.uniqueName", "test-h2");
      props.setProperty("resource.ds.minPoolSize", "2");
      props.setProperty("resource.ds.maxPoolSize", "5");
      props.setProperty("resource.ds.driverProperties.url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

      File file = File.createTempFile("btm", ".properties");
      file.deleteOnExit();
      try (OutputStream out = new FileOutputStream(file))
      {
         props.store(out, "");
      }

      TransactionManagerServices.getConfiguration().setResourceConfigurationFilename(file.getAbsolutePath());

      BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
      TransactionElf.setTransactionManager(tm);
      TransactionElf.setUserTransaction(tm);

      Map<String, XAResourceProducer> resources = TransactionManagerServices.getResourceLoader().getResources();
      Object ds = resources.values().iterator().next();
      SqlClosure.setDefaultDataSource((DataSource) ds);

      SqlClosureElf.executeUpdate("CREATE TABLE target_class1 ("
                                  + "id INTEGER NOT NULL IDENTITY PRIMARY KEY, "
                                  + "timestamp TIMESTAMP, "
                                  + "string VARCHAR(128) "
                                  + ")");
   }

   @AfterClass
   public static void tearDown()
   {
      TransactionManagerServices.getTransactionManager().shutdown();
   }

   @Test
   public void testObjectFromClause()
   {
      TargetClass1 original = new TargetClass1(new Date(0), "Hi");
      SqlClosureElf.insertObject(original);

      TargetClass1 target = SqlClosureElf.objectFromClause(TargetClass1.class, "string = ?", "Hi");
      assertEquals("Hi", target.getString());
      assertEquals(0, target.getTimestamp().getTime());
   }

   @Test
   public void testNumberFromSql()
   {
      Number initialCount = SqlClosureElf.numberFromSql("SELECT count(id) FROM target_class1");
      SqlClosureElf.insertObject(new TargetClass1(null, ""));

      Number newCount = SqlClosureElf.numberFromSql("SELECT count(id) FROM target_class1");
      assertEquals(initialCount.intValue() + 1, newCount.intValue());

      int countCount = SqlClosureElf.countObjectsFromClause(TargetClass1.class, null);
      assertEquals(newCount.intValue(), countCount);
   }

   public void testDate()
   {
      Date date = new Date();

      TargetClass1 target = SqlClosureElf.insertObject(new TargetClass1(date, "Date"));
      target = SqlClosureElf.getObjectById(TargetClass1.class, target.getId());

      assertEquals(target.getTimestamp(), date);
      assertEquals(target.getTimestamp().getClass(), Date.class);
      assertEquals(target.getString(), "Date");
   }

   @Test
   public void testTimestamp()
   {
      Timestamp tstamp = new Timestamp(System.currentTimeMillis());
      tstamp.setNanos(200);

      TargetTimestampClass1 target = SqlClosureElf.insertObject(new TargetTimestampClass1(tstamp, "Timestamp"));
      target = SqlClosureElf.getObjectById(TargetTimestampClass1.class, target.getId());
      assertEquals(target.getTimestamp().getClass(), Timestamp.class);
      assertEquals(target.getTimestamp(), tstamp);
      assertEquals(200, target.getTimestamp().getNanos());
      assertEquals("Timestamp", target.getString());
   }
}
