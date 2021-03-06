package perfcharts.perftest.parser.perfsummary;

import org.json.JSONArray;
import org.json.JSONObject;
import perfcharts.common.Utilities;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PerfSummarySupport {
    public static JSONObject findPerfSummaryChart1(JSONArray charts) {
        for (int i = 0; i < charts.length(); i++) {
            JSONObject chart = charts.getJSONObject(i);
            if (chart.has("chartType")
                    && "JmeterSummaryChart"
                    .equals(chart.getString("chartType")))
                return chart;
        }
        return null;
    }

    public static JSONObject findPerfSummaryChart2(JSONArray charts) {
        for (int i = 0; i < charts.length(); i++) {
            JSONObject chart = charts.getJSONObject(i);
            if (chart.has("key")) {
                String key = chart.getString("key");
                if ("perf-summary-table".equals(key) || "perf-simple-summary".equals(key))
                    return chart;
            }
        }
        return null;
    }

    public static PerfSummaryData parsePerfSummaryTable1(JSONObject summaryChart) {
        JSONArray columnLabels = summaryChart.getJSONArray("columnLabels");
        int txIndex = -1;
        int samplesIndex = -1;
        int avgIndex = -1;
        int _90LineIndex = -1;
        // int stdDevIndex = -1;
        int errorIndex = -1;
        int throughputIndex = -1;
        for (int i = 0; i < columnLabels.length(); ++i) {
            String columnLabel = columnLabels.getString(i);
            switch (columnLabel) {
                case "Transation": // Oops, we have to make it compatible with this typo
                    txIndex = i;
                    break;
                case "Transaction":
                    txIndex = i;
                    break;
                case "#Samples":
                    samplesIndex = i;
                    break;
                case "Average":
                    avgIndex = i;
                    break;
                case "90% Line":
                    _90LineIndex = i;
                    break;
                // case "Std. Dev.":
                // stdDevIndex = i;
                // break;
                case "Error%":
                    errorIndex = i;
                    break;
                case "Throughput":
                    throughputIndex = i;
                    break;
                default:
                    break;
            }
        }
        JSONArray rows = summaryChart.getJSONArray("rows");
        Map<String, PerfSummaryItem> transactionName2ItemMap = new HashMap<String, PerfSummaryItem>();
        for (int j = 0; j < rows.length() - 1; ++j) {
            JSONArray row = (JSONArray) rows.get(j);
            String txName = row.getString(txIndex);
            long samples = samplesIndex >= 0 ? row.getLong(samplesIndex) : 0;
            double avgRT = avgIndex >= 0 ? Utilities.parseDouble(row.get(avgIndex).toString()) : Double.NaN;
            double _90LineRT = _90LineIndex >= 0 ? Utilities.parseDouble(row.get(_90LineIndex)
                    .toString()) : Double.NaN;
            // double stdDevRT =
            // Utilities.parseDouble(row.getString((stdDevIndex));
            double errorPercentage = errorIndex >= 0 ? Utilities.parseDouble(row.get(errorIndex)
                    .toString()) : Double.NaN;
            double throughput = throughputIndex >= 0 ? convertThoughputToTxPerHour(row.get(
                    throughputIndex).toString()) : Double.NaN;
            PerfSummaryItem item = transactionName2ItemMap.get(txName);
            if (item == null)
                transactionName2ItemMap.put(txName,
                        item = new PerfSummaryItem());
            item.setTransaction(txName);
            item.setSamples(samples);
            item.setAverage(avgRT);
            item.set90Line(_90LineRT);
            item.setError(errorPercentage);
            item.setThroughput(throughput);
        }
        PerfSummaryItem totalItem = null;
        if (rows.length() > 0) {
            totalItem = new PerfSummaryItem();
            JSONArray row = (JSONArray) rows.get(rows.length() - 1);
            long samples = samplesIndex >= 0 ? row.getLong(samplesIndex) : 0;
            double avgRT = avgIndex >= 0 ? Utilities.parseDouble(row.get(avgIndex).toString()) : Double.NaN;
            double _90LineRT = _90LineIndex >= 0 ? Utilities.parseDouble(row.get(_90LineIndex)
                    .toString()) : Double.NaN;
            // double stdDevRT = row.getDouble(stdDevIndex);
            double errorPercentage = errorIndex >= 0 ? Utilities.parseDouble(row.get(errorIndex)
                    .toString()) : Double.NaN;
            double throughput = throughputIndex >= 0 ? convertThoughputToTxPerHour(row.get(
                    throughputIndex).toString()) : Double.NaN;
            totalItem.setSamples(samples);
            totalItem.setAverage(avgRT);
            totalItem.set90Line(_90LineRT);
            totalItem.setError(errorPercentage);
            totalItem.setThroughput(throughput);
        }
        return new PerfSummaryData(transactionName2ItemMap, totalItem);
    }

    public static double convertThoughputToTxPerHour(String s) {
        Pattern pattern = Pattern.compile("(.+)/(.+)");
        Matcher m = pattern.matcher(s);
        if (!m.matches())
            return Double.NaN;
        double num = Double.parseDouble(m.group(1));
        String unit = m.group(2);
        switch (unit) {
            case "h":
                return num;
            case "min":
                return num * 60;
            case "s":
                return num * 60 * 60;
            case "ms":
                return num * 60 * 60 * 1000;
            default:
                return Double.NaN;
        }
    }

    public static PerfSummaryData parsePerfSummaryTable2(JSONObject summaryChart) {
        JSONObject columnKeys = summaryChart.getJSONObject("columnKeys");
        int txIndex = -1;
        if (columnKeys.has("Transation")) // Oops, we have to make it compatible with this typo
            txIndex = columnKeys.getInt("Transation");
        else
            txIndex = columnKeys.getInt("Transaction");
        int samplesIndex = columnKeys.getInt("#Samples");
        int avgIndex = columnKeys.getInt("Average");
        int _90LineIndex = columnKeys.getInt("90% Line");
        int errorIndex = columnKeys.getInt("Error%");
        // this row is optional
        int throughputIndex = columnKeys.has("Throughput (tx/h)") ? columnKeys.getInt("Throughput (tx/h)") : -1;
        JSONArray rows = summaryChart.getJSONArray("rows");
        Map<String, PerfSummaryItem> transactionName2ItemMap = new HashMap<String, PerfSummaryItem>();

        PerfSummaryItem totalItem = null;
        int dataRows = rows.length();
        JSONArray totalRow = null;
        if (summaryChart.has("bottomRows")) {
            JSONArray bottomRows = summaryChart.getJSONArray("bottomRows");
            if (bottomRows.length() > 0) {
                totalRow = bottomRows.getJSONArray(0);
            }
        } else {
            --dataRows;
            if (rows.length() > 0) {
                totalRow = rows.getJSONArray(rows.length() - 1);
            }
        }
        {
            if (totalRow != null) {
                totalItem = new PerfSummaryItem();
                JSONArray row = totalRow;
                // String txName =
                // row.getJSONObject(txIndex).getString("value");
                long samples = row.getJSONObject(samplesIndex).getLong("value");
                double avgRT = getDoubleFromTableCell(row
                        .getJSONObject(avgIndex));
                double _90LineRT = getDoubleFromTableCell(row
                        .getJSONObject(_90LineIndex));
                // double stdDevRT =
                // getDoubleFromTableCell(row.getJSONObject(stdDevIndex);
                double errorPercentage = getDoubleFromTableCell(row
                        .getJSONObject(errorIndex));
                double throughput = throughputIndex >= 0 ? getDoubleFromTableCell(row
                        .getJSONObject(throughputIndex)) : Double.NaN;
                totalItem.setSamples(samples);
                totalItem.setAverage(avgRT);
                totalItem.set90Line(_90LineRT);
                totalItem.setError(errorPercentage);
                totalItem.setThroughput(throughput);
            }
        }
        for (int j = 0; j < dataRows /*- 1*/; ++j) {
            JSONArray row = (JSONArray) rows.get(j);
            String txName = row.getJSONObject(txIndex).getString("value");
            long samples = row.getJSONObject(samplesIndex).getLong("value");
            double avgRT = getDoubleFromTableCell(row.getJSONObject(avgIndex));
            double _90LineRT = getDoubleFromTableCell(row
                    .getJSONObject(_90LineIndex));
            // double stdDevRT =
            // getDoubleFromTableCell(row.getJSONObject(stdDevIndex);
            double errorPercentage = getDoubleFromTableCell(row
                    .getJSONObject(errorIndex));
            double throughput = throughputIndex >= 0 ? getDoubleFromTableCell(row
                    .getJSONObject(throughputIndex)): Double.NaN;
            PerfSummaryItem item = transactionName2ItemMap.get(txName);
            if (item == null)
                transactionName2ItemMap.put(txName,
                        item = new PerfSummaryItem());
            item.setTransaction(txName);
            item.setSamples(samples);
            item.setAverage(avgRT);
            item.set90Line(_90LineRT);
            item.setError(errorPercentage);
            item.setThroughput(throughput);
        }
        return new PerfSummaryData(transactionName2ItemMap, totalItem);
    }

    public static double getDoubleFromTableCell(JSONObject tableCell) {
        String value = tableCell.get("value").toString();
        if (value == "null") {
            String rawValue = tableCell.get("rawValue").toString();
            return Utilities.parseDouble(rawValue);
        }
        return Double.parseDouble(value);
    }
}
