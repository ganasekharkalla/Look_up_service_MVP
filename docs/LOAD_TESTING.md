# Load testing

This note records how the service was load tested with [k6](https://grafana.com/docs/k6/latest/), what throughput it reached, and what changed when the JVM setup changed. Everything here ran on one laptop against the **mock provider**, so the numbers are for comparing configurations with each other. They are not capacity figures for production.

Run date: 2026-11-27. Raw outputs are in [`load-test/results/2026-11-27/`](../load-test/results/2026-11-27/).

## Summary

- The service never returned an error in any test. When it is overloaded it gets slower and k6 drops iterations, but every response that comes back is `200`.
- Running the packaged jar on **JDK 21** raised the load the service handles cleanly from about 3,000 to about 4,500 requests per second, with the 50 ms mock provider.
- Adding **virtual threads** raised it again to about 5,000 req/s (6,160 req/s at the very peak). The gain is small when the provider is fast.
- With a **300 ms provider**, which is closer to a real network call, virtual threads matter much more. Platform threads stop at about 1,650 req/s because Tomcat's 200 request threads are all waiting on the provider. Virtual threads handled about 4,000 req/s cleanly and 5,800 req/s at the peak, roughly **3.5 times more**, with the same latency.
- Under heavy overload, virtual threads give up Tomcat's built-in limit on concurrent requests. The tail latency at the highest test rate was worse with them (p95 2.2 s against 0.8 s at 8,000 req/s). See [Recommendations](#recommendations).

## Setup

| Item | Value |
| --- | --- |
| Machine | 12 cores, 31 GB RAM, Linux 7.0 (one laptop running everything) |
| App | `lookup-service-0.1.0-MVP.jar`, Spring Boot 4.2.0, Tomcat default 200 request threads, Mongo driver default pool of 100 |
| JDKs | OpenJDK 17.0.21.1 and OpenJDK 21.0.13.1 (the jar is built for Java 17 and runs unchanged on 21) |
| Database | MongoDB 8.0 in Docker, one database per run, dropped before each case |
| Provider | The built-in mock, 0% failure rate, fixed latency of 50 ms or 300 ms |
| Load generator | k6 v2.4.0 |
| CPU pinning | App on cores 4-9, k6 on cores 0-3 (`taskset`). MongoDB in Docker was not pinned. |

## Traffic

Every request is `POST` to `/api/v1/lookups/phone` or `/api/v1/lookups/email`, chosen 50/50.

- **40% fresh:** a new phone number or email each time. The service misses in MongoDB, calls the provider (including its latency), and writes a new document.
- **60% cached:** one of 5 fixed phone numbers or 4 fixed emails. After the first call these are answered from MongoDB with no provider call.

That is close to the 55/45 database/provider split seen in the first `lookup-load.js` run.

## Tools

All scripts are in [`load-test/`](../load-test/).

| File | Purpose |
| --- | --- |
| `lookup-load.js` | Realistic load with think time and pass/fail thresholds. Profiles: `smoke`, `load` (default), `stress`. |
| `max-throughput.js` | Fires a fixed number of requests per second with no think time, using k6's `constant-arrival-rate` executor. If the app falls behind, k6 reports `dropped_iterations` instead of slowing down. |
| `find-max.sh` | Runs `max-throughput.js` at a list of rates and prints one row per rate. |
| `compare-jvm.sh` | Starts the jar under JDK 17, JDK 21 and JDK 21 with virtual threads, then runs `find-max.sh` against each. |

Columns in the result tables:

- **achieved:** requests per second the service actually completed.
- **dropped:** iterations k6 could not start because every VU was still busy. This is the clearest sign that the service has stopped keeping up.
- **p95, p99:** latency percentiles in milliseconds.
- **Errors** was 0% in every step of every run, so it is left out of the tables.

## Method

Each case follows the same steps:

1. Start the jar on port 8090 with the case's JDK and flags.
2. Drop the `lookup_bench` database so every case starts empty.
3. Warm up for 45 seconds at 2,000 req/s. These results are discarded so the JIT compiler has finished its main work.
4. Run steps of 20 seconds each at increasing rates, with a 3 second pause between steps.
5. Sample the app's CPU every 2 seconds.

The three cases differ in one thing at a time:

| Case | JDK | Threads |
| --- | --- | --- |
| A | 17 | Platform threads (default) |
| B | 21 | Platform threads (default) |
| C | 21 | Virtual threads (`--spring.threads.virtual.enabled=true`) |

## Results

### First baseline: app started from the IDE

This was the first stepped test, before the jar comparison. The app ran from the IDE on JDK 17 with `-XX:TieredStopAtLevel=1` (a quick-start JIT mode that limits optimization), with no CPU pinning, 50 ms provider, against `lookup_test`.

| Target req/s | Achieved | Dropped | avg ms | p95 ms | p99 ms |
| --- | --- | --- | --- | --- | --- |
| 200 | 199 | 0 | 25 | 57 | 59 |
| 800 | 798 | 0 | 24 | 58 | 61 |
| 1,600 | 1,588 | 150 | 23 | 54 | 61 |
| 2,000 | 1,994 | 0 | 22 | 55 | 59 |
| 3,000 | 2,989 | 0 | 23 | 55 | 65 |
| 4,000 | 3,884 | 1,835 | 81 | 212 | 288 |
| 6,000 | 3,875 | 35,218 | 1,091 | 1,946 | 2,034 |

It was clean up to 3,000 req/s and topped out at about 3,900.

### 50 ms provider

Rates from 2,500 up, since the baseline was already clean below that.

**A: JDK 17, platform threads**

| Target req/s | Achieved | Dropped | avg ms | p95 ms | p99 ms |
| --- | --- | --- | --- | --- | --- |
| 2,500 | 2,493 | 0 | 23 | 57 | 67 |
| 3,000 | 2,991 | 0 | 22 | 54 | 60 |
| 3,500 | 3,448 | 848 | 33 | 104 | 207 |
| 4,000 | 3,959 | 537 | 36 | 113 | 184 |
| 4,500 | 4,445 | 848 | 50 | 137 | 186 |
| 5,000 | 4,628 | 6,419 | 217 | 346 | 438 |
| 6,000 | 5,171 | 13,940 | 374 | 612 | 712 |
| 8,000 | 5,541 | 44,720 | 578 | 822 | 896 |

**B: JDK 21, platform threads**

| Target req/s | Achieved | Dropped | avg ms | p95 ms | p99 ms |
| --- | --- | --- | --- | --- | --- |
| 2,500 | 2,493 | 0 | 22 | 53 | 63 |
| 3,000 | 2,991 | 0 | 21 | 52 | 55 |
| 3,500 | 3,490 | 0 | 21 | 53 | 57 |
| 4,000 | 3,989 | 0 | 25 | 60 | 100 |
| 4,500 | 4,488 | 0 | 22 | 55 | 61 |
| 5,000 | 4,536 | 7,229 | 119 | 435 | 535 |
| 6,000 | 4,802 | 20,435 | 519 | 840 | 958 |
| 8,000 | 5,532 | 45,036 | 586 | 813 | 874 |

**C: JDK 21, virtual threads**

| Target req/s | Achieved | Dropped | avg ms | p95 ms | p99 ms |
| --- | --- | --- | --- | --- | --- |
| 2,500 | 2,493 | 0 | 21 | 52 | 54 |
| 3,000 | 2,992 | 0 | 21 | 52 | 55 |
| 3,500 | 3,490 | 0 | 21 | 52 | 54 |
| 4,000 | 3,989 | 0 | 21 | 53 | 54 |
| 4,500 | 4,487 | 0 | 22 | 53 | 57 |
| 5,000 | 4,986 | 0 | 24 | 58 | 67 |
| 6,000 | 5,952 | 629 | 43 | 117 | 194 |
| 8,000 | 6,161 | 33,434 | 441 | 2,207 | 2,448 |

### 300 ms provider

Lower rates, because the platform-thread cases saturate much earlier here.

**A: JDK 17, platform threads**

| Target req/s | Achieved | Dropped | avg ms | p95 ms | p99 ms |
| --- | --- | --- | --- | --- | --- |
| 1,000 | 982 | 51 | 121 | 302 | 304 |
| 1,500 | 1,473 | 93 | 119 | 301 | 303 |
| 2,000 | 1,628 | 4,330 | 865 | 1,653 | 1,841 |
| 2,500 | 1,649 | 12,706 | 1,487 | 2,575 | 2,639 |
| 3,000 | 1,644 | 22,862 | 1,716 | 2,641 | 2,676 |
| 4,000 | 1,621 | 43,359 | 1,920 | 2,670 | 2,704 |
| 6,000 | 1,629 | 82,993 | 2,042 | 2,667 | 2,705 |

**B: JDK 21, platform threads**

| Target req/s | Achieved | Dropped | avg ms | p95 ms | p99 ms |
| --- | --- | --- | --- | --- | --- |
| 1,000 | 982 | 56 | 119 | 301 | 302 |
| 1,500 | 1,473 | 82 | 120 | 301 | 302 |
| 2,000 | 1,648 | 4,270 | 828 | 1,549 | 1,703 |
| 2,500 | 1,631 | 13,077 | 1,546 | 2,603 | 2,643 |
| 3,000 | 1,637 | 23,093 | 1,711 | 2,620 | 2,680 |
| 4,000 | 1,623 | 43,319 | 1,923 | 2,682 | 2,708 |
| 6,000 | 1,643 | 82,900 | 2,048 | 2,649 | 2,684 |

**C: JDK 21, virtual threads**

| Target req/s | Achieved | Dropped | avg ms | p95 ms | p99 ms |
| --- | --- | --- | --- | --- | --- |
| 1,000 | 982 | 64 | 122 | 301 | 302 |
| 1,500 | 1,473 | 90 | 121 | 301 | 302 |
| 2,000 | 1,965 | 105 | 121 | 302 | 303 |
| 2,500 | 2,455 | 146 | 120 | 302 | 304 |
| 3,000 | 2,941 | 255 | 120 | 302 | 304 |
| 4,000 | 3,921 | 401 | 120 | 303 | 307 |
| 6,000 | 5,800 | 2,225 | 137 | 326 | 350 |

The few dozen drops in the first rows of the 300 ms tables (50 to 90 per 20 seconds, under 0.5%) show up in all three cases. They are most likely k6 starting extra VUs partway through the step, not slowness in the service. The large drop counts in the other rows are real.

### App CPU

Average of the five highest 2-second samples while the sweep ran. 600% means all six pinned cores were busy.

| Case | 50 ms provider | 300 ms provider |
| --- | --- | --- |
| A (JDK 17) | 477% | 148% |
| B (JDK 21) | 470% | 148% |
| C (JDK 21, virtual threads) | 481% | 466% |

## What the results show

**With the 50 ms provider the app is CPU-bound.** All three cases drive the six pinned cores to about 80% busy at saturation. Latency stays flat until the app runs out of CPU, then climbs sharply. In this regime the thread model hardly matters, which is why the three cases are close.

**JDK 21 moved the clean limit up by about 50%.** Case A started dropping iterations at 3,500 req/s, while case B stayed clean until 4,500. At full overload both topped out near 5,500 req/s, so the gain shows up as a steadier service at mid-high load, not a higher absolute peak. This is a single run per step. Treat a difference of a few hundred req/s as noise and the shape as the finding.

**Virtual threads added a smaller gain with the fast provider.** Case C stayed clean at 5,000 req/s and peaked at 6,160, about 11% above case B's 5,532.

**With the 300 ms provider the limit is Tomcat's thread pool, not CPU.** A and B stop at about 1,650 req/s while using only about 150% CPU. This matches a simple calculation: each request needs about 0.4 × 300 ms ≈ 120 ms of waiting on average, and 200 threads can complete 200 / 0.12 ≈ 1,650 requests per second. More CPU would not help. Virtual threads remove that limit, and case C kept the same 300 ms p95 up to 4,000 req/s before CPU became the limit again (about 470%).

**Overload behaviour differs.** With platform threads, Tomcat's 200 threads cap the number of requests being worked on, so the excess waits in a queue and p95 stayed under 1 s at 8,000 req/s. With virtual threads there is no such cap, so far more requests were in flight and p95 reached 2.2 s at the same rate, even though more requests completed. Nothing failed in either case. I did not measure whether CPU or the Mongo connection pool set the limit in the virtual-thread run.

## Limits of these tests

- One run per step, 20 seconds each. Small differences between steps are within noise.
- k6, the app, and MongoDB (in Docker) share one machine. Pinning k6 and the app reduced interference but MongoDB was not pinned.
- The provider is a mock that only sleeps. A real provider adds network variance, rate limits, and failures, none of which are tested here.
- The cached part of the traffic uses only 9 identities, so cache hits are the best case for MongoDB.
- Each case's database starts empty and grows during the run. A pre-filled database was not tested.
- The initial IDE baseline is not directly comparable with cases A to C. Case A uses the same JDK 17, but it also differs in JIT mode and CPU pinning (and runs from the jar). The jump from the baseline's peak of about 3,900 req/s to case A's 5,541 req/s therefore cannot be credited to any single setting.
- Virtual threads were enabled with the Spring property and not inspected with a thread dump. The 300 ms results show the setting took effect.

## Recommendations

1. **Move to JDK 21.** The jar already runs on it. Change `java.version` in `pom.xml` to 21 when you want to compile against it.
2. **Enable virtual threads** if the real provider is slow (hundreds of milliseconds), because that is where they pay off. With a fast provider they matter little.
3. **Before relying on virtual threads, add limits that Tomcat's thread pool used to provide.** Set an explicit Mongo pool size and maximum wait time (`maxPoolSize`, `maxWaitTime` in the connection string), and consider a cap on in-flight provider calls. Otherwise a slow provider can pile up requests without bound.
4. **Consider a virtual-thread executor in `ResilientCall`.** It currently uses a cached platform-thread pool for each provider call (`newCachedThreadPool`). Switching to `Executors.newVirtualThreadPerTaskExecutor()` needs Java 21. This was not tested.
5. **Benchmark from the jar, not the IDE.** The IDE run used `-XX:TieredStopAtLevel=1`, which limits JIT optimization.
6. **Re-test with the real provider** (or a stub with realistic latency and some failures) before turning any of these numbers into a capacity plan.

## How to reproduce

Requirements: Docker with the `mongo` container running, k6 at `~/.local/bin/k6`, `jq`, `taskset`, `pidstat`, and both JDKs installed.

```bash
# 1. Build the jar
./mvnw -DskipTests package

# 2. Full comparison, all three cases at both provider latencies (about 30 minutes)
load-test/compare-jvm.sh

# 3. A subset
CASES="B C" LATENCIES="300" load-test/compare-jvm.sh

# 4. One stepped sweep against an app that is already running
BASE_URL=http://localhost:8080 RATES="2500 3000 3500 4000" load-test/find-max.sh
```

`compare-jvm.sh` starts its own app on port 8090 and uses a separate database, so it does not touch a running dev instance or its data. Useful settings:

| Variable | Default | Meaning |
| --- | --- | --- |
| `CASES` | `A B C` | Which JVM setups to run |
| `LATENCIES` | `50 300` | Mock provider latencies in ms |
| `RATES_50`, `RATES_300` | see script | Target rates for each latency |
| `DURATION` | `20s` | Length of each step |
| `FRESH_RATIO` | `0.4` | Share of requests that are new identities |
| `APP_CPUS`, `K6_CPUS` | `4-9`, `0-3` | CPU pinning |
| `MAX_VUS` | `4000` | Cap on k6 virtual users |
