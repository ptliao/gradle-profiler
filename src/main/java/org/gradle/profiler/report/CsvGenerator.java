package org.gradle.profiler.report;

import org.gradle.profiler.result.BuildInvocationResult;
import org.gradle.profiler.result.Sample;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CsvGenerator extends AbstractGenerator {
    private final Format format;

    public enum Format {
        LONG, WIDE;

        public static Format parse(String name) {
            for (Format format : values()) {
                if (format.name().toLowerCase().equals(name)) {
                    return format;
                }
            }
            throw new IllegalArgumentException("Unknown CSV format: " + name);
        }
    }

    public CsvGenerator(File outputFile, Format format) {
        super(outputFile);
        this.format = format;
    }

    @Override
    protected void write(BenchmarkResult benchmarkResult, BufferedWriter writer) throws IOException {
        List<? extends BuildScenarioResult> allScenarios = benchmarkResult.getScenarios();
        switch (format) {
            case WIDE:
                writeWide(writer, allScenarios);
                break;
            case LONG:
                writeLong(writer, allScenarios);
                break;
            default:
                throw new AssertionError();
        }
    }

    private void writeWide(BufferedWriter writer, List<? extends BuildScenarioResult> allScenarios) throws IOException {
        writer.write("scenario");
        for (BuildScenarioResult scenario : allScenarios) {
            for (int i = 0; i < scenario.getSamples().size(); i++) {
                writer.write(",");
                writer.write(scenario.getScenarioDefinition().getTitle());
            }
        }
        writer.newLine();

        writer.write("version");
        for (BuildScenarioResult scenario : allScenarios) {
            for (int i = 0; i < scenario.getSamples().size(); i++) {
                writer.write(",");
                writer.write(scenario.getScenarioDefinition().getBuildToolDisplayName());
            }
        }
        writer.newLine();

        writer.write("tasks");
        for (BuildScenarioResult scenario : allScenarios) {
            for (int i = 0; i < scenario.getSamples().size(); i++) {
                writer.write(",");
                writer.write(scenario.getScenarioDefinition().getTasksDisplayName());
            }
        }
        writer.newLine();

        writer.write("value");
        for (BuildScenarioResult scenario : allScenarios) {
            for (Sample<? super BuildInvocationResult> sample : scenario.getSamples()) {
                writer.write(",");
                writer.write(sample.getName());
            }
        }
        writer.newLine();

        int maxRows = allScenarios.stream().mapToInt(v -> v.getResults().size()).max().orElse(0);
        for (int row = 0; row < maxRows; row++) {
            for (BuildScenarioResult scenario : allScenarios) {
                List<? extends BuildInvocationResult> results = scenario.getResults();
                if (row >= results.size()) {
                    continue;
                }
                BuildInvocationResult buildResult = results.get(row);
                writer.write(buildResult.getBuildContext().getDisplayName());
                break;
            }
            for (BuildScenarioResult scenario : allScenarios) {
                List<? extends BuildInvocationResult> results = scenario.getResults();
                writer.write(",");
                if (row >= results.size()) {
                    continue;
                }
                BuildInvocationResult buildResult = results.get(row);
                for (int i = 0; i < scenario.getSamples().size(); i++) {
                    Sample<? super BuildInvocationResult> sample = scenario.getSamples().get(i);
                    Duration duration = sample.extractFrom(buildResult);
                    if (i > 0) {
                        writer.write(",");
                    }
                    writer.write(String.valueOf(duration.toMillis()));
                }
            }
            writer.newLine();
        }

        List<BuildScenarioResult.Statistics> statistics = allScenarios.stream().flatMap(s -> s.getStatistics().stream()).collect(Collectors.toList());
        statistic(writer, "mean", statistics, BuildScenarioResult.Statistics::getMean);
        statistic(writer, "min", statistics, BuildScenarioResult.Statistics::getMin);
        statistic(writer, "25th percentile", statistics, v -> v.getPercentile(25));
        statistic(writer, "median", statistics, BuildScenarioResult.Statistics::getMedian);
        statistic(writer, "75th percentile", statistics, v -> v.getPercentile(75));
        statistic(writer, "max", statistics, BuildScenarioResult.Statistics::getMax);
        statistic(writer, "stddev", statistics, BuildScenarioResult.Statistics::getStandardDeviation);
        statistic(writer, "confidence", statistics, BuildScenarioResult.Statistics::getConfidencePercent);
    }

    private void writeLong(BufferedWriter writer, List<? extends BuildScenarioResult> allScenarios) throws IOException {
        writer.write("Scenario,Tool,Tasks,Phase,Iteration,Sample,Duration");
        writer.newLine();
        for (BuildScenarioResult scenario : allScenarios) {
            for (BuildInvocationResult result : scenario.getResults()) {
                for (Sample<? super BuildInvocationResult> sample : scenario.getSamples()) {
                    writer.write(scenario.getScenarioDefinition().getTitle());
                    writer.write(",");
                    writer.write(scenario.getScenarioDefinition().getBuildToolDisplayName());
                    writer.write(",");
                    writer.write(scenario.getScenarioDefinition().getTasksDisplayName());
                    writer.write(",");
                    writer.write(result.getBuildContext().getPhase().name());
                    writer.write(",");
                    writer.write(String.valueOf(result.getBuildContext().getIteration()));
                    writer.write(",");
                    writer.write(sample.getName());
                    writer.write(",");
                    writer.write(String.valueOf(sample.extractFrom(result).toMillis()));
                    writer.newLine();
                }
            }
        }
    }

    private void statistic(BufferedWriter writer, String name, List<BuildScenarioResult.Statistics> statistics, Function<BuildScenarioResult.Statistics, Double> value) throws IOException {
        writer.write(name);
        for (BuildScenarioResult.Statistics statistic : statistics) {
            writer.write(",");
            writer.write(String.valueOf(value.apply(statistic)));
        }
        writer.newLine();
    }
}
