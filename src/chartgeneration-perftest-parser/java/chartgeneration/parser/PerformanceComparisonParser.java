package chartgeneration.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import chartgeneration.common.Utilities;
import chartgeneration.parser.model.PerfSummaryData;
import chartgeneration.parser.model.PerfSummaryItem;

public class PerformanceComparisonParser implements DataParser {
	private final static Logger LOGGER = Logger
			.getLogger(PerformanceComparisonParser.class.getName());

	@Override
	public void parse(InputStream in, OutputStream out) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		CSVParser csvParser = null;
		CSVPrinter csvPrinter = null;
		try {
			csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
			csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
			//int sourceBuildID = 0;
			//String sourceBuildName = null;
			String sourceBuildPath = null;
			//int destBuildID = 0;
			//String destBuildName = null;
			String destBuildPath = null;
			for (CSVRecord csvRecord : csvParser) {
				switch (csvRecord.get(0)) {
				case "SOURCE":
					//sourceBuildID = Integer.parseInt(csvRecord.get(1));
					//sourceBuildName = csvRecord.get(2);
					sourceBuildPath = csvRecord.get(3);
					break;
				case "DEST":
					//destBuildID = Integer.parseInt(csvRecord.get(1));
					//destBuildName = csvRecord.get(2);
					destBuildPath = csvRecord.get(3);
					break;
				default:
					break;
				}
			}
			if (sourceBuildPath == null || sourceBuildPath.isEmpty()) {
				LOGGER.severe("Source build path is required.");
				Runtime.getRuntime().exit(1);
			}

			File sourceFile = new File(sourceBuildPath);
			JSONObject sourceResult = new JSONObject(new JSONTokener(
					new FileInputStream(sourceFile)));
			JSONArray sourceCharts = sourceResult.getJSONArray("charts");

			JSONObject sourceSummaryChart = findPerfSummaryChart2(sourceCharts);
			PerfSummaryData sourceData = null;
			if (sourceSummaryChart != null) {
				sourceData = parsePerfSummaryTable2(sourceSummaryChart);
			} else {
				sourceSummaryChart = findPerfSummaryChart1(sourceCharts);
				if (sourceSummaryChart != null)
					sourceData = parsePerfSummaryTable1(sourceSummaryChart);
			}
			if (sourceData == null) {
				LOGGER.severe("No valid result found from source build.");
				Runtime.getRuntime().exit(1);
			}

			if (destBuildPath == null || destBuildPath.isEmpty()) {
				LOGGER.severe("Destination build path is required.");
				Runtime.getRuntime().exit(1);
			}

			File destFile = new File(destBuildPath);
			JSONObject destResult = new JSONObject(new JSONTokener(
					new FileInputStream(destFile)));
			JSONArray destCharts = destResult.getJSONArray("charts");

			JSONObject destSummaryChart = findPerfSummaryChart2(destCharts);
			PerfSummaryData destData = null;
			if (destSummaryChart != null) {
				destData = parsePerfSummaryTable2(destSummaryChart);
			} else {
				destSummaryChart = findPerfSummaryChart1(destCharts);
				if (destSummaryChart != null)
					destData = parsePerfSummaryTable1(destSummaryChart);
			}
			if (destData == null) {
				LOGGER.severe("No valid result found from destination build.");
				Runtime.getRuntime().exit(1);
			}
			printCSVItems(sourceData, destData, csvPrinter);
			csvPrinter.flush();
		} finally {
			if (csvParser != null)
				csvParser.close();
			if (csvPrinter != null)
				csvPrinter.close();
		}

	}

	private PerfSummaryData parsePerfSummaryTable1(JSONObject summaryChart) {
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
			case "Transation":
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
			long samples = row.getLong(samplesIndex);
			double avgRT = Utilities.parseDouble(row.get(avgIndex).toString());
			double _90LineRT = Utilities.parseDouble(row.get(_90LineIndex)
					.toString());
			// double stdDevRT =
			// Utilities.parseDouble(row.getString((stdDevIndex));
			double errorPercentage = Utilities.parseDouble(row.get(errorIndex)
					.toString());
			double throughput = convertThoughputToTxPerHour(row.get(throughputIndex)
					.toString());
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
			long samples = row.getLong(samplesIndex);
			double avgRT = Utilities.parseDouble(row.get(avgIndex).toString());
			double _90LineRT = Utilities.parseDouble(row.get(_90LineIndex)
					.toString());
			// double stdDevRT = row.getDouble(stdDevIndex);
			double errorPercentage = Utilities.parseDouble(row.get(errorIndex)
					.toString());
			double throughput = convertThoughputToTxPerHour(row.get(throughputIndex)
					.toString());
			totalItem.setSamples(samples);
			totalItem.setAverage(avgRT);
			totalItem.set90Line(_90LineRT);
			totalItem.setError(errorPercentage);
			totalItem.setThroughput(throughput);
		}
		return new PerfSummaryData(transactionName2ItemMap, totalItem);
	}

	private double convertThoughputToTxPerHour(String s) {
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

	private PerfSummaryData parsePerfSummaryTable2(JSONObject summaryChart) {
		JSONObject columnKeys = summaryChart.getJSONObject("columnKeys");
		int txIndex = columnKeys.getInt("Transation");
		int samplesIndex = columnKeys.getInt("#Samples");
		int avgIndex = columnKeys.getInt("Average");
		int _90LineIndex = columnKeys.getInt("90% Line");
		int errorIndex = columnKeys.getInt("Error%");
		int throughputIndex = columnKeys.getInt("Throughput (tx/h)");
		JSONArray rows = summaryChart.getJSONArray("rows");
		Map<String, PerfSummaryItem> transactionName2ItemMap = new HashMap<String, PerfSummaryItem>();
		for (int j = 0; j < rows.length() - 1; ++j) {
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
			double throughput = getDoubleFromTableCell(row
					.getJSONObject(throughputIndex));
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
			// String txName = row.getJSONObject(txIndex).getString("value");
			long samples = row.getJSONObject(samplesIndex).getLong("value");
			double avgRT = getDoubleFromTableCell(row.getJSONObject(avgIndex));
			double _90LineRT = getDoubleFromTableCell(row
					.getJSONObject(_90LineIndex));
			// double stdDevRT =
			// getDoubleFromTableCell(row.getJSONObject(stdDevIndex);
			double errorPercentage = getDoubleFromTableCell(row
					.getJSONObject(errorIndex));
			double throughput = getDoubleFromTableCell(row
					.getJSONObject(throughputIndex));
			totalItem.setSamples(samples);
			totalItem.setAverage(avgRT);
			totalItem.set90Line(_90LineRT);
			totalItem.setError(errorPercentage);
			totalItem.setThroughput(throughput);
		}
		return new PerfSummaryData(transactionName2ItemMap, totalItem);
	}

	private double getDoubleFromTableCell(JSONObject tableCell) {
		String value = tableCell.get("value").toString();
		if (value == "null") {
			String rawValue = tableCell.get("rawValue").toString();
			return Utilities.parseDouble(rawValue);
		}
		return Double.parseDouble(value);
	}

	private void printCSVItems(PerfSummaryData source, PerfSummaryData dest,
			CSVPrinter csvPrinter) throws IOException {
		for (String transactionName : source.getItems().keySet()) {
			PerfSummaryItem sourceItem = source.getItems().get(transactionName);
			PerfSummaryItem destItem = dest.getItems().get(transactionName);
			if (destItem == null)
				destItem = new PerfSummaryItem();
			csvPrinter.printRecord("TX-" + transactionName,
					sourceItem.getSamples(), destItem.getSamples(),
					Utilities.doubleToString(sourceItem.getAverage()),
					Utilities.doubleToString(destItem.getAverage()),
					Utilities.doubleToString(sourceItem.get90Line()),
					Utilities.doubleToString(destItem.get90Line()),
					Utilities.doubleToString(sourceItem.getError()),
					Utilities.doubleToString(destItem.getError()),
					Utilities.doubleToString(sourceItem.getThroughput()),
					Utilities.doubleToString(destItem.getThroughput()));
		}
		PerfSummaryItem sourceItem = source.getTotal();
		if (sourceItem != null) {
			PerfSummaryItem destItem = dest.getTotal();
			if (destItem == null)
				destItem = new PerfSummaryItem();
			csvPrinter.printRecord("TOTAL", sourceItem.getSamples(),
					destItem.getSamples(),
					Utilities.doubleToString(sourceItem.getAverage()),
					Utilities.doubleToString(destItem.getAverage()),
					Utilities.doubleToString(sourceItem.get90Line()),
					Utilities.doubleToString(destItem.get90Line()),
					Utilities.doubleToString(sourceItem.getError()),
					Utilities.doubleToString(destItem.getError()),
					Utilities.doubleToString(sourceItem.getThroughput()),
					Utilities.doubleToString(destItem.getThroughput()));
		}
	}

	private static JSONObject findPerfSummaryChart1(JSONArray charts) {
		for (int i = 0; i < charts.length(); i++) {
			JSONObject chart = charts.getJSONObject(i);
			if (chart.has("chartType")
					&& "JmeterSummaryChart"
							.equals(chart.getString("chartType")))
				return chart;
		}
		return null;
	}

	private static JSONObject findPerfSummaryChart2(JSONArray charts) {
		for (int i = 0; i < charts.length(); i++) {
			JSONObject chart = charts.getJSONObject(i);
			if (chart.has("chartType")
					&& "TABLE".equals(chart.getString("chartType"))
					&& chart.has("key")
					&& "perf-summary-table".equals(chart.getString("key")))
				return chart;
		}
		return null;
	}
}

