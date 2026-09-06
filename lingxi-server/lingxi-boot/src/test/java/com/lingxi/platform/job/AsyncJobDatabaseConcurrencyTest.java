package com.lingxi.platform.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class AsyncJobDatabaseConcurrencyTest {
  @Test
  void onlyOneInstanceCanClaimSameReadyJob() throws Exception {
    String url = "jdbc:h2:mem:job_claim;MODE=MySQL;DB_CLOSE_DELAY=-1";
    try (Connection c = DriverManager.getConnection(url)) {
      c.createStatement()
          .execute(
              "CREATE TABLE job(id BIGINT PRIMARY KEY,status VARCHAR(20),lease_owner"
                  + " VARCHAR(40),lease_until TIMESTAMP,next_retry_at TIMESTAMP)");
      c.createStatement()
          .execute("INSERT INTO job VALUES(1,'PENDING',NULL,NULL,TIMESTAMP '2026-08-05 00:00:00')");
    }
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    Callable<Integer> claim =
        () -> {
          start.await();
          try (Connection c = DriverManager.getConnection(url);
              PreparedStatement p =
                  c.prepareStatement(
                      "UPDATE job SET status='RUNNING',lease_owner=? WHERE id=1 AND"
                          + " ((status='PENDING' AND next_retry_at<=CURRENT_TIMESTAMP) OR"
                          + " (status='RUNNING' AND lease_until<=CURRENT_TIMESTAMP))")) {
            p.setString(1, Thread.currentThread().getName());
            return p.executeUpdate();
          }
        };
    Future<Integer> a = pool.submit(claim);
    Future<Integer> b = pool.submit(claim);
    start.countDown();
    assertThat(a.get() + b.get()).isEqualTo(1);
    pool.shutdownNow();
  }

  @Test
  void expiredLeaseCanBeRecoveredAfterRestart() throws Exception {
    String url = "jdbc:h2:mem:job_recovery;MODE=MySQL;DB_CLOSE_DELAY=-1";
    try (Connection c = DriverManager.getConnection(url)) {
      c.createStatement()
          .execute(
              "CREATE TABLE job(id BIGINT PRIMARY KEY,status VARCHAR(20),lease_owner"
                  + " VARCHAR(40),lease_until TIMESTAMP,next_retry_at TIMESTAMP)");
      c.createStatement()
          .execute(
              "INSERT INTO job VALUES(1,'RUNNING','dead-node',TIMESTAMP '2026-08-05"
                  + " 00:00:00',TIMESTAMP '2026-08-05 00:00:00')");
      int updated =
          c.createStatement()
              .executeUpdate(
                  "UPDATE job SET"
                      + " lease_owner='new-node',lease_until=DATEADD('SECOND',120,CURRENT_TIMESTAMP)"
                      + " WHERE id=1 AND status='RUNNING' AND lease_until<=CURRENT_TIMESTAMP");
      assertThat(updated).isEqualTo(1);
    }
  }
}
