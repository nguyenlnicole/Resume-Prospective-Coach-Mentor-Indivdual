package com.pnbpaws.resume;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

final class SectionRewrites implements AutoCloseable {
  record Job(String name, String fingerprint, Supplier<ObjectNode> work) {}
  private final ExecutorService executor = new ThreadPoolExecutor(6,6,0L,TimeUnit.SECONDS,
      new ArrayBlockingQueue<>(48), Thread.ofPlatform().daemon().name("resume-section-",0).factory(),new ThreadPoolExecutor.AbortPolicy());
  static final class Cache {
    private record Entry(long time,ObjectNode result) {}
    private final LinkedHashMap<String,Entry> entries=new LinkedHashMap<>();
    private final Clock clock;
    Cache(){this(Clock.systemUTC());}
    Cache(Clock clock){this.clock=clock;}
    synchronized ObjectNode get(String key) {
      entries.entrySet().removeIf(entry -> clock.millis()-entry.getValue().time()>15*60*1000);
      var entry=entries.get(key);
      return entry==null?null:entry.result().deepCopy();
    }
    synchronized void put(String key,ObjectNode value) {
      entries.put(key,new Entry(clock.millis(),value.deepCopy()));
      while(entries.size()>16) entries.remove(entries.keySet().iterator().next());
    }
  }
  static Cache sessionCache(HttpSession session) {
    synchronized(session) {
      String owner=Objects.toString(session.getAttribute("coachEmail"),"")+"|"+Objects.toString(session.getAttribute("hiringFunnelUser"),"");
      if(!(session.getAttribute("sectionRewrites") instanceof Cache)
          || !owner.equals(session.getAttribute("sectionRewritesOwner"))) {
        session.setAttribute("sectionRewrites",new Cache());
        session.setAttribute("sectionRewritesOwner",owner);
      }
      return (Cache)session.getAttribute("sectionRewrites");
    }
  }
  static String key(String value) {
    try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
    catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
  }
  Map<String,ObjectNode> run(List<Job> jobs,Cache cache) {
    Map<String,Future<ObjectNode>> pending=new LinkedHashMap<>();
    try {
      for(var job:jobs) {
        String key=key(job.name()+job.fingerprint());
        ObjectNode prior=cache.get(key);
        pending.put(job.name(),prior==null?executor.submit(() -> {
          ObjectNode result=job.work().get();
          if(!result.path("_skipCache").asBoolean(false))cache.put(key,result);
          return result;
        }):CompletableFuture.completedFuture(prior));
      }
      Map<String,ObjectNode> results=new LinkedHashMap<>();
      for(var entry:pending.entrySet()) results.put(entry.getKey(),entry.getValue().get());
      return results;
    } catch(InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(SERVICE_UNAVAILABLE,"Draft preparation was interrupted. Please try again.");
    } catch(ExecutionException e) {
      if(e.getCause() instanceof RuntimeException runtime)throw runtime;
      throw new ResponseStatusException(BAD_GATEWAY,"A section could not be prepared. Please try again.");
    } catch(RejectedExecutionException e) {
      throw new ResponseStatusException(SERVICE_UNAVAILABLE,"Draft preparation is busy. Please try again shortly.");
    } finally {pending.values().forEach(task -> {if(!task.isDone()) task.cancel(true);});}
  }
  public void close(){executor.shutdownNow();}
}
