import org.gradle.api.logging.Logger

/*
 * Copyright Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class FilePartition {
  public final int totalPartitions
  public final int partitionIndex
  public final int seed
  public final int testsPerPartition
  public final List<String> allFiles

  static boolean isPartitioningDisabled() {
    (System.getenv('GO_JOB_RUN_COUNT')?.toInteger() ?: 1) == 1
  }

  FilePartition(List<String> allFiles) {
    totalPartitions = System.getenv('GO_JOB_RUN_COUNT')?.toInteger() ?: 1
    partitionIndex = System.getenv('GO_JOB_RUN_INDEX')?.toInteger() ?: 1
    seed = System.getenv('GO_PIPELINE_COUNTER')?.toInteger() ?: System.getenv('PARTITION_SEED')?.toInteger() ?: 1

    this.allFiles = allFiles.sort()

    // random shuffle, every agent uses the same seed, so shuffling is predictable
    Collections.shuffle(this.allFiles, new Random(seed))

    testsPerPartition = (int) Math.ceil((double) allFiles.size() / totalPartitions)
  }

  List<String> currentFiles() {
    def allPartitions = allFiles.collate(testsPerPartition)
    (allPartitions[partitionIndex - 1] ?: []) as List<String>
  }

  String logTo(Logger logger) {
    logger.quiet("Partitioned ${allFiles.size()} files into ${totalPartitions} buckets with approx ${testsPerPartition} files per bucket. Using seed ${seed}.")
    logger.quiet("To reproduce the test failure, run with:")
    logger.quiet("PARTITION_SEED=${seed} GO_JOB_RUN_COUNT=${totalPartitions} GO_JOB_RUN_INDEX=${partitionIndex} ./gradlew YOUR_TARGET")
  }
}